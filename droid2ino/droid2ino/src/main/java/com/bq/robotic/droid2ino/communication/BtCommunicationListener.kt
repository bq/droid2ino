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

import com.bq.robotic.droid2ino.utils.Droid2InoConstants

interface BtCommunicationListener {
    /**
     * Callback that will be invoked after a successful request for enabling the bluetooth adapter
     * when the app already had permission for enabling the bluetooth.
     * See [BluetoothManager.enableBluetooth] and [BluetoothManager.onEnableBluetoothIsAllowed].
     */
    fun onBluetoothAdapterWasEnabled()

    /**
     * Callback that will be invoked when Bluetooth connectivity state changes
     *
     * @param connectionState Message types sent from the BluetoothConnectService Handler
     */
    fun onConnectionStatusUpdated(connectionState: Droid2InoConstants.ConnectionState)

    /**
     * Callback that will be invoked after message was sent successfully.
     *
     * @param message data that was sent to remote device
     */
    fun onMessageSent(message: String)

    /**
     * Callback that will be invoked when new message is received.
     *
     * @param message new message string
     */
    fun onMessageReceived(message: String)

    /**
     * Callback that will be invoked when we obtain the name of the device to which we are connected to.
     */
    fun onDeviceNameObtained(deviceName: String)

    /**
     * Callback called when the connection type is going to be changed, so if another extra configuration
     * over that connection has to be set, such as setting as simplex or a custom BLE profile.
     */
    fun onPreConnectionChangesTo(connectionTypeChangedTo: BluetoothManager.BtConnectionType)

    /**
     * Callback that will be invoked when an error appears.
     */
    fun onError(errorMessage: String? = null, errorState: Droid2InoConstants.ConnectionState? = null,
                e: Exception? = null)
}

/**
 * This adapter class provides empty implementations of the methods from {@link BtCommunicationListener}.
 * Any custom listener that cares only about a subset of the methods of this listener can
 * simply subclass this adapter class instead of implementing the interface directly.
 */
abstract class BtCommunicationListenerAdapter : BtCommunicationListener {
    /**
     * {@inheritDoc}
     */
    override fun onBluetoothAdapterWasEnabled() {
    }

    /**
     * {@inheritDoc}
     */
    override fun onConnectionStatusUpdated(connectionState: Droid2InoConstants.ConnectionState) {
    }

    /**
     * {@inheritDoc}
     */
    override fun onMessageSent(message: String) {
    }

    /**
     * {@inheritDoc}
     */
    override fun onMessageReceived(message: String) {
    }

    /**
     * {@inheritDoc}
     */
    override fun onDeviceNameObtained(deviceName: String) {
    }

    /**
     * {@inheritDoc}
     */
    override fun onPreConnectionChangesTo(connectionTypeChangedTo: BluetoothManager.BtConnectionType) {
    }

    /**
     * {@inheritDoc}
     */
    override fun onError(errorMessage: String?, errorState: Droid2InoConstants.ConnectionState?, e: Exception?) {
    }
}
