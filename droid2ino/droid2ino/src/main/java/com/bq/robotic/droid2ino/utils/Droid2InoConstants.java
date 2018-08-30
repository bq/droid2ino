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

import com.bq.robotic.droid2ino.BuildConfig;

import java.util.UUID;

/**
 * Utilities class with all the constants of the library
 */

public class Droid2InoConstants {

   /**
    * Message types sent from the BluetoothConnection Handler
    */
   public static final int MESSAGE_STATE_CHANGE = 1;
   public static final int MESSAGE_RECEIVED = 2;
   public static final int MESSAGE_SENT = 3;
   public static final int MESSAGE_DEVICE_NAME = 4;
   public static final int MESSAGE_ERROR = 5;

   /**
    * Key names received from the BluetoothConnection Handler
    */
   public static final String DEVICE_NAME = "device_name";

   /**
    * Intent request codes
    */
   public static final int REQUEST_ENABLE_BT = 3;
   public static final String EXTRA_DEVICE_ADDRESS = "device_address";

   /**
    * Name for the SDP record when creating server socket
    */
   public static final String SOCKET_NAME = "BluetoothSocket";

   /**
    * Possible connection states.
    */
   public enum ConnectionState {
      DISCONNECTED,               // we're doing nothing
      LISTENING,                  // listening for incoming connections
      CONNECTING,                 // initiating an outgoing connection
      CONNECTED_NOT_CONFIGURED,   // connected to a remote device but not configured yet
      CONNECTED_CONFIGURED,       // connected to a remote device and configured
      ERROR_CONNECTING,           // error while trying to connect
      ERROR_CONFIGURING;          // error while trying to configure after being connected to the device

      public boolean isError() {
         return this == ERROR_CONNECTING || this == ERROR_CONFIGURING;
      }
   }

   // Save bundle keys
   public static final String WAS_BLUETOOTH_ALLOWED_KEY = "wasBluetoothAllowedKey";

   // Read delimiters
   public static final String START_READ_DELIMITER = "&&";
   public static final String END_READ_DELIMITER = "%%";

   // Debug
   public static final boolean DEBUG = BuildConfig.DEBUG;

   // Others
   public static final String NEW_LINE_CHARACTER = "\n";
}
