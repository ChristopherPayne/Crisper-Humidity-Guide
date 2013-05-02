package com.sensorcon.crisperhumidityguide;

import java.util.EventObject;

import com.sensorcon.sdhelper.ConnectionBlinker;
import com.sensorcon.sdhelper.SDHelper;
import com.sensorcon.sdhelper.SDStreamer;
import com.sensorcon.sensordrone.Drone;
import com.sensorcon.sensordrone.Drone.DroneEventListener;
import com.sensorcon.sensordrone.Drone.DroneStatusListener;

import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends Activity {

	/*
	 * GUI Variables
	 */
	private ImageButton button1;
	private ImageButton button2;
	private ImageButton button3;
	private ImageButton xButton;
	private ImageButton xButton2;
	private PopupWindow popup;
	private PopupWindow instPopup;
	private TextView tv_humidityVal;
	private TextView tv_tempVal;
	private TextView tv_popupDescription;
	private TextView tv_popupHumidity;
	private TextView tv_popupExamples;
	/*
	 * IO variables
	 */
	PreferencesStream pStream;
	/*
	 * Data variables
	 */
	private int humidityVal;
	private int tempVal_F;
	private int tempVal_C;
	/*
	 * Sensordone variables
	 */
	protected Drone myDrone;
	public Storage box;
	public SDHelper myHelper;
	private Handler myHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		popup = new PopupWindow(this);

		humidityVal = 0;
		tempVal_F = 0;
		tempVal_C = 0;
		tv_humidityVal = (TextView)findViewById(R.id.humidityValue);
		tv_tempVal = (TextView)findViewById(R.id.tempValue);
		button1 = (ImageButton)findViewById(R.id.button1);
		button2 = (ImageButton)findViewById(R.id.button2);
		button3 = (ImageButton)findViewById(R.id.button3);
		
		tv_tempVal.setVisibility(View.GONE);

		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View layout = inflater.inflate(R.layout.popup,
				(ViewGroup) findViewById(R.id.popup_element));

		popup = new PopupWindow(
				layout, 
				740, 
				360, 
				true);

		LayoutInflater inflater2 = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View layout2 = inflater2.inflate(R.layout.inst_popup,
				(ViewGroup) findViewById(R.id.popup_element));
		
		instPopup = new PopupWindow(
				layout2, 
				600, 
				600, 
				true);
		
		tv_popupHumidity = (TextView)layout.findViewById(R.id.popupHumidity);
		tv_popupDescription = (TextView)layout.findViewById(R.id.popupDescription);
		tv_popupExamples = (TextView)layout.findViewById(R.id.popupExamples);
		
		xButton = (ImageButton)layout.findViewById(R.id.xButton);
		xButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				popup.dismiss();
			}	
		});
		
		xButton2 = (ImageButton)layout2.findViewById(R.id.xButton);
		xButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				instPopup.dismiss();
			}	
		});

		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopup1();
			}		
		});	
		
		button2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopup2();
			}		
		});	
		
		button3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopup3();
			}		
		});	
		

		myDrone = new Drone();
		box = new Storage(this);
		myHelper = new SDHelper();
		
		// Check to see if user still wants intro screen to show
		pStream = new PreferencesStream();
		pStream.initFile(this);
		String[] preferences = new String[1];
		preferences = pStream.readPreferences();
		
		if(!preferences[0].equals("DISABLE INTRO")){
			showIntroDialog();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.disconnect:
			// Only disconnect if it's connected
			if (myDrone.isConnected) {
				// Run our routine of things to do on disconnect
				doOnDisconnect();
			} else {
			}
			break;
		case R.id.connect:
			myHelper.scanToConnect(myDrone, MainActivity.this , this, false);
			break;
		case R.id.instructions:
			showInstructions();
			break;
		}

		return true;
	}

	/**
	 * Loads the dialog shown at startup
	 */
	public void showIntroDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(false);
		alert.setTitle("Introduction").setMessage(R.string.instructions);
		alert.setPositiveButton("Don't Show Again", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            pStream.disableIntroDialog();
		        }
		     })
		    .setNegativeButton("Okay", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            // do nothing
		        }
		     }).show();
	}
	
	/**
	 * Shows a simple message on the screen
	 * 
	 * @param msg	Message to be displayed
	 */
	public void quickMessage(final String msg) {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showPopup1() {
		tv_popupHumidity.setText(R.string.humidity1);
		tv_popupDescription.setText(R.string.description1);
		tv_popupExamples.setText(R.string.examples1);

		popup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showPopup2() {
		tv_popupHumidity.setText(R.string.humidity2);
		tv_popupDescription.setText(R.string.description2);
		tv_popupExamples.setText(R.string.examples2);
		
		popup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showPopup3() {
		tv_popupHumidity.setText(R.string.humidity3);
		tv_popupDescription.setText(R.string.description3);
		tv_popupExamples.setText(R.string.examples3);
		
		popup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}
	
	/**
	 * Makes informative popup appear
	 */
	public void showInstructions() {
		instPopup.showAtLocation(findViewById(R.id.anchor), Gravity.CENTER, 0, 0);
	}

	/**
	 * Things to do when drone is disconnected
	 */
	public void doOnDisconnect() {

		// Shut off any sensors that are on
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// Turn off myBlinker
				box.myBlinker.disable();
				tv_tempVal.setVisibility(View.GONE);

				// Make sure the LEDs go off
				if (myDrone.isConnected) {
					myDrone.setLEDs(0, 0, 0);
				}

				// Only try and disconnect if already connected
				if (myDrone.isConnected) {
					myDrone.disconnect();
				}
			}
		});
	}

	public Runnable displayHumidityRunnable = new Runnable() {

		@Override
		public void run() {
			if(myDrone.isConnected) {
				tv_humidityVal.setText(Integer.toString(humidityVal) + "% R.H.");
				tv_tempVal.setText(Integer.toString(tempVal_F) + (char) 0x00B0 + " F / " + Integer.toString(tempVal_C) + (char) 0x00B0 + " C");
				
				myHandler.postDelayed(this, 1000);
			}
			else {
				tv_humidityVal.setText("Not Connected");
			}
		}
	};
	
	/*
	 * Because Android will destroy and re-create things on events like orientation changes,
	 * we will need a way to store our objects and return them in such a case. 
	 * 
	 * A simple and straightforward way to do this is to create a class which has all of the objects
	 * and values we want don't want to get lost. When our orientation changes, it will reload our
	 * class, and everything will behave as normal! See onRetainNonConfigurationInstance in the code
	 * below for more information.
	 * 
	 * A lot of the GUI set up will be here, and initialized via the Constructor
	 */
	public final class Storage {

		// A ConnectionBLinker from the SDHelper Library
		public ConnectionBlinker myBlinker;

		// Holds the sensor of interest - the CO precision sensor
		public int sensor1;
		public int sensor2;

		// Our Listeners
		public DroneEventListener droneEventListener;
		public DroneStatusListener droneStatusListener;
		public String MAC = "";

		// GUI variables
		public TextView statusView;
		public TextView tvConnectionStatus;
		public TextView tvConnectInfo;

		// Streams data from sensor
		public SDStreamer streamer1;
		public SDStreamer streamer2;

		public Storage(Context context) {

			// Initialize sensor
			sensor1 = myDrone.QS_TYPE_HUMIDITY;
			sensor2 = myDrone.QS_TYPE_TEMPERATURE;

			// This will Blink our Drone, once a second, Blue
			myBlinker = new ConnectionBlinker(myDrone, 1000, 0, 255, 0);

			streamer1 = new SDStreamer(myDrone, sensor1);
			streamer2 = new SDStreamer(myDrone, sensor2);

			/*
			 * Let's set up our Drone Event Listener.
			 * 
			 * See adcMeasured for the general flow for when a sensor is measured.
			 * 
			 */
			droneEventListener = new DroneEventListener() {

				@Override
				public void connectEvent(EventObject arg0) {

					quickMessage("Connected!");

					streamer1.enable();
					streamer2.enable();
					myDrone.quickEnable(sensor1);
					myDrone.quickEnable(sensor2);

					// Flash teh LEDs green
					myHelper.flashLEDs(myDrone, 3, 100, 0, 0, 22);
					// Turn on our blinker
					myBlinker.enable();
					myBlinker.run();
					
					tv_tempVal.setVisibility(View.VISIBLE);
					myHandler.post(displayHumidityRunnable);
				}


				@Override
				public void connectionLostEvent(EventObject arg0) {
					// Turn off the blinker
					myBlinker.disable();
					doOnDisconnect();
				}

				@Override
				public void disconnectEvent(EventObject arg0) {
					doOnDisconnect();
					tv_humidityVal.setText("Not Connected");
				}

				@Override
				public void humidityMeasured(EventObject arg0) {
					humidityVal = (int)myDrone.humidity_Percent;
					Log.d("chris", Integer.toString(humidityVal));
	
					streamer1.streamHandler.postDelayed(streamer1, 250);
				}
				
				@Override
				public void temperatureMeasured(EventObject arg0) {
					tempVal_F = (int)myDrone.temperature_Farenheit;
					tempVal_C = (int)myDrone.temperature_Celcius;
					Log.d("chris", Integer.toString(humidityVal));
	
					streamer2.streamHandler.postDelayed(streamer2, 250);
				}
				
				/*
				 * Unused events
				 */
				@Override
				public void customEvent(EventObject arg0) {}
				@Override
				public void adcMeasured(EventObject arg0) {}
				@Override
				public void precisionGasMeasured(EventObject arg0) {}
				@Override
				public void altitudeMeasured(EventObject arg0) {}
				@Override
				public void capacitanceMeasured(EventObject arg0) {}
				@Override
				public void i2cRead(EventObject arg0) {}
				@Override
				public void irTemperatureMeasured(EventObject arg0) {}
				@Override
				public void oxidizingGasMeasured(EventObject arg0) {}
				@Override
				public void pressureMeasured(EventObject arg0) {}
				@Override
				public void reducingGasMeasured(EventObject arg0) {}
				@Override
				public void rgbcMeasured(EventObject arg0) {}
				@Override
				public void uartRead(EventObject arg0) {}
				@Override
				public void unknown(EventObject arg0) {}
				@Override
				public void usbUartRead(EventObject arg0) {}
			};

			/*
			 * Set up our status listener
			 * 
			 * see adcStatus for the general flow for sensors.
			 */
			droneStatusListener = new DroneStatusListener() {

				@Override
				public void humidityStatus(EventObject arg0) {
					streamer1.run();
				}
				@Override
				public void temperatureStatus(EventObject arg0) {
					streamer2.run();
				}

				/*
				 * Unused statuses
				 */
				@Override
				public void adcStatus(EventObject arg0) {}
				@Override
				public void altitudeStatus(EventObject arg0) {}
				@Override
				public void batteryVoltageStatus(EventObject arg0) {}
				@Override
				public void capacitanceStatus(EventObject arg0) {}
				@Override
				public void chargingStatus(EventObject arg0) {}
				@Override
				public void customStatus(EventObject arg0) {}
				@Override
				public void precisionGasStatus(EventObject arg0) {}
				@Override
				public void irStatus(EventObject arg0) {}
				@Override
				public void lowBatteryStatus(EventObject arg0) {}
				@Override
				public void oxidizingGasStatus(EventObject arg0) {}
				@Override
				public void pressureStatus(EventObject arg0) {}
				@Override
				public void reducingGasStatus(EventObject arg0) {}
				@Override
				public void rgbcStatus(EventObject arg0) {}
				@Override
				public void unknownStatus(EventObject arg0) {}
			};

			// Register the listeners
			myDrone.registerDroneEventListener(droneEventListener);
			myDrone.registerDroneStatusListener(droneStatusListener);
		}
	}
}