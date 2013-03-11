package com.example.ics499_lipyeow;

import java.io.File;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The GUI Interface
 * 
 * @author Alexander Cam Liu
 * 
 */
public class Locator extends Activity {

	// Buttons
	public Button startService;
	public Button stopService;

	// TextViews
	public TextView latitude;
	public TextView longitude;

	// The file to be uploaded to the server
	private File gpxfile;
	private String file;

	// Unique ID for the phone
	TelephonyManager telephonyManager;
	String uniqueID;

	// The receiver to get data from the service
	private ResponsiveReceiver receiver;

	// Notifications
	public NotificationManager notificationManager;
	public Notification notification;

	// Make sure if back button is clickable or not
	public boolean running = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gpsbackground);

		// Get the unique phone ID
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		uniqueID = telephonyManager.getDeviceId();
		file = uniqueID + ".csv";
		// Get the file from the SD card
		gpxfile = new File(Environment.getExternalStorageDirectory(), file);

		// Notifications
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Register the reciever
		IntentFilter filter = new IntentFilter(ResponsiveReceiver.ACTION_RESP);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		receiver = new ResponsiveReceiver();
		registerReceiver(receiver, filter);

		// Initialize the widgets
		startService = (Button) findViewById(R.id.b_startService);
		stopService = (Button) findViewById(R.id.b_stopService);
		longitude = (TextView) findViewById(R.id.longitude);
		latitude = (TextView) findViewById(R.id.latitude);

		// make the stop button not clickable
		stopService.setEnabled(false);

		// Listener for the start button
		startService.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// make sure only one button is enabled
				stopService.setEnabled(true);
				startService.setEnabled(false);
				createNotification();
				running = !running;
				// Call the onStart on the Service class
				startService(new Intent(Locator.this, LocationService.class));
			}
		});

		// Listener for the stop button
		stopService.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// make sure only one button is enabled
				stopService.setEnabled(false);
				startService.setEnabled(true);
				running = !running;
				// Call the onDestroy() on the Service class
				stopService(new Intent(Locator.this, LocationService.class));
				latitude.setText("Services Stopped");
				longitude.setText("Services Stopped");
				// Cancel the notification when the stop button is clicked
				notificationManager.cancel(0);
				new UploadFileTask().execute(gpxfile);
			}
		});
	}

	/**
	 * Pressing the back button will not do anything.
	 */
	@Override
	public void onBackPressed() {
		if (running) {
			// DO NOTHING
		} else {
			super.onBackPressed();
		}
	}

	/**
	 * Create a notification for the app
	 */
	public void createNotification() {
		notification = new Notification(R.drawable.ic_launcher,
				"Location Services Started", System.currentTimeMillis());
		// Set the notification to stay in the bar
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		// Create a new intent that will return to the last activity when click
		// the notification
		// That way is not creating a new Activity of the same class
		Intent intent = new Intent(this, Locator.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, "Location Services Started",
				"Click here to go back to the app", activity);
		notificationManager.notify(0, notification);

	}

	/**
	 * Create the BroadCastReceiver to get data from the service
	 * 
	 * @author Alex
	 * 
	 */
	public class ResponsiveReceiver extends BroadcastReceiver {
		public static final String ACTION_RESP = "com.example.ics499.intent.action.MESSAGE_PROCESSED";

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// Get the new coordinates processed in the service
			double[] coordinates = intent
					.getDoubleArrayExtra(LocationService.outGoingMessage);
			longitude.setText(coordinates[1] + "");
			latitude.setText(coordinates[0] + "");
		}

	}

	/**
	 * Create a background process to upload a file
	 * 
	 * @author Alex
	 * 
	 */
	public class UploadFileTask extends AsyncTask<File, Void, String> {

		@Override
		protected void onPreExecute() {
			Toast.makeText(Locator.this, "Uploading data to server",
					Toast.LENGTH_LONG).show();
		}

		/**
		 * Using the JSch client to SSH to Uhunix.hawaii.edu server Credits to
		 * http://www.jcraft.com/jsch/ Credits also for the sample codes:
		 * http://www.jcraft.com/jsch/examples/
		 * 
		 * @throws JSchException
		 * @throws SftpException
		 */
		@Override
		protected String doInBackground(File... file) {
			String fileName = file[0].getName();
			String filePath = file[0].getPath();
			JSch sshJSch = new JSch();
			Session session = null;
			try {
				session = sshJSch.getSession("ics499", "128.171.10.13", 22);
				session.setConfig("StrictHostKeyChecking", "no");
				session.setPassword("l0cs3rv");
				session.connect();

				Channel channel = session.openChannel("sftp");
				channel.connect();
				ChannelSftp sftpchannel = (ChannelSftp) channel;
				sftpchannel.put(filePath, "/home2/ics499/Data/" +fileName);
				sftpchannel.chmod(0755, "/home2/ics499/Data/" + fileName);
				sftpchannel.exit();
				session.disconnect();

			} catch (JSchException e) {
				Toast.makeText(Locator.this, e.getMessage(), Toast.LENGTH_LONG)
						.show();
			} catch (SftpException e) {
				Toast.makeText(Locator.this, e.getMessage(), Toast.LENGTH_LONG)
						.show();
			}
			return "File \"" + fileName + "\" uploaded to server";
		}

		protected void onPostExecute(String result) {
			Toast.makeText(Locator.this, result, Toast.LENGTH_LONG).show();
		}

	}
}
