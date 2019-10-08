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

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.*
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import com.bq.robotic.droid2ino.utils.GsonValidator
import java.util.*

// Delay to correctly enqueue the operations in gatt (if not they will be dropped and nothing will be done)
private const val GATT_MTU_DELAY_MS = 1000L
private const val GATT_ENQUEUE_DELAY_MS = 150L
private const val DEFAULT_MSG_DATA_SIZE = 20 // 20 bytes
private const val MSG_DATA_MARGIN_SIZE = 3 // 3 bytes
private const val SHOULD_REQUEST_MTU_CHANGE = false

/**
 * This class does all the work for setting up and managing Bluetooth connections with other devices
 * using a GATT protocol over BLE (bluetooth low energy).
 *
 * Documentation of some well known problems:
 * The PDF in: https://devzone.nordicsemi.com/b/blog/posts/what-to-keep-in-mind-when-developing-your-ble-andr
 * https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues
 * https://android.jlelse.eu/lessons-for-first-time-android-bluetooth-le-developers-i-learned-the-hard-way-fee07646624
 */
// TODO: Add more checking error mechanisms
// TODO: Refactor the naming of variables/methods, some methods could be more similar between them
// TODO: Encrypt the communication? Other security levels: https://stackoverflow.com/questions/38963836/bluetooth-low-energy-gatt-security-levels
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class GattClient(private val bleProfile: BleProfile) {
    private val LOG_TAG = this.javaClass.simpleName

    companion object {
        /**
         * Obtain a human-readable string from a GATT code error.
         */
        fun gattErrorToString(errorCode: Int): String = when (errorCode) {
            GATT_READ_NOT_PERMITTED -> "GATT_READ_NOT_PERMITTED"
            GATT_WRITE_NOT_PERMITTED -> "GATT_WRITE_NOT_PERMITTED"
            GATT_INSUFFICIENT_AUTHENTICATION -> "GATT_INSUFFICIENT_AUTHENTICATION"
            GATT_REQUEST_NOT_SUPPORTED -> "GATT_REQUEST_NOT_SUPPORTED"
            GATT_INSUFFICIENT_ENCRYPTION -> "GATT_INSUFFICIENT_ENCRYPTION"
            // A read or write operation was requested with an invalid offset
            GATT_INVALID_OFFSET -> "GATT_INVALID_OFFSET"
            // A read or write operation was requested with an invalid offset
            GATT_INVALID_ATTRIBUTE_LENGTH -> "GATT_INVALID_ATTRIBUTE_LENGTH"
            GATT_CONNECTION_CONGESTED -> "GATT_CONNECTION_CONGESTED"
            // A GATT operation failed, errors other than the above
            GATT_FAILURE -> "GATT_FAILURE"
            else -> "GATT_UNKNOWN_ERROR"
        }
    }

    /**
     * Possible states for the Gatt client.
     */
    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED_NOT_CONFIGURED,
        REQUESTING_MTU,
        DISCOVERING_SERVICES,
        ERROR_CONNECTING,
        ERROR_DISCOVERING_SERVICES,
        ENABLING_NOTIFICATIONS,
        CONFIGURED;

        /**
         * Checks if a connecting request was sent to the device.
         */
        fun isConnecting() = this == CONNECTING

        fun isConfiguring() = this == CONNECTED_NOT_CONFIGURED || this == REQUESTING_MTU
                              || this == DISCOVERING_SERVICES || this == ENABLING_NOTIFICATIONS

        fun isConnected() = isConfiguring() || this == CONFIGURED

        fun isError() = ERROR_CONNECTING == this || ERROR_DISCOVERING_SERVICES == this
    }

    /**
     * Request status for the request to the gatt client.
     */
    enum class RequestStatus {
        IDLE,
        REQUESTING_CUSTOM_CHARACTERISTIC_VALUE, // requesting read on the device name characteristic
        REQUESTING_DEVICE_NAME_CHARACTERISTIC_VALUE, // requesting read on the custom characteristic
        DEVICE_NAME_RECEIVED,
        ERROR_REQUESTING_CUSTOM_READ_CHARACTERISTIC,
        ERROR_READING_CUSTOM_CHARACTERISTIC,
        ERROR_REQUESTING_DEVICE_NAME_CHARACTERISTIC,
        ERROR_READING_DEVICE_NAME_CHARACTERISTIC,
        SENDING_MESSAGE_TO_DEVICE,
        MESSAGE_RECEIVED_FROM_DEVICE,
        ERROR_REQUESTING_CUSTOM_WRITE_CHARACTERISTIC,
        ERROR_WRITING_CUSTOM_CHARACTERISTIC,
        MESSAGE_SENT_TO_DEVICE;

        fun isError() = ERROR_REQUESTING_CUSTOM_READ_CHARACTERISTIC == this || ERROR_WRITING_CUSTOM_CHARACTERISTIC == this
                        || ERROR_REQUESTING_DEVICE_NAME_CHARACTERISTIC == this
    }

    /**
     * Current state of this gatt client.
     */
    var state = State.DISCONNECTED
        private set (newState) {
            if (newState == field) return
            field = newState
            eventListener?.onStateChanged(newState)
        }

    /**
     * Status of the last request to this gatt client.
     */
    var lastRequestStatus = RequestStatus.IDLE
        private set (requestStatus) {
            if (requestStatus == field) return
            field = requestStatus
            eventListener?.onLastRequestStatusChanged(requestStatus)
        }

    // Reading/writing messages helpers
    private val messageReceivedBuilder: StringBuilder = StringBuilder()
    private var messageToSend = mutableListOf<String>()
    private val deviceNameReceivedBuilder: StringBuilder = StringBuilder()
    private var msgDataSize = DEFAULT_MSG_DATA_SIZE

    private var bluetoothGatt: BluetoothGatt? = null

    /**
     * Listener used to communicate changes or the result of requests to this gatt client.
     */
    var eventListener: OnGattEventListener? = null

    // TODO: Poll reading the device data characteristic if the notification wasn't able to be enabled?
    private var areNotificationsEnabled = false

    private val gattCallback: BluetoothGattCallback by lazy {
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(LOG_TAG, "Connected to the GATT server")
                        state = State.CONNECTED_NOT_CONFIGURED

                        // Try to request the increase of MTU, if it fails, discover directly the services
                        if (SHOULD_REQUEST_MTU_CHANGE/* && gatt.requestMtu(PREFERRED_MTU)*/) {
                            state = State.REQUESTING_MTU

                            // TODO: Check if don't trust in the [onMtuChanged] callback being called
                            // if the MTU doesn't changes, so we should start discovering services after
                            // the needed gatt delay

                            // Add delay to correctly enqueue the operations in gatt (if not they will
                            // be dropped and nothing will be done)
//                            compositeDisposable.add(Completable.complete()
//                                .delay(GATT_MTU_DELAY_MS, TimeUnit.MILLISECONDS)
//                                .subscribe {
//                                    startServicesDiscovery()
//                                })

                        } else {
                            startServicesDiscovery()
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(LOG_TAG, "Disconnected from the GATT server")

                        // TODO: Manage the 133 error
                        if (state == State.CONNECTING) {
                            state = State.ERROR_CONNECTING
                        }

                        closeClient()
                        state = State.DISCONNECTED
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                Log.d(LOG_TAG, "Mtu changed = $mtu")
                msgDataSize = mtu - MSG_DATA_MARGIN_SIZE
                startServicesDiscovery()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.d(LOG_TAG, "onServicesDiscovered called")

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(LOG_TAG, "Error while discovering services: ${gattErrorToString(status)}")
                    state = State.ERROR_DISCOVERING_SERVICES
                    return
                }

                // Sometimes the service is null
                bleProfile.customService?.let {
                    val service = gatt.getService(it)

                    if (service == null) {
                        Log.e(LOG_TAG, "Error discovering custom service: ${gattErrorToString(status)}")
                        state = State.ERROR_DISCOVERING_SERVICES
                        return
                    } else {
                        Log.d(LOG_TAG, "Custom service discovered")
                    }

                    // Enable notifications in the custom characteristic
                    bleProfile.customReadCharacteristic?.let {
                        state = State.ENABLING_NOTIFICATIONS
                        enableCharacteristicNotifications(gatt, service, bleProfile.customReadCharacteristic,
                            bleProfile.characteristicConfigDescriptor)
                    }
                }

                // TODO: Check if we trust in the [onDescriptorWrite] callback being called or if we
                // try to read anyway here
//                    Timber.i("Requesting a read operation in the wifi list characteristic")
//                    compositeDisposable.add(Completable.complete()
//                        .delay(GATT_ENQUEUE_DELAY_MS * 2, TimeUnit.MILLISECONDS)
//                        .subscribe {
//                            state = State.REQUESTING_WIFI_LIST)
//                            gatt.readCharacteristic(it.getCharacteristic(WIFI_LIST_CHARACTERISTIC))
//                        })

            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
                                              status: Int) {
                Log.d(LOG_TAG, "onCharacteristicRead called")

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    when (characteristic.uuid) {
                        null -> { }  // Do nothing
                        bleProfile.customReadCharacteristic -> lastRequestStatus = RequestStatus.ERROR_READING_CUSTOM_CHARACTERISTIC
                        bleProfile.deviceNameCharacteristic -> lastRequestStatus = RequestStatus.ERROR_READING_DEVICE_NAME_CHARACTERISTIC
                    }

                    Log.e(LOG_TAG, "Error reading the characteristic " +
                                   "${bleProfile.getCharacteristicNameFromUuid(characteristic.uuid)} with error: ${gattErrorToString(status)}")
                    return
                }

                when (characteristic.uuid) {
                    null -> { }  // Do nothing
                    bleProfile.customReadCharacteristic -> {
                        // Clear the builder for the next messages
                        messageReceivedBuilder.setLength(0)
                        readCustomCharacteristic(characteristic)
                    }

                    bleProfile.deviceNameCharacteristic -> {
                        deviceNameReceivedBuilder.setLength(0)
                        readDeviceNameCharacteristic(characteristic)
                    }
                    else -> { } // Do nothing
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                Log.v(LOG_TAG, "Characteristic ${bleProfile.getCharacteristicNameFromUuid(characteristic.uuid)} updated")
                when (characteristic.uuid) {
                    null -> { }  // Do nothing
                    bleProfile.customReadCharacteristic -> readCustomCharacteristic(characteristic)
                    bleProfile.deviceNameCharacteristic -> readDeviceNameCharacteristic(characteristic)
                    else -> { } // Do nothing
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
                                               status: Int) {
                Log.d(LOG_TAG, "onCharacteristicWrite called")

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(LOG_TAG, "Error writing on the characteristic " +
                                   "${bleProfile.getCharacteristicNameFromUuid(characteristic.uuid)} " +
                                   "with error: ${gattErrorToString(status)}")

                    bleProfile.customWriteCharacteristic?.let {
                        if (characteristic.uuid == it)
                            lastRequestStatus = RequestStatus.ERROR_WRITING_CUSTOM_CHARACTERISTIC
                    }
                    return
                }

                when (characteristic.uuid) {
                    null -> { }  // Do nothing
                    bleProfile.customWriteCharacteristic -> {
                        Log.d(LOG_TAG, "Message was sent")
                        writePartialMsgToDevice(messageToSend)
                    }
                }
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                Log.d(LOG_TAG, "onDescriptorWrite called")

                // Result of this client write on a descriptor to the server
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(LOG_TAG, "Error writing on the descriptor " +
                                   "${bleProfile.getDescriptorNameFromUuid(descriptor.uuid)} " +
                                   "with error: ${gattErrorToString(status)}")
                    return
                }

                when (descriptor.uuid) {
                    null -> { }  // Do nothing
                    bleProfile.characteristicConfigDescriptor -> {
                        Log.d(LOG_TAG, "Notifications on wifi list characteristic enabled")
                        state = State.CONFIGURED
                        areNotificationsEnabled = true

                        Log.d(LOG_TAG, "Requesting a read operation in the wifi list characteristic")
                        lastRequestStatus = RequestStatus.REQUESTING_DEVICE_NAME_CHARACTERISTIC_VALUE
                        gatt.readCharacteristic(gatt.getService(bleProfile.genericAccessService)
                            .getCharacteristic(bleProfile.deviceNameCharacteristic))
                    }
                }
            }

        }
    }

    /**
     * Start a Gatt communication with the given [BluetoothDevice].
     */
    fun startClient(context: Context, bluetoothDevice: BluetoothDevice) {
        state = State.CONNECTING

        bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)
        if (bluetoothGatt == null)
            Log.e(LOG_TAG, "Unable to create the GATT client")

    }

    /**
     * Close the started Gatt communication.
     */
    fun closeClient() {
        bluetoothGatt?.let {
            it.close()
            Log.d(LOG_TAG, "GATT client was closed")
        }
        bluetoothGatt = null
        state = State.DISCONNECTED
    }

    private fun startServicesDiscovery() {
        if (state == State.DISCOVERING_SERVICES) {
            Log.d(LOG_TAG, "There is a discovery already in process")
            return
        }

        state = if (bluetoothGatt?.discoverServices() == true) {
            Log.d(LOG_TAG, "Start connected device services discovery")
            State.DISCOVERING_SERVICES
        } else {
            Log.e(LOG_TAG, "Error trying to discover the device's services")
            State.ERROR_DISCOVERING_SERVICES
        }
    }

    /**
     * In order to enable the notifications on a characteristic, we have to write in its
     * configuration descriptor, not only enabling the characteristicNotification.
     * The boolean result that this function returns isn't reliable. The framework returns many times
     * 'false' when trying to write in the notifications descriptor but after that, the callback
     * that the notification were enabled correctly is called -_-
     */
    private fun enableCharacteristicNotifications(gatt: BluetoothGatt, service: BluetoothGattService,
                                                  characteristicUuid: UUID, configDescriptor: UUID): Boolean {
        val characteristic = service.getCharacteristic(characteristicUuid)
        characteristic?.let {
            gatt.setCharacteristicNotification(it, true)

            val descriptor = characteristic.getDescriptor(configDescriptor)
            descriptor?.let {
                Log.d(LOG_TAG, "Preparing to write in the notification descriptor")
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            }
        }

        return false
    }

    private fun readCustomCharacteristic(characteristic: BluetoothGattCharacteristic) {
//        val json = characteristic.getStringValue(0)
//        Log.d(LOG_TAG, "Custom characteristic message obtained: $json")
//
//        if (json.isNotBlank()) {
//            messageReceivedBuilder.append(json)
//
//            if (GsonValidator.isJsonValid(messageReceivedBuilder.toString())) {
//                Log.d(LOG_TAG, "Message received = $messageReceivedBuilder")
//                lastRequestStatus = RequestStatus.MESSAGE_RECEIVED_FROM_DEVICE
//                eventListener?.onMessageReceived(messageReceivedBuilder.toString())
//
//                // Clear the builder for the next messages
//                messageReceivedBuilder.setLength(0)
//            }
//        }

        val value = characteristic.value
        Log.d(LOG_TAG, "Custom characteristic message obtained: $value")

        if (value.isNotEmpty()) {
                lastRequestStatus = RequestStatus.MESSAGE_RECEIVED_FROM_DEVICE
                eventListener?.onValueReceived(value)
        }
    }

    private fun readDeviceNameCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val readOffset = 0
        val json = characteristic.getStringValue(readOffset)
        Log.d(LOG_TAG, "Device data obtained: $json")

        if (json.isBlank()) {
            Log.w(LOG_TAG, "Device data chunk received is null or empty")
            return
        }

        deviceNameReceivedBuilder.append(json)
        if (GsonValidator.isJsonValid(deviceNameReceivedBuilder.toString())) {

            Log.d(LOG_TAG, "Device name received = $deviceNameReceivedBuilder")
            lastRequestStatus = RequestStatus.MESSAGE_RECEIVED_FROM_DEVICE
            eventListener?.onDeviceNameObtained(deviceNameReceivedBuilder.toString())

            lastRequestStatus = RequestStatus.DEVICE_NAME_RECEIVED

            // Clear the builder for the next messages
            deviceNameReceivedBuilder.setLength(0)
        }
    }

    /**
     * Request reading in the custom read characteristic.
     */
    fun requestLastMsgFromConnectedDevice() {
        var customCharacteristic: BluetoothGattCharacteristic? = null
        if (bleProfile.customService != null && bleProfile.customReadCharacteristic != null) {
            customCharacteristic = bluetoothGatt?.getService(bleProfile.customService)
                ?.getCharacteristic(bleProfile.customReadCharacteristic)
        }

        if (customCharacteristic != null) {
            lastRequestStatus = RequestStatus.REQUESTING_CUSTOM_CHARACTERISTIC_VALUE
            bluetoothGatt?.readCharacteristic(customCharacteristic)
        } else {
            lastRequestStatus = RequestStatus.ERROR_REQUESTING_CUSTOM_READ_CHARACTERISTIC
            // TODO: Use a more concrete error for each case, service == null or characteristic == null etc
            Log.e(LOG_TAG, "Error requesting the last message from the connected device")
        }
    }

    /**
     * Send the message to the connected device by writing in a dedicated gatt service characteristic.
     */
    fun sendMsgToConnectedDevice(json: String) {
        lastRequestStatus = RequestStatus.SENDING_MESSAGE_TO_DEVICE

        // Split the message in a list of packets of the mtu size
        messageToSend = json.chunked(msgDataSize).toMutableList()
        writePartialMsgToDevice(messageToSend)
    }

    private fun writePartialMsgToDevice(msgChunkedList: MutableList<String>) {
        if (msgChunkedList.isEmpty()) {
            Log.v(LOG_TAG, "All the message was already sent to the connected device")
            lastRequestStatus = RequestStatus.MESSAGE_SENT_TO_DEVICE
            return
        }

        var customCharacteristic: BluetoothGattCharacteristic? = null
        if (bleProfile.customService != null && bleProfile.customWriteCharacteristic != null) {
            customCharacteristic = bluetoothGatt?.getService(bleProfile.customService)
                ?.getCharacteristic(bleProfile.customWriteCharacteristic)
        }

        if (customCharacteristic != null) {
            // Retrieve and remove the next chunk of data to send
            val nextChunk = msgChunkedList.removeAt(0)

            customCharacteristic.setValue(nextChunk)
            bluetoothGatt!!.writeCharacteristic(customCharacteristic)

        } else {
            lastRequestStatus = RequestStatus.ERROR_REQUESTING_CUSTOM_WRITE_CHARACTERISTIC
            // TODO: Use a more concrete error for each case, service == null or characteristic == null etc
            Log.e(LOG_TAG, "Error sending the message to the connected device")
        }
    }

    interface OnGattEventListener {
        fun onStateChanged(state: State)
        fun onLastRequestStatusChanged(requestStatus: RequestStatus)
        fun onMessageReceived(messageReceived: String)
        fun onValueReceived(value: ByteArray)
        fun onMessageSent(messageSent: String)
        fun onDeviceNameObtained(deviceName: String)
    }

}