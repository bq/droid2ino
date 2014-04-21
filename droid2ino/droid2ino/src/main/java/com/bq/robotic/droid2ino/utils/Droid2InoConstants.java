/*
* This file is part of the Androidino
*
* Copyright (C) 2013 Mundo Reader S.L.
* 
* Date: February 2014
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

package com.bq.robotic.droid2ino.utils;

import java.util.UUID;

/**
 * Utilities class with all the constants of the library
 */

public class Droid2InoConstants {

    // Message types sent from the BluetoothConnection Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothConnection Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 3;
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    // Name for the SDP record when creating server socket
    public static final String SOCKET_NAME = "BluetoothSocket";
    
    // Name of the threads
    public static final String ACCEPT_THREAD_NAME = "AcceptThread";
    public static final String CONNECT_THREAD_NAME = "ConnectThread";
    public static final String CONNECTED_THREAD_NAME = "ConnectedThread";

    // Unique UUID for this application
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // Save bundle keys
    public static final String WAS_BLUETOOTH_ALLOWED_KEY = "wasBluetoothAllowedKey";
    
    // Read delimiters
    public static final String START_READ_DELIMITER = "&&";
    public static final String END_READ_DELIMITER = "%%";
    
    // Debug
    public static final boolean D = true;
	
    // Others
    public static final String NEW_LINE_CHARACTER = "\n";
}
