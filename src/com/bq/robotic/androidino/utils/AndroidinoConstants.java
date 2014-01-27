package com.bq.robotic.androidino.utils;

import java.util.UUID;

public class AndroidinoConstants {

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
    
    // Debug
    public static final boolean D = true;
	
    // Others
    public static final String NEW_LINE_CHARACTER = "\n";
}
