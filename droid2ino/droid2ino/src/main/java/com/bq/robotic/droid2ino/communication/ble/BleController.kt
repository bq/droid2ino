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

package com.bq.robotic.droid2ino.communication.ble

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresApi
import android.util.Log
import com.bq.robotic.droid2ino.R
import com.bq.robotic.droid2ino.communication.BtControllerInterface
import com.bq.robotic.droid2ino.utils.ConnectionErrorFeedback
import com.bq.robotic.droid2ino.utils.Droid2InoConstants
import com.bq.robotic.droid2ino.utils.Droid2InoConstants.ConnectionState
import org.jetbrains.annotations.NotNull
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class BleController(private val btAdapter: BluetoothAdapter): BtControllerInterface {
    private val LOG_TAG = this.javaClass.simpleName

    var bleProfile: BleProfile = BqZumCoreProfile.PROFILE
    private val gattClient by lazy { GattClient(bleProfile) }

    override fun prepareBtEnvironment(context: Context, communicationHandler: Handler) {
        gattClient.eventListener = (object : GattClient.OnGattEventListener {
            override fun onStateChanged(@NotNull state: GattClient.State) {
                if (state == GattClient.State.ERROR_CONNECTING) {
                    communicationHandler.obtainMessage(Droid2InoConstants.MESSAGE_ERROR,
                        ConnectionErrorFeedback(context.getString(R.string.connecting_bluetooth_error),
                            ConnectionState.ERROR_CONNECTING)).sendToTarget()
                }

                communicationHandler.obtainMessage(Droid2InoConstants.MESSAGE_STATE_CHANGE,
                    parseGattStateToAppState(state)).sendToTarget()
            }

            override fun onLastRequestStatusChanged(@NotNull requestStatus: GattClient.RequestStatus) {
                if (requestStatus.isError()) {
                    Log.e(LOG_TAG, "Error in the last request = $requestStatus")

                    communicationHandler.obtainMessage(Droid2InoConstants.MESSAGE_ERROR,
                        ConnectionErrorFeedback(connectionState = ConnectionState.ERROR_CONFIGURING,
                            exception = IOException("BLE request failed: $requestStatus"))).sendToTarget()
                }
            }

            override fun onMessageSent(@NotNull messageReceived: String) {
                communicationHandler.obtainMessage(Droid2InoConstants.MESSAGE_SENT, messageReceived).sendToTarget()
            }

            override fun onMessageReceived(@NotNull messageSent: String) {
                communicationHandler.obtainMessage(Droid2InoConstants.MESSAGE_RECEIVED, messageSent)
                    .sendToTarget()
            }

            override fun onDeviceNameObtained(@NotNull deviceName: String) {
                communicationHandler.obtainMessage(Droid2InoConstants.MESSAGE_DEVICE_NAME, deviceName)
                    .sendToTarget()
            }
        })
    }

    override fun connectToBtDevice(context: Context, address: String) {
        // Get the BluetoothDevice object
        val device = btAdapter.getRemoteDevice(address)

        if (gattClient.state.isConnecting() || gattClient.state.isConfiguring()) {
            Log.d(LOG_TAG, "Skipping new connection attempt as there is already one in progress")
        } else {
            Log.d(LOG_TAG, "New connection attempt")

            device?.let {
                // Just in case it wasn't correctly closed in the retry
                gattClient.closeClient()
                gattClient.startClient(context, it)
            }
        }
    }

    override fun stopBtConnection(context: Context) {
        // Stop the Bluetooth connect services
        gattClient.closeClient()
    }

    override fun sendMessage(message: String) {
        // Check that we're actually connected before trying anything
        if (!isConnected()) {
            return
        } else if (gattClient.state.isConfiguring()) {
            Log.d(LOG_TAG, "BLE is still being configured, current state is " + gattClient.state)
            return
        } else if (gattClient.state != GattClient.State.CONFIGURED) {
            Log.w(LOG_TAG, "BLE is not configured yet, current state is " + gattClient.state)
            return
        }

        if (message.isNotBlank()) {
            gattClient.sendMsgToConnectedDevice(message)
        }
    }

    override fun sendMessage(messageBuffer: ByteArray) {
        sendMessage(String(messageBuffer))
    }

    override fun isConnected() = gattClient.state.isConnected()

    private fun parseGattStateToAppState(connectionState: GattClient.State) = when(connectionState) {
        GattClient.State.DISCONNECTED -> ConnectionState.DISCONNECTED
        GattClient.State.CONNECTING -> ConnectionState.CONNECTING
        GattClient.State.CONNECTED_NOT_CONFIGURED -> ConnectionState.CONNECTED_NOT_CONFIGURED
        GattClient.State.REQUESTING_MTU -> ConnectionState.CONNECTED_NOT_CONFIGURED
        GattClient.State.DISCOVERING_SERVICES -> ConnectionState.CONNECTED_NOT_CONFIGURED
        GattClient.State.ENABLING_NOTIFICATIONS -> ConnectionState.CONNECTED_NOT_CONFIGURED
        GattClient.State.ERROR_CONNECTING -> ConnectionState.ERROR_CONNECTING
        GattClient.State.ERROR_DISCOVERING_SERVICES -> ConnectionState.ERROR_CONFIGURING
        GattClient.State.CONFIGURED -> ConnectionState.CONNECTED_CONFIGURED
    }

}