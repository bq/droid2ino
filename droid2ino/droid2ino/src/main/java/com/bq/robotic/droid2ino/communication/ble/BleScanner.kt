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
import android.os.CountDownTimer
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.bq.robotic.droid2ino.communication.BtScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.support.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BleScanner: BtScanner {
    private val LOG_TAG = this.javaClass.simpleName

    private val btAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val bleScanner by lazy { btAdapter.bluetoothLeScanner }

    private var scanListener: BtScanner.BtScanListener? = null
    private var isScanning = false

    private val BLE_SCANNING_TIMEOUT_MS = 8000L
    private val bleScanningTimer = object : CountDownTimer(BLE_SCANNING_TIMEOUT_MS, BLE_SCANNING_TIMEOUT_MS) {
        override fun onTick(millisUntilFinished: Long) {
            // Do nothing
        }

        override fun onFinish() {
            onTimeoutBleScanning()
        }
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(LOG_TAG, "BT discovery found device: " + result?.scanRecord?.deviceName)

            val address = result?.device?.address
            address?.let {
                val device = btAdapter.getRemoteDevice(address)
                device?.let {
                    scanListener?.onDeviceFound(it)
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // We scan with report delay == 0. This will never be called.
            super.onBatchScanResults(results)
            Log.d(LOG_TAG, "onBatchScanResults: $results")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(LOG_TAG, "BLE scan failed")
            // TODO: Should be clean the list?
            stopBleScan()
        }
    }

    override fun setBtScanListener(scanListener: BtScanner.BtScanListener) {
        this.scanListener = scanListener
    }

    override fun scanForBtDevices(): Boolean {
        if (isScanning) {
            Log.d(LOG_TAG, "Already scanning")
            return false
        }

        Log.d(LOG_TAG, "Starting Bluetooth LE scan...")
        isScanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        val filters = emptyList<ScanFilter>()
        bleScanner.startScan(filters, settings, bleScanCallback)

        startBleScanningTimeout()
        return true
    }

    override fun stopScan() {
        // Stop the ble scan
        stopBleScan()
    }

    override fun getPairedBtDevices(): List<BluetoothDevice>? {
        return null
    }

    private fun stopBleScan() {
        if (!btAdapter.isEnabled) return

        if (!isScanning) {
            Log.d(LOG_TAG, "Scanning already stopped")
            return
        }

        isScanning = false
        Log.d(LOG_TAG, "Stopping Bluetooth LE scan...")

        stopBleScanningTimeout()
        bleScanner.stopScan(bleScanCallback)
        scanListener?.onScanFinished()
    }

    private fun onTimeoutBleScanning() {
        stopBleScan()
    }

    private fun startBleScanningTimeout() {
        bleScanningTimer.cancel()
        bleScanningTimer.start()
    }

    private fun stopBleScanningTimeout() {
        bleScanningTimer.cancel()
    }

}