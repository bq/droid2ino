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

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import com.bq.robotic.droid2ino.communication.BtControllerInterface
import com.bq.robotic.droid2ino.utils.Droid2InoConstants.ConnectionState
import com.bq.robotic.droid2ino.utils.HandlerUtils

class BtSocketController(private val btAdapter: BluetoothAdapter): BtControllerInterface {
    private val LOG_TAG = this.javaClass.simpleName

    // Member object for the BT connect services
    private var btSocketConnection: BtSocketConnection? = null
    private val bluetoothDisconnectReceiver: BroadcastReceiver by lazy { DisconnectBluetoothBroadcastReceiver() }
    private val disconnectBluetoothFilter: IntentFilter by lazy { IntentFilter("android.bluetooth.device.action.ACL_DISCONNECTED") }

    // Used while trying to write into the bluetooth connection. This is needed in order to avoid an
    // ANR exception in slow bluetooth connections, or when the connection is going to be lost in
    // some old devices
    private var sendHandler: Handler? = null

    override fun prepareBtEnvironment(context: Context, communicationHandler: Handler) {
        // Initialize the BluetoothConnectService to perform bluetooth connections
        btSocketConnection = BtSocketConnection(context, communicationHandler)
    }

    /**
     * Helper method to start discovering devices.
     */
    fun ensureDiscoverable(activity: Activity) {
        if (btAdapter.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            activity.startActivity(discoverableIntent)
        }
    }

    /**
     * Request this bt socket connection to be duplex if {@param isDuplex} is true, or simplex if
     * it's false. By default, the connection is duplex. In a duplex connection the app can read
     * from the connected device and also write messages to it. In a simplex connection the app can
     * only write messages to the connected device.
     */
    fun setDuplexConnection(isDuplex: Boolean) {
        btSocketConnection?.isDuplexConnection = isDuplex
        Log.d(LOG_TAG, "Set connection to ${ if(isDuplex) "duplex" else "simplex" }")
    }

    override fun connectToBtDevice(context: Context, address: String) {
        registerReceivers(context)
        if (sendHandler == null) sendHandler = HandlerUtils.createHandler("btSocketSendHandler")

        // Get the BluetoothDevice object
        val device = btAdapter.getRemoteDevice(address)
        // Attempt to connect to the device
        device?.let {
            btSocketConnection?.connect(it)
        }
    }

    override fun stopBtConnection(context: Context) {
        unregisterReceivers(context)
        // Stop the Bluetooth connect services
        btSocketConnection?.stop()
        releaseResourcesOnDestroy()
    }

    override fun sendMessage(message: String) {
        // Check that we're actually connected before trying anything
        if (!isConnected()) {
            return
        }

        // Check that there's actually something to send
        if (!message.isNullOrBlank()) {
            // Get the message bytes and tell the BluetoothConnectService to write
            val send = message.toByteArray()

            sendHandler?.post { btSocketConnection?.write(send) }
        }
    }

    override fun sendMessage(messageBuffer: ByteArray) {
        // Check that we're actually connected before trying anything
        if (!isConnected()) {
            return
        }

        // Check that there's actually something to send
        if (messageBuffer.isNotEmpty()) {
            sendHandler?.post { btSocketConnection?.write(messageBuffer) }
        }
    }

    override fun isConnected() = btSocketConnection?.state == ConnectionState.CONNECTED_CONFIGURED

    private fun registerReceivers(context: Context) {
        // register the Bluetooth disconnect receiver
        context.registerReceiver(bluetoothDisconnectReceiver, disconnectBluetoothFilter)
    }

    private fun unregisterReceivers(context: Context) {
        try {
            // Unregister the bluetooth disconnect receiver
            context.unregisterReceiver(bluetoothDisconnectReceiver)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error unregistering the bluetooth receiver")
        }
    }

    private fun releaseResourcesOnDestroy() {
        // Quit the send handler and it looper
        sendHandler?.removeCallbacksAndMessages(null)
        sendHandler?.looper?.quit()
        sendHandler = null
    }


    /***********************************************************************************************
     *
     * This is the bluetooth disconnect broadcast receiver. When a device is disconnected, this
     * class is triggered and stops the connected thread. This is an inner class in order to call
     * easier the stop() method of the BluetoothConnection object. Furthermore, the app disable
     * the Bluetooth when is not visible, so it has no sense to have this in the manifest and be
     * called always, because the connection is already closed in that cases.
     */
    inner class DisconnectBluetoothBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == intent.action) {
                Log.d(LOG_TAG, "The connection was lost. The Bluetooth device was disconnected.")
                stopBtConnection(context)
            }
        }
    }

}