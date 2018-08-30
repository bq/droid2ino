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
 * Implementation of a [BleProfile] for the Bq Zum Core board. The characteristic used for reading
 * in the Zum Core board can only be accessed by enabling the notifications, but it can be directly
 * read.
 */
object BqZumCoreProfile {
    private const val ZUM_CORE_CUSTOM_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    private const val ZUM_CORE_CUSTOM_READ_CHARACTERISTIC = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
    private const val ZUM_CORE_CUSTOM_WRITE_CHARACTERISTIC = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"

    val PROFILE = BleProfile(UUID.fromString(ZUM_CORE_CUSTOM_SERVICE),
        UUID.fromString(ZUM_CORE_CUSTOM_READ_CHARACTERISTIC),
        UUID.fromString(ZUM_CORE_CUSTOM_WRITE_CHARACTERISTIC))
}