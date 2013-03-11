package com.example.ics499_lipyeow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.example.ics499_lipyeow.Locator.ResponsiveReceiver;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * A Service to run the Locator app on the background
 * 
 * @author Alexander Cam Liu
 * 
 */
public class LocationService extends IntentService implements LocationListener {

	public static final String outGoingMessage = "imsg";
	public static final String inComingMessage = "imsg";
	public static final String TAG = "LocationService";

	public LocationService() {
		super("LocationService");
		// TODO Auto-generated constructor stub
	}

	// Location services fields
	private Location location;
	private LocationManager service;
	private String provider;

	// The file to be uploaded to the server
	private File gpxfile;
	private String file;

	// Unique ID for the phone
	TelephonyManager telephonyManager;
	String uniqueID;

	// Store the coordinates
	double[] coordinates = new double[2];

	/**
	 * The method that will serve as the bridge between the Activity and this
	 * service At the end it will broadcast the data
	 */
	@Override
	protected void onHandleIntent(Intent intent) {

	}

	public void sendData() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ResponsiveReceiver.ACTION_RESP);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(outGoingMessage, coordinates);
		sendBroadcast(broadcastIntent);
	}

	/**
	 * Initalize all the fields with the class is called
	 */
	public void onCreate() {
		super.onCreate();
		// Get the phone device unique ID
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		uniqueID = telephonyManager.getDeviceId();
		file = uniqueID + ".csv";
		// Get the file from the SD card
		gpxfile = new File(Environment.getExternalStorageDirectory(), file);

		// If the file does not exit, create the file and write the headers
		if (!gpxfile.exists()) {
			String fields = "\"latitude\",\"longitude\",\"time\" \n";
			try {
				writeToFile(fields, gpxfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Start the location services manager
		service = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Set the criteria
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
		criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
		criteria.setCostAllowed(false);
		provider = service.getBestProvider(criteria, false);
		location = service.getLastKnownLocation(provider);

		// Get the last known location from the provider cache
		// If null, that is fine. That means it was not used the provider
		if (location != null) {
			onLocationChanged(location);
			Toast.makeText(this,
					"Using: " + location.getProvider() + " provider",
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Called when the startService is called
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		service.requestLocationUpdates(provider, 300000, 0, this);

	}

	/*
	 * Called when the stopService is called
	 */
	@Override
	public void onDestroy() {
		// remove any listener
		service.removeUpdates(this);
	}

	/**
	 * Called when a new location is changed
	 */
	@Override
	public void onLocationChanged(Location location) {
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		coordinates[0] = latitude;
		coordinates[1] = longitude;
		String data = latitude + "," + longitude + "," + "\"" + getTime()
				+ "\"" + "\n";
		try {
			// write the data to the file
			writeToFile(data, gpxfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendData();
	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, "Provider disabled", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(this, "Provider enabled", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Toast.makeText(this, "Provider status changed", Toast.LENGTH_LONG)
				.show();
	}

	/**
	 * Write the data to the file
	 * 
	 * @param data
	 *            the data to be stored
	 * @param file
	 *            the file to store the data
	 * @throws IOException
	 */
	public void writeToFile(String data, File file) throws IOException {
		FileWriter writer = new FileWriter(file, true);
		writer.append(data);
		writer.flush();
		writer.close();
	}

	/**
	 * Get the current time
	 * 
	 * @return the time in following format 'WED, 10 Oct 2012 10:00'
	 */
	public String getTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, d MMM yyyy HH:mm");
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());
	}
}