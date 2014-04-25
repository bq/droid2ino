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

package com.bq.robotic.droid2ino;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bq.robotic.droid2ino.utils.Droid2InoConstants;
import com.bq.robotic.droid2ino.utils.DeviceListDialogStyle;

import java.util.Set;

/**
 * This dialog lists any paired devices and devices detected in the area 
 * after discovery. When a device is chosen by the user, the MAC address 
 * of the device is sent back to the parent Activity in the result Intent.
 * 
 */

public class DeviceListDialog extends Dialog {
    // Debugging
    private static final String LOG_TAG = "DeviceListActivity";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    
    private DeviceListDialogStyle mDialogStyle;
    
    private DialogListener mListener;
    
    public DeviceListDialog(Context context, DialogListener listener) {
        super(context);
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.device_list);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // Text to show when there isn't any devices paired to show in the list
        TextView emptyPairedDevicesListItem = (TextView) findViewById(R.id.paired_devices_empty_item);

        // Text to show when none device was found when the discovery to show in the list
        TextView emptyNewDevicesListItem = (TextView) findViewById(R.id.new_devices_empty_item);

        // Initialize the object for the styling modifications of the search bluetooth device dialog
        mDialogStyle = new DeviceListDialogStyle((TextView) findViewById(R.id.dialog_title), 
        		(TextView) findViewById(R.id.title_paired_devices), 
        		(TextView) findViewById(R.id.title_new_devices));
        
        
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        pairedListView.setEmptyView(emptyPairedDevicesListItem);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        newDevicesListView.setEmptyView(emptyNewDevicesListItem);
        emptyNewDevicesListItem.setVisibility(View.GONE);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getContext().registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getContext().registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
            	
                mPairedDevicesArrayAdapter.add(device.getName() + Droid2InoConstants.NEW_LINE_CHARACTER +
                		device.getAddress());
            }
        }
    }
    

	@Override
    protected void onStop() {
        super.onStop();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        getContext().unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (Droid2InoConstants.D) Log.d(LOG_TAG, "doDiscovery()");

        // Indicate scanning in the title
//        setProgressBarIndeterminateVisibility(true);
        TextView dialogTitle = (TextView) findViewById(R.id.dialog_title);
        dialogTitle.setText(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(Droid2InoConstants.EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            Bundle values = new Bundle();
            values.putString(Droid2InoConstants.EXTRA_DEVICE_ADDRESS, address);
            if(mListener != null) mListener.onComplete(values);
            dismiss();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                // Change the weight of the list of paired devices in order to show the new devices 
                ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
                p.weight = 1;
                pairedListView.setLayoutParams(p);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + 
                    		Droid2InoConstants.NEW_LINE_CHARACTER + device.getAddress());
                }

            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                setProgressBarIndeterminateVisibility(false);
                TextView dialogTitle = (TextView) findViewById(R.id.dialog_title);
                dialogTitle.setText(R.string.select_device);
            }
        }
    };
    
    
	public DeviceListDialogStyle getDialogStyle() {
		return mDialogStyle;
	}      

}
