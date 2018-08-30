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

import android.bluetooth.BluetoothDevice

interface BtScanner {
    /**
     * Starts the scan for finding new bluetooth devices. Depending on the implementation requested,
     * this scan will be done via BluetoothSocket or BLE (bluetooth low energy).
     */
    fun scanForBtDevices(): Boolean

    /**
     * Retrieve the list of the current paired BT devices with this device.
     */
    fun getPairedBtDevices(): List<BluetoothDevice>?

    /**
     * Stops the current scanning of BT devices.
     */
    fun stopScan()

    /**
     * Set a listener which will be invoked with results of the scan.
     */
    fun setBtScanListener(scanListener: BtScanListener)

    interface BtScanListener {
        /**
         * Called each time a new device has been found by the scanner.
         */
        fun onDeviceFound(btDevice: BluetoothDevice)

        /**
         * Called when the scan has finished.
         */
        fun onScanFinished()
    }
}

