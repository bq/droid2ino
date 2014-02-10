package com.bq.robotic.androidino.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.bq.robotic.androidino.BluetoothConnection;
import com.bq.robotic.androidino.DeviceListDialog;
import com.bq.robotic.androidino.DialogListener;
import com.bq.robotic.androidino.R;
import com.bq.robotic.androidino.utils.AndroidinoConstants;

public abstract class BaseBluetoothConnectionActivity extends ActionBarActivity {

	/**
	 * This is the main abstract Activity that can be used by clients to setup Bluetooth.
	 * It provides helper methods that can be used to find and connect devices.
	 */

	private static final String LOG_TAG = "BaseConnectActivity";

	// Name of the connected device
	protected String mConnectedDeviceName = null;
	// String buffer for outgoing messages
//	protected StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	protected BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the BT connect services
	protected BluetoothConnection mBluetoothConnection = null;
	
	// Store the state of the Bluetooth before this app was executed in order to leave it as it was
	private boolean wasBluetoothEnabled = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		if(mBluetoothAdapter.isEnabled()) {
			wasBluetoothEnabled = true;
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		// If BT is not on, request that it be enabled.
		// setupSession() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, AndroidinoConstants.REQUEST_ENABLE_BT);

		} else { // Otherwise, setup BT connection
			if (mBluetoothConnection == null)
				setupSession();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
//		if (mBluetoothConnection != null) {
//			// Only if the state is STATE_NONE, do we know that we haven't
//			// started already
//			if (mBluetoothConnection.getState() == AndroidinoConstants.STATE_NONE) {
//				// Start the Bluetooth services
//				mBluetoothConnection.start();
//			}
//		}
	}
	
	
//	@Override
//	public synchronized void onPause() {
//	    super.onPause();
//	    if (mBluetoothConnection != null) mBluetoothConnection.stop();
//	    if(AndroidinoConstants.D) Log.e(LOG_TAG, "- ON PAUSE -");
//	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		stopApp();
	}
	
	
	/**
	 * This method stops all threads of the BluetoothConnection and disable the Bluetooth in the
	 * mobile device if it was disabled before this app
	 * This is protected in case that a child wants to close when the user press a button or other view
	 */
	protected void stopApp() {
		// Stop the Bluetooth connect services
		if (mBluetoothConnection != null) mBluetoothConnection.stop();
		
		// Disable the Bluetooth if it was disable before executing this app
		if (mBluetoothAdapter.isEnabled() && !wasBluetoothEnabled) {
			mBluetoothAdapter.disable();
		}
	}
	
	
	/**
	 * create a new bluetooth connection
	 */
	private void setupSession() {

		// Initialize the BluetoothConnectService to perform bluetooth connections
		mBluetoothConnection = new BluetoothConnection(this, mHandler);

		// Initialize the buffer for outgoing messages
//		mOutStringBuffer = new StringBuffer("");

	}
	

	/**
	 * Helper method to start discovering devices. 
	 */
	protected void ensureDiscoverable() {
		if (mBluetoothAdapter.getScanMode() !=
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * @param message  A string of text to send.
	 */
	protected void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (!isConnected()) {
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothConnectService to write
			byte[] send = message.getBytes();
			mBluetoothConnection.write(send);

			// Reset out string buffer to zero and clear the edit text field
//			mOutStringBuffer.setLength(0);
		}
	}
	
	
	/**
	 * Checks if the mobile device is connected to another device
	 * @return
	 */
	protected boolean isConnected() {
		if (mBluetoothConnection.getState() != AndroidinoConstants.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return false;
		} else {
			return true;
		}
	}
	

	/**
	 * Helper to launch {@link DeviceListDialog}
	 * @param listener
	 */
	private void deviceListDialog(DialogListener listener) {
		new DeviceListDialog(this, listener).show();
	}

	/**
	 * Launch the {@link DeviceListDialog} to see devices and do scan
	 */
	protected void requestDeviceConnection() {
		deviceListDialog(new DialogListener() {
			public void onComplete(Bundle values) {
				connectDevice(values);
			}
			public void onCancel() {}
		});
	}

	private void connectDevice(Bundle values) {
		// Get the device MAC address
		String address = values.getString(AndroidinoConstants.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mBluetoothConnection.connect(device);
	}

	private void connectDevice(Intent data) {
		// Get the device MAC address
		String address = data.getExtras().getString(AndroidinoConstants.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mBluetoothConnection.connect(device);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case AndroidinoConstants.REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data);
			}
			break;        
		case AndroidinoConstants.REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a session
				setupSession();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(LOG_TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}	

	// The Handler that gets information back from the BluetoothConnectService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case AndroidinoConstants.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case AndroidinoConstants.STATE_CONNECTED:
				case AndroidinoConstants.STATE_CONNECTING:
				case AndroidinoConstants.STATE_LISTEN:
				case AndroidinoConstants.STATE_NONE:
					onConnectionStatusUpdate(msg.arg1);
					break;
				}
				break;
				
			case AndroidinoConstants.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				onWriteSuccess(writeMessage);
				break;
				
			case AndroidinoConstants.MESSAGE_READ:
				// construct a string from the valid bytes in the buffer
				String readMessage = (String) msg.obj;
				onNewMessage(readMessage);	
				break;
				
			case AndroidinoConstants.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(AndroidinoConstants.DEVICE_NAME);
				Toast.makeText(getApplicationContext(), getString(R.string.connected_to) + 
						mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
				
			case AndroidinoConstants.MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), 
						msg.getData().getString(AndroidinoConstants.TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	/**
	 * Callback that will be invoked when Bluetooth connectivity state changes
	 * @param connectionState Message types sent from the BluetoothConnectService Handler
	 */
	public void onConnectionStatusUpdate(int connectionState) {
		Log.d(LOG_TAG, "Connectivity changed  : " + connectionState);
	}

	/**
	 * Callback that will be called after message was sent successfully.
	 * @param message data that was sent to remote device
	 */
	public void onWriteSuccess(String message) {
		Log.d(LOG_TAG, "Response message : " + message);
	}

	/**
	 * Callback that will be invoked when new message is received
	 * @param message new message string
	 */
	public abstract void onNewMessage(String message);

}
