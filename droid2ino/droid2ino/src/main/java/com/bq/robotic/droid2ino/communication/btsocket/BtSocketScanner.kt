/*
 * This file is part of the Androidino
 *
 * Copyright (C) 2017 Mundo Reader S.L.
 *
 * Date: August 2018
 * Author: Estefanía Sarasola Elvira <estefania.sarasola@bq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.bq.robotic.droid2ino.communication.btsocket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.bq.robotic.droid2ino.communication.BtScanner

class BtSocketScanner(private val context: Context): BtScanner {
    private val LOG_TAG = this.javaClass.simpleName

    private val btAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val btDiscoveryReceiver by lazy { createBtDiscoveryReceiver() }
    private var scanListener: BtScanner.BtScanListener? = null

    private var isScanning = false

    override fun setBtScanListener(scanListener: BtScanner.BtScanListener) {
        this.scanListener = scanListener
    }

    override fun getPairedBtDevices(): List<BluetoothDevice>? = btAdapter.bondedDevices.toList()

    /**
     * Start device discover with the BluetoothAdapter
     */
    override fun scanForBtDevices(): Boolean {
        // If we're already discovering, stop it
        if (isScanning) {
            return false
        }

        isScanning = true

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(btDiscoveryReceiver, filter)
        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        context.registerReceiver(btDiscoveryReceiver, filter)

        // Request discover from BluetoothAdapter
        btAdapter.startDiscovery()
        return true
    }

    override fun stopScan() {
        if (!isScanning) return

        Log.d(LOG_TAG, "Stopping scan...")
        isScanning = false

        // Make sure we're not doing discovery anymore
        btAdapter.cancelDiscovery()

        // Unregister broadcast listeners
        try {
            context.unregisterReceiver(btDiscoveryReceiver)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error unregistering the bluetooth receiver: $e")
        }

        scanListener?.onScanFinished()
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private fun createBtDiscoveryReceiver() = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null)
                    scanListener?.onDeviceFound(device)

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                isScanning = false
                context.unregisterReceiver(this)
                scanListener?.onScanFinished()
            }
        }
    }
}