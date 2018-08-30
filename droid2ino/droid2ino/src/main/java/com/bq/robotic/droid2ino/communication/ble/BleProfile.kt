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

import java.util.*

/**
 * UUIDs used for the service, characteristics and descriptors of the communication through
 * a [BluetoothGattServer] using BLE.
 */
data class BleProfile(val customService: UUID?,
                      val customReadCharacteristic: UUID?,
                      val customWriteCharacteristic: UUID?,
                      val preferredMtu: Int = 512) {
    // These 3 UUIDs are generic to all BLE devices, check
    // [https://www.bluetooth.com/specifications/gatt/characteristics] for the full list.
    // For generic access the correspondent short UUID would be 1800, but in Android we need the full UUID:
    val genericAccessService = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    val deviceNameCharacteristic = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    /**
     * Generic UUID to config the descriptor of a characteristic. Used for enabling the notifications
     * on a characteristic, which will call to the `onCharacteristicChanged()` callback when the
     * characteristic changes its value.
     */
    val characteristicConfigDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /**
     * Obtain a human-readable name for the service given by parameter.
     */
    fun getServiceNameFromUuid(uuid: UUID) = when (uuid) {
        genericAccessService -> "GENERIC_ACCESS_SERVICE"
        customService -> "CUSTOM_SERVICE"
        else -> "UNKNOWN_SERVICE_UUID"
    }

    /**
     * Obtain a human-readable name for the characteristic given by parameter.
     */
    fun getCharacteristicNameFromUuid(uuid: UUID) = when (uuid) {
        deviceNameCharacteristic -> "DEVICE_NAME_CHARACTERISTIC"
        customReadCharacteristic -> "CUSTOM_READ_CHARACTERISTIC"
        customWriteCharacteristic -> "CUSTOM_WRITE_CHARACTERISTIC"
        else -> "UNKNOWN_CHARACTERISTIC_UUID"
    }

    /**
     * Obtain a human-readable name for the descriptor given by parameter.
     */
    fun getDescriptorNameFromUuid(uuid: UUID) = when (uuid) {
        characteristicConfigDescriptor -> "READ_CONFIG_DESCRIPTOR"
        else -> "UNKNOWN_DESCRIPTOR_UUID"
    }
}