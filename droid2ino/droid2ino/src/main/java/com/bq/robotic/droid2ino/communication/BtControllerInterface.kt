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

import android.content.Context
import android.os.Handler

interface BtControllerInterface {
    /**
     * Prepare a new future bluetooth connection. Depending on the implementation requested, this connection
     * will be via BluetoothSocket or BLE (bluetooth low energy).
     * For connecting with a device call to [connectDevice] after calling this method.
     */
    fun prepareBtEnvironment(context: Context, communicationHandler: Handler)

    /**
     * Connect to a bluetooth device. Depending on the implementation requested, this connection
     * will be via BluetoothSocket or BLE (bluetooth low energy).
     * Call to [prepareBtEnvironment] before calling this method.
     */
    fun connectToBtDevice(context: Context, address: String)

    /**
     * Stops the current bluetooth connection.
     */
    fun stopBtConnection(context: Context)

    /**
     * Send a message to a connected device. Depending on the implementation requested, it will be
     * sent via BluetoothSocket or BLE (bluetooth low energy).
     */
    fun sendMessage(message: String)

    /**
     * Send a message to a connected device. Depending on the implementation requested, it will be
     * sent via BluetoothSocket or BLE (bluetooth low energy).
     */
    fun sendMessage(messageBuffer: ByteArray)

    /**
     * Checks if this device is connected to another device.
     */
    fun isConnected(): Boolean
}