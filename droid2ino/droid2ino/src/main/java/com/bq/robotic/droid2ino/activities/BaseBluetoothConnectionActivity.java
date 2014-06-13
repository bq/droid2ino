/*
* This file is part of the Androidino
*
* Copyright (C) 2014 Mundo Reader S.L.
* 
* Date: February 2014
* Author: Estefanía Sarasola Elvira <estefania.sarasola@bq.com>
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3.0 of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/lgpl.html>.
*
*/

package com.bq.robotic.droid2ino.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.bq.robotic.droid2ino.BluetoothConnection;
import com.bq.robotic.droid2ino.DeviceListDialog;
import com.bq.robotic.droid2ino.DialogListener;
import com.bq.robotic.droid2ino.R;
import com.bq.robotic.droid2ino.utils.DeviceListDialogStyle;
import com.bq.robotic.droid2ino.utils.Droid2InoConstants;

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

    // The user accepted to use the Bluetooth with the app
    protected boolean wasEnableBluetoothAllowed = false;

    // The user requested the list of the bluetooth devices available
    protected boolean deviceConnectionWasRequested = false;

	// Store the state of the Bluetooth before this app was executed in order to leave it as it was
	private boolean wasBluetoothEnabled = false;

    private BroadcastReceiver bluetoothDisconnectReceiver;
    private IntentFilter disconnectBluetoothFilter;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			//Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
			return;
		}

        if(savedInstanceState != null) {
            wasEnableBluetoothAllowed = savedInstanceState.getBoolean(Droid2InoConstants.WAS_BLUETOOTH_ALLOWED_KEY);
        }

		if(mBluetoothAdapter.isEnabled()) {
			wasBluetoothEnabled = true;

		}

        bluetoothDisconnectReceiver = new DisconnectBluetoothBroadcastReceiver();
        disconnectBluetoothFilter = new IntentFilter("android.bluetooth.device.action.ACL_DISCONNECTED");

//        else {
//            enableBluetooth();
//        }
	}

	@Override
	protected void onResume() {
		super.onResume();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            return;
        }

        // register the Bluetooth disconnect receiver
        registerReceiver(bluetoothDisconnectReceiver, disconnectBluetoothFilter);

		// If BT is not on, request that it be enabled.
		// setupSession() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled() && wasEnableBluetoothAllowed) {
            mBluetoothAdapter.enable();
            setupSession();

		} else { // Otherwise, setup BT connection
			if (mBluetoothConnection == null)
				setupSession();
		}

	}


    @Override
    protected void onPause() {
        super.onPause();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter != null) {
            // Unregister the bluetooth disconnect receiver
            unregisterReceiver(bluetoothDisconnectReceiver);
        }

    }


    @Override
    protected void onStop() {
        super.onStop();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            return;
        }

        stopApp();
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean(Droid2InoConstants.WAS_BLUETOOTH_ALLOWED_KEY, wasEnableBluetoothAllowed);
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        wasEnableBluetoothAllowed = savedInstanceState.getBoolean(Droid2InoConstants.WAS_BLUETOOTH_ALLOWED_KEY);
    }


    /**
	 * This method stops all threads of the BluetoothConnection and disable the Bluetooth in the
	 * mobile device if it was disabled before this app
	 * This is protected in case that a child wants to close when the user press a button or other view
	 */
	protected void stopApp() {
		
		stopBluetoothConnection();
		
		// Disable the Bluetooth if it was disable before executing this app
		if (mBluetoothAdapter.isEnabled() && !wasBluetoothEnabled) {
			mBluetoothAdapter.disable();
		}
	}
	
	
	/**
	 * this method provides to the child classes a way to stop te bluetooth connection
	 */
	protected void stopBluetoothConnection() {
		// Stop the Bluetooth connect services
		if (mBluetoothConnection != null) mBluetoothConnection.stop();
	}


    /**
     * Enable the Bluetooth of the device
     */
    protected void enableBluetooth() {
        if(wasEnableBluetoothAllowed) {
            mBluetoothAdapter.enable();
            setupSession();
        } else {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Droid2InoConstants.REQUEST_ENABLE_BT);
        }
    }


    /**
     * create a new bluetooth connection
     */
    protected void setupSession() {

        // Initialize the BluetoothConnectService to perform bluetooth connections
        mBluetoothConnection = new BluetoothConnection(this, mHandler);

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
	 * Sends a message.
	 * @param messageBuffer  A string of text to send.
	 */
	protected void sendMessage(byte [] messageBuffer) {
		// Check that we're actually connected before trying anything
		if (!isConnected()) {
			return;
		}

		// Check that there's actually something to send
		if (messageBuffer.length > 0) {
			mBluetoothConnection.write(messageBuffer);

		}
	}
	
	
	/**
	 * Checks if the mobile device is connected to another device
	 * @return
	 */
	protected boolean isConnected() {
		if (mBluetoothAdapter == null || mBluetoothConnection.getState() != Droid2InoConstants.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return false;
		} else {
			return true;
		}
	}
	

	/**
	 * Checks if the mobile device is connected to another device
	 * @return
	 */
	protected boolean isConnectedWithoutToast() {
		if (mBluetoothAdapter == null || mBluetoothConnection == null || mBluetoothConnection.getState() != Droid2InoConstants.STATE_CONNECTED) {
			return false;
		} else {
			return true;
		}
	}
	

	/**
	 * Helper to launch {@link DeviceListDialog}
	 * @param listener
	 */
	private DeviceListDialog deviceListDialog(DialogListener listener) {
		DeviceListDialog deviceDialog = new DeviceListDialog(this, listener);
		deviceDialog.show();
		
		return deviceDialog;
	}


	/**
	 * Launch the {@link DeviceListDialog} to see devices and do scan
	 */
	protected void requestDeviceConnection() {

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            return;
        }

        if (mBluetoothAdapter.isEnabled()) {
            DeviceListDialog deviceDialog = deviceListDialog(new DialogListener() {
                public void onComplete(Bundle values) {
                    connectDevice(values);
                }

                public void onCancel() {
                }
            });
            onDeviceListDialogStyleObtained(deviceDialog.getDialogStyle());

        } else {
            deviceConnectionWasRequested = true;
            enableBluetooth();
        }
	}


    /**
     * Shows the Bluetooth devices available list to the user, when the user requested it
     * but the Bluetooth wasn't enable, and the list must wait to the Bluetooth being enable
     * for showing it
     */
    private void showListDialog() {
        if(mBluetoothAdapter.isEnabled()) {
            DeviceListDialog deviceDialog = deviceListDialog(new DialogListener() {
                public void onComplete(Bundle values) {
                    connectDevice(values);
                }

                public void onCancel() {
                }
            });

            onDeviceListDialogStyleObtained(deviceDialog.getDialogStyle());
            deviceConnectionWasRequested = false;

        } else {
            deviceConnectionWasRequested = false;
        }
    }


    /**
     * Style the lists with the bluetooth devices
     * @return the styling class for the lists with the devices
     */
    protected void onDeviceListDialogStyleObtained(DeviceListDialogStyle deviceListDialogStyle) {
        // By default, do nothing
    }


	private void connectDevice(Bundle values) {
		// Get the device MAC address
		String address = values.getString(Droid2InoConstants.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mBluetoothConnection.connect(device);
	}

	private void connectDevice(Intent data) {
		// Get the device MAC address
		String address = data.getExtras().getString(Droid2InoConstants.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mBluetoothConnection.connect(device);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
            case Droid2InoConstants.REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;

            case Droid2InoConstants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // User accepted to enable the bluetooth
                    wasEnableBluetoothAllowed = true;

                    // Bluetooth is now enabled, so set up a session
                    setupSession();

                    // If the user requested the list of the bluetooth devices available, show it
                    if(deviceConnectionWasRequested) {
                        showListDialog();
                    }

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(LOG_TAG, "BT not enabled");
                    wasEnableBluetoothAllowed = false;

                    Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();

                    // If the user requested the list of the bluetooth devices available, show it
                    if(deviceConnectionWasRequested) {
                        showListDialog();
                    }
                }
		}
	}	

	// The Handler that gets information back from the BluetoothConnectService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Droid2InoConstants.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case Droid2InoConstants.STATE_CONNECTED:
				case Droid2InoConstants.STATE_CONNECTING:
				case Droid2InoConstants.STATE_LISTEN:
				case Droid2InoConstants.STATE_NONE:
					onConnectionStatusUpdate(msg.arg1);
					break;
				}
				break;
				
			case Droid2InoConstants.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				onWriteSuccess(writeMessage);
				break;
				
			case Droid2InoConstants.MESSAGE_READ:
				// construct a string from the valid bytes in the buffer
				String readMessage = (String) msg.obj;
				onNewMessage(readMessage);	
				break;
				
			case Droid2InoConstants.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(Droid2InoConstants.DEVICE_NAME);
				Toast.makeText(getApplicationContext(), getString(R.string.connected_to) + 
						mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
				
			case Droid2InoConstants.MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), 
						msg.getData().getString(Droid2InoConstants.TOAST), Toast.LENGTH_SHORT).show();
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



    /***********************************************************************************************
     *
     * This is the bluetooth disconnect broadcast receiver. When a device is disconnected, this
     * class is triggered and stops the connected thread. This is an inner class in order to call
     * easier the stop() method of the BluetoothConnection object. Furthermore, the app disable
     * the Bluetooth when is not visible, so it has no sense to have this in the manifest and be
     * called always, because the connection is already closed in that cases.
     *
     **********************************************************************************************/

    public class DisconnectBluetoothBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.i(LOG_TAG, "The connection was lost. The Bluetooth device was disconnected.");
                mBluetoothConnection.stop();
            }

        }

    }

}
