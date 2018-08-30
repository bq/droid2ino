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

package com.bq.robotic.droid2ino.views

import android.widget.ImageButton
import android.widget.TextView
import android.view.View

/**
 * Utils class for style the components of the scanned bluetooth devices dialog [BtDevicesListDialog].
 * @param bleSelectorView will be null if the BLE scanner is not supported as the device api level
 * is less that LOLLIPOP (21).
 *
 * @param titleView                 TextView of the title of the dialog for searching the bluetooth devices
 * @param btSocketSelectorView      TextView used for select the BT socket connection type
 * @param bleSelectorView           TextView used for select the BLE socket connection type.
 *                                  Null if BLE scanner is not supported
 * @param scanBtDevicesButton       Button used for scanning BT devices again
 * @param titleSeparatorView        Simple line separator between the title of the dialog and the content.
 */
data class DevicesListDialogStyle(val titleView: TextView,
                                  val btSocketSelectorView: TextView,
                                  val bleSelectorView: TextView? = null,
                                  val scanBtDevicesButton: ImageButton,
                                  val titleSeparatorView: View)