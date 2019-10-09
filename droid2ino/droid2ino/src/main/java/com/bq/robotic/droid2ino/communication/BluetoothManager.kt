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

package com.bq.robotic.droid2ino.communication

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import com.bq.robotic.droid2ino.utils.Droid2InoConstants
import com.bq.robotic.droid2ino.utils.Droid2InoConstants.ConnectionState
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.bq.robotic.droid2ino.R
import com.bq.robotic.droid2ino.communication.ble.BleController
import com.bq.robotic.droid2ino.communication.ble.BleProfile
import com.bq.robotic.droid2ino.communication.btsocket.BtSocketController
import com.bq.robotic.droid2ino.utils.ConnectionErrorFeedback

class BluetoothManager(ctx: Context) {
    private val LOG_TAG = this.javaClass.simpleName

    /**
     * Types of bluetooth connection that can be selected.
     */
    enum class BtConnectionType { BT_SOCKET, BLE;
        companion object {
            val DEFAULT = BT_SOCKET
        }
    }

    /**
     * Current bluetooth connection type.
     * See [BtConnectionType].
     */
    var btConnectionType = BtConnectionType.DEFAULT
        private set // The setter is private and has the default implementation

    /**
     * Listener used to communicate changes or the results of requests.
     */
    var btCommunicationListener: BtCommunicationListener? = null

    /**
     * The user accepted that the app can use the Bluetooth and enable/disable it when needed.
     */
    var isEnableBluetoothAllowed = false
        private set // The setter is private and has the default implementation

    // Store the state of the Bluetooth before this app was executed in order to leave it as it was
    private var wasBluetoothEnabled = false

    /**
     * By default the BT socket option is duplex. This variable will be used when creating the
     * [BtDevicesListDialog] so when the user clicks on the BT socket option it will set duplex or simplex
     * and when preparing a new [BtSocketConnection]
     */
    var isBtSocketDuplexRequested = BtConnectionType.BT_SOCKET == BtConnectionType.DEFAULT
        private set // The setter is private and has the default implementation

    private val context = ctx.applicationContext
    private var currentBtController: BtControllerInterface? = null

    private val btAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter() ?: throw NullPointerException()
    }
    // Custom [BroadcastReceiver] to manage Bluetooth state changes
    private var btAdapterChangesReceiver: BroadcastReceiver? = null

    /**
     * By default the [BqZumCoreProfile] will be used. Store here the profile to use if another one
     * is required to be used.
     */
    private var customBleProfileRequested: BleProfile? = null

    /**
     * Return true if the bluetooth is currently enabled and ready for use.
     */
    fun isBtAdapterEnabled() = btAdapter.isEnabled

    /**
     * Enable the Bluetooth of the device.
     */
    fun enableBluetooth(activity: Activity) {
        if (isEnableBluetoothAllowed) {
            try {
                registerBtAdapterChangesReceiver()
                btAdapter.enable()
            } catch (se: SecurityException) {
                Log.d(LOG_TAG,"Enabling the BT failed, retry asking the permission again")
                isEnableBluetoothAllowed = false
                unregisterBtAdapterChangesReceiver()
                requestEnableBtPermission(activity)
            }
        } else {
            requestEnableBtPermission(activity)
        }
    }

    private fun requestEnableBtPermission(activity: Activity) {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        Log.v(LOG_TAG, "Requesting to enable the BT")
        activity.startActivityForResult(enableIntent, Droid2InoConstants.REQUEST_ENABLE_BT)
    }

    /**
     * Prepare a new future bluetooth connection depending on the current [btConnectionType].
     */
    @SuppressLint("NewApi") // Already checked in [selectBleConnectionType()]
    private fun prepareBtEnvironment(messagesHandler: Handler) {
        Log.d(LOG_TAG, "Preparing the BT environment")
        currentBtController = when (btConnectionType) {
            BtConnectionType.BT_SOCKET -> BtSocketController(btAdapter)
            BtConnectionType.BLE ->
                customBleProfileRequested?.let {
                    BleController(btAdapter, it)
                } ?: BleController(btAdapter)
        }

        currentBtController?.let {
            it.prepareBtEnvironment(context, messagesHandler)

            if (btConnectionType == BtConnectionType.BT_SOCKET && it is BtSocketController) {
                it.setDuplexConnection(isBtSocketDuplexRequested)
            }
        }
    }

    /**
     * Stop the current bluetooth connection but without disabling the [BluetoothAdapter].
     */
    fun stopBluetoothConnection() {
        // Stop the Bluetooth connect services
        currentBtController?.stopBtConnection(context)
        currentBtController = null
    }

    /**
     * Connect to another bluetooth device.
     */
    fun connectDevice(btDeviceAddress: String) {
        currentBtController?.let {
            stopBluetoothConnection()
        }

        prepareBtEnvironment(communicationHandler)

        // Get the device MAC address
        try {
            currentBtController?.connectToBtDevice(context, btDeviceAddress)
        } catch (e: Exception) {
            btCommunicationListener?.onError("Error trying to connect to the device with " +
                                             "address $btDeviceAddress", ConnectionState.ERROR_CONNECTING, e)
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    fun sendMessage(message: String) = currentBtController?.sendMessage(message)

    /**
     * Sends a message.
     *
     * @param messageBuffer A string of text to send.
     */
    fun sendMessage(messageBuffer: ByteArray) = currentBtController?.sendMessage(messageBuffer)

    private fun selectBtConnectionType(connectionType: BtConnectionType) {
        if (connectionType == btConnectionType) return

        btConnectionType = connectionType
        btCommunicationListener?.onPreConnectionChangesTo(btConnectionType)
        Log.d(LOG_TAG, "Selected $btConnectionType connection type")

        currentBtController?.let {
            currentBtController?.stopBtConnection(context)
//            prepareBtEnvironment(communicationHandler)
        }
    }

    /**
     * Select BLE as the desired bluetooth connection type. Use the [bleProfile] passed as argument
     * for using a custom [BleProfile]. The [com.bq.robotic.droid2ino.communication.ble.BqZumCoreProfile]
     * is the default [BleProfile] used.
     * Api version JELLY_BEAN_MR2 is required for using BLE.
     */
    @JvmOverloads
    fun selectBleConnectionType(bleProfile: BleProfile? = customBleProfileRequested) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.e(LOG_TAG, "BLE can not be used with the android version of the current device. " +
                           "BLE can be used only with Build.VERSION_CODES.JELLY_BEAN_MR2 or greater")

            // If the current connection isn't BLE, just maintain the current connection type
            if (btConnectionType == BtConnectionType.BLE) {
                selectBtConnectionType(BtConnectionType.DEFAULT)
            }
        } else {
            customBleProfileRequested = bleProfile
            selectBtConnectionType(BtConnectionType.BLE)
        }
    }

    /**
     * Select BT socket as the desired bluetooth connection type. Set the connection to simplex if
     *  the [isDuplex] param is false, or to duplex if it is true.
     */
    @JvmOverloads
    fun selectBtSocketConnectionType(isDuplex: Boolean = isBtSocketDuplexRequested) {
        isBtSocketDuplexRequested = isDuplex
        selectBtConnectionType(BtConnectionType.BT_SOCKET)
    }

    /**
     * Configure a future BT socket connection for being duplex depending on the
     * [isBtSocketTypeDuplex] param.
     */
    fun configureBtSocketConnectionType(isBtSocketTypeDuplex: Boolean = BtConnectionType.BT_SOCKET == BtConnectionType.DEFAULT) {
        if (isBtSocketTypeDuplex != isBtSocketDuplexRequested) {
            isBtSocketDuplexRequested = isBtSocketTypeDuplex
            currentBtController?.let {
                (it as? BtSocketController)?.setDuplexConnection(isBtSocketDuplexRequested)
            }
        }
    }

    /**
     * Configure a future BLE connection type for using a different [BleProfile] or the default one
     * if [bleProfile] param is set to null.
     * The [com.bq.robotic.droid2ino.communication.ble.BqZumCoreProfile] is the default [BleProfile] used.
     * Api version JELLY_BEAN_MR2 is required for using BLE.
     */
    fun configureBleConnectionType(bleProfile: BleProfile? = customBleProfileRequested) {
        if (bleProfile != customBleProfileRequested) {
            customBleProfileRequested = bleProfile

            currentBtController?.let {
                if (it is BleController) stopBluetoothConnection()
            }
        }
    }

    /**
     * Checks if the mobile device is connected to another device
     */
    fun isConnected() = currentBtController?.isConnected() == true

    /**
     * This method should be called when the user allows the app to enable or disable the
     * bluetooth adapter when it needs it.
     */
    fun onEnableBluetoothIsAllowed(allowed: Boolean) {
        isEnableBluetoothAllowed = allowed
    }

    /**
     * This method should be called from the [onCreate] of the calling activity.
     * Stores if the [BluetoothAdapter] was disabled when opening this app in order to disabling it
     * when the user goes out of the app.
     */
    fun onCreate(activity: Activity) {
        try {
            if (isBtAdapterEnabled()) wasBluetoothEnabled = true
        } catch (e: NullPointerException) {
            // If the adapter is null, then Bluetooth is not supported
            Toast.makeText(activity, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show()
            activity.finish()
        }
    }

    /**
     * This method should be called from the [onResume] of the calling activity. If the app has
     * been allowed to manage the [BluetoothAdapter] enabling, it will be enabled if it is't already.
     */
    fun onResume() {
        // The user doesn't have accepted that the app can enable the BT, don't do anything,
        // prepareBtEnvironment() will then be called during onActivityResult
        // If the user has accepted, if the bluetooth is disabled, enable it and prepare the BT controller
        if (isEnableBluetoothAllowed) {
            if (!btAdapter.isEnabled) {
                // Don't call to enableBluetooth() here, as it would request the user to give permission now
                // Wait until the user require it. Also in the case of a SecurityException, just wait
                // until the user requires to enable it to ask for ot again
                try {
                    btAdapter.enable()
                } catch (se: SecurityException) {
                    Log.d(LOG_TAG, "Permission to enable the BT is not granted anymore")
                    isEnableBluetoothAllowed = false
                }
            }
        }
    }

    /**
     * This method should be called from the [onPause] of the calling activity. The current bluetooth
     * connection will be stopped if any and the [BluetoothAdapter] will be disabled if required.
     */
    fun onPause() {
        fullStop()
    }

    /**
     * Stop the current bluetooth connection if any and also, disable the [BluetoothAdapter] too if
     * it wasn't enabled when the app started.
     */
    fun fullStop() {
        unregisterBtAdapterChangesReceiver()
        stopBluetoothConnection()

        // Disable the Bluetooth if it was disable before executing this app
        if (btAdapter.isEnabled && !wasBluetoothEnabled) {
            btAdapter.disable()
        }
    }

    /**
     * This method should be called from the [onDestroy] of the calling activity.
     */
    fun onDestroy() {
        // Do nothing for now
    }

    private fun createBtAdapterChangesReceiver() = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    unregisterBtAdapterChangesReceiver()
                    btCommunicationListener?.onBluetoothAdapterWasEnabled()
                }

//                    BluetoothAdapter.STATE_TURNING_OFF,
//                    BluetoothAdapter.STATE_OFF ->

                else -> {
                    Log.v(LOG_TAG, "Current bluetooth state is ${bluetoothStateToString(state)}")
                    // Wait for on/off state
                }
            }
        }
    }

    private fun registerBtAdapterChangesReceiver() {
        if (btAdapterChangesReceiver != null) return

        btAdapterChangesReceiver = createBtAdapterChangesReceiver()
        context.registerReceiver(btAdapterChangesReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun unregisterBtAdapterChangesReceiver() {
        if (btAdapterChangesReceiver == null) return

        try {
            context.unregisterReceiver(btAdapterChangesReceiver)
        } catch (e: Exception) {
            Log.v(LOG_TAG, "Trying to unregister an already unregistered bluetooth receiver: $e")
        }
        btAdapterChangesReceiver = null
    }

    private fun bluetoothStateToString(state: Int): String = when (state) {
        BluetoothAdapter.STATE_OFF -> "BLUETOOTH_STATE_OFF"
        BluetoothAdapter.STATE_TURNING_OFF -> "BLUETOOTH_STATE_TURNING_OFF"
        BluetoothAdapter.STATE_TURNING_ON -> "BLUETOOTH_STATE_TURNING_ON"
        BluetoothAdapter.STATE_ON -> "BLUETOOTH_STATE_ON"
        else -> "BLUETOOTH_STATE_UNKNOWN"
    }

    // The Handler that gets information back from the BluetoothConnectService
    private val communicationHandler by lazy {
        val thread = HandlerThread("BT_bg_thread")
        thread.start()

        object : Handler(thread.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Droid2InoConstants.MESSAGE_STATE_CHANGE -> {
                        if (msg.obj is ConnectionState) {
                            val connectionState = msg.obj as ConnectionState
                            btCommunicationListener?.onConnectionStatusUpdated(connectionState)
                        }
                    }

                    Droid2InoConstants.MESSAGE_SENT -> {
                        var messageSent: String? = when {
                            msg.obj is ByteArray -> {
                                val writeBuf = msg.obj as ByteArray
                                // construct a string from the buffer
                                String(writeBuf)
                            }
                            msg.obj is String -> msg.obj as String
                            msg.obj != null -> msg.obj.toString()
                            else -> null
                        }

                        messageSent?.let {
                            btCommunicationListener?.onMessageSent(messageSent)
                        }
                    }

                    Droid2InoConstants.MESSAGE_RECEIVED -> {
                        // construct a string from the valid bytes in the buffer
                        if (msg.obj is String)
                            btCommunicationListener?.onMessageReceived(msg.obj as String)

                    }

                    Droid2InoConstants.VALUE_RECEIVED -> {
                        if (msg.obj is ByteArray)
                            btCommunicationListener?.onValueReceived(msg.obj as ByteArray)

                    }

                    Droid2InoConstants.MESSAGE_DEVICE_NAME -> {
                        // save the connected device's name
                        if (msg.obj is String)
                            btCommunicationListener?.onDeviceNameObtained(msg.obj as String)
                    }

                    Droid2InoConstants.MESSAGE_ERROR -> {
                        // TODO: Call to stopBluetoothConnection()?
                        when (msg.obj) {
                            is ConnectionErrorFeedback -> {
                                with(msg.obj as ConnectionErrorFeedback) {
                                    btCommunicationListener?.onError(errorMessage, connectionState, exception)

                                    connectionState?.let {
                                        btCommunicationListener?.onConnectionStatusUpdated(it)
                                    }
                                }
                            }
                            is Exception -> btCommunicationListener?.onError(e = msg.obj as Exception)
                            is String -> btCommunicationListener?.onError(errorMessage = msg.obj as String)
                            is ConnectionState -> {
                                with(msg.obj as ConnectionState) {
                                    btCommunicationListener?.onError(errorState = this)
                                    btCommunicationListener?.onConnectionStatusUpdated(this)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}