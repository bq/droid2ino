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

public class BaseBluetoothConnectionActivity extends ActionBarActivity {

	/**
	 * This is the main abstract Activity that can be used by clients to setup Bluetooth.
	 * It provides helper methods that can be used to find and connect devices.
	 */
	public abstract class BaseConnectActivity extends ActionBarActivity {
		private static final String TAG = "BaseConnectActivity";

		public static final int REQUEST_CONNECT_DEVICE = 1;
		public static final int REQUEST_ENABLE_BT = 2;

		// Name of the connected device
		protected String mConnectedDeviceName = null;
		// String buffer for outgoing messages
		protected StringBuffer mOutStringBuffer;
		// Local Bluetooth adapter
		protected BluetoothAdapter mBluetoothAdapter = null;
		// Member object for the BT connect services
		protected BluetoothConnection mConnectService = null;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Get local Bluetooth adapter
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			// If the adapter is null, then Bluetooth is not supported
			if (mBluetoothAdapter == null) {
				Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}

		@Override
		public void onStart() {
			super.onStart();

			// If BT is not on, request that it be enabled.
			// setupSession() will then be called during onActivityResult
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

			} else { // Otherwise, setup BT connection
				if (mConnectService == null)
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
			if (mConnectService != null) {
				// Only if the state is STATE_NONE, do we know that we haven't
				// started already
				if (mConnectService.getState() == BluetoothConnection.STATE_NONE) {
					// Start the Bluetooth services
					mConnectService.start();
				}
			}
		}

		/**
		 * create a new bluetooth connection
		 */
		private void setupSession() {

			// Initialize the BluetoothConnectService to perform bluetooth connections
			mConnectService = new BluetoothConnection(this, mHandler);

			// Initialize the buffer for outgoing messages
			mOutStringBuffer = new StringBuffer("");

		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			// Stop the Bluetooth connect services
			if (mConnectService != null) mConnectService.stop();
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
			if (mConnectService.getState() != BluetoothConnection.STATE_CONNECTED) {
				Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
				return;
			}

			// Check that there's actually something to send
			if (message.length() > 0) {
				// Get the message bytes and tell the BluetoothConnectService to write
				byte[] send = message.getBytes();
				mConnectService.write(send);

				// Reset out string buffer to zero and clear the edit text field
				mOutStringBuffer.setLength(0);
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
			String address = values.getString(DeviceListDialog.EXTRA_DEVICE_ADDRESS);
			// Get the BluetoothDevice object
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			// Attempt to connect to the device
			mConnectService.connect(device);
		}

		private void connectDevice(Intent data) {
			// Get the device MAC address
			String address = data.getExtras().getString(DeviceListDialog.EXTRA_DEVICE_ADDRESS);
			// Get the BluetoothDevice object
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			// Attempt to connect to the device
			mConnectService.connect(device);
		}

		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch (requestCode) {
			case REQUEST_CONNECT_DEVICE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					connectDevice(data);
				}
				break;        
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a session
					setupSession();
				} else {
					// User did not enable Bluetooth or an error occurred
					Log.d(TAG, "BT not enabled");
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
				case BluetoothConnection.MESSAGE_STATE_CHANGE:
					switch (msg.arg1) {
					case BluetoothConnection.STATE_CONNECTED:
					case BluetoothConnection.STATE_CONNECTING:
					case BluetoothConnection.STATE_LISTEN:
					case BluetoothConnection.STATE_NONE:
						onConnectionStatusUpdate(msg.arg1);
						break;
					}
					break;
				case BluetoothConnection.MESSAGE_WRITE:
					byte[] writeBuf = (byte[]) msg.obj;
					// construct a string from the buffer
					String writeMessage = new String(writeBuf);
					onWriteSuccess(writeMessage);
					break;
				case BluetoothConnection.MESSAGE_READ:
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buffer
					String readMessage = new String(readBuf, 0, msg.arg1);
					onNewMessage(readMessage);	
					break;
				case BluetoothConnection.MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(BluetoothConnection.DEVICE_NAME);
					Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
					break;
				case BluetoothConnection.MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothConnection.TOAST), Toast.LENGTH_SHORT).show();
					break;
				}
			}
		};

		/**
		 * Callback that will be invoked when Bluetooth connectivity state changes
		 * @param connectionState Message types sent from the BluetoothConnectService Handler
		 */
		public void onConnectionStatusUpdate(int connectionState) {
			Log.d(TAG, "Connectivity changed  : " + connectionState);
		}

		/**
		 * Callback that will be called after message was sent successfully.
		 * @param message data that was sent to remote device
		 */
		public void onWriteSuccess(String message) {
			Log.d(TAG, "Response message : " + message);
		}

		/**
		 * Callback that will be invoked when new message is received
		 * @param message new message string
		 */
		public abstract void onNewMessage(String message);
	}
}
