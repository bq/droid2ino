/*
* This file is part of the Androidino
*
* Copyright (C) 2014 Mundo Reader S.L.
* 
* Date: February 2014
* Author: Estefanía Sarasola Elvira <estefania.sarasola@bq.com>
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3.0 of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/lgpl.html>.
*
*/

package com.bq.robotic.droid2ino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.bq.robotic.droid2ino.utils.Droid2InoConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 * 
 * based on http://developer.android.com/resources/samples/BluetoothChat/index.html
 * 
 */

public class BluetoothConnection {

	private static final String LOG_TAG = "BluetoothConnection";

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private Context mContext;
    private boolean isDuplexConnection = true;

    /**
     * Constructor. Prepares a new BluetoothConnect session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothConnection(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = Droid2InoConstants.STATE_NONE;
        mHandler = handler;
        mContext = context;
    }

    /**
     * Set the current state of the connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Droid2InoConstants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }


    /**
     * When the connection is only for sending from the mobile device, not in both directions, the
     * thread mustn't be listening to the inputStream.
     * Returns whether this connection is full duplex or not
     * @return
     */
    public boolean isDuplexConnection() {
        return isDuplexConnection;
    }


    /**
     * Set if this connection to full duplex or not
     * @param isDuplexConnection is full duplex or not
     */
    public void setDuplexConnection(boolean isDuplexConnection) {
        this.isDuplexConnection = isDuplexConnection;
    }

    /**
     * Start the connectivity service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(Droid2InoConstants.STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
        	mAcceptThread = new AcceptThread();
        	mAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
        if (mState == Droid2InoConstants.STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(Droid2InoConstants.STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
        	mAcceptThread.cancel();
        	mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Droid2InoConstants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Droid2InoConstants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(Droid2InoConstants.STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
        	mAcceptThread.cancel();
        	mAcceptThread = null;
        }

        setState(Droid2InoConstants.STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != Droid2InoConstants.STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }


    public OutputStream getBTOutputStream() {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != Droid2InoConstants.STATE_CONNECTED) return null;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        return r.getMmOutStream();
    }


    public InputStream getBTInputStream() {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != Droid2InoConstants.STATE_CONNECTED) return null;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        return r.getMmInStream();
    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Droid2InoConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Droid2InoConstants.TOAST, mContext.getString(R.string.connecting_bluetooth_error));
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothConnection.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Droid2InoConstants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Droid2InoConstants.TOAST, mContext.getString(R.string.connection_lost_error));
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothConnection.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(Droid2InoConstants.SOCKET_NAME,
                		Droid2InoConstants.MY_UUID);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName(Droid2InoConstants.ACCEPT_THREAD_NAME);

            BluetoothSocket socket = null;

            //FIXME:
            if(mmServerSocket == null) {
                Log.e(LOG_TAG, "mmServerSocket in run of AcceptThread = null");
                return;
            }

            // Listen to the server socket if we're not connected
            while (mState != Droid2InoConstants.STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Socket accept() failed", e);
                    break;
                    
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Some type error", e);
                    break;
                } 

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothConnection.this) {
                        switch (mState) {
                        case Droid2InoConstants.STATE_LISTEN:
                        case Droid2InoConstants.STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case Droid2InoConstants.STATE_NONE:
                        case Droid2InoConstants.STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(LOG_TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }

        }

        public void cancel() {
            try {
            	if(mmServerSocket != null) {
            		mmServerSocket.close();
            	}
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(
                		Droid2InoConstants.MY_UUID);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(LOG_TAG, "BEGIN mConnectThread");
            setName(Droid2InoConstants.CONNECT_THREAD_NAME);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            //FIXME:
            if(mmSocket == null) {
                Log.e(LOG_TAG, "mmSocket in run of ConnectThread = null");
                return;
            }

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                Log.e(LOG_TAG, "error connecting the socket in run method of connect thread: " + e);
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(LOG_TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnection.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final StringBuffer readMessage;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(LOG_TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            readMessage = new StringBuffer();

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(LOG_TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(LOG_TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            int startIndex = -1;
            int endIndex = -1;
            String message;

            // Keep listening to the InputStream while connected
            while (isDuplexConnection) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    
                    readMessage.append(new String(buffer, 0, bytes));
                    
                    // Check if the string stored is a complete instruction, that starts with "_" and ends with "%"
                    // If the instruction is full send it to the handler and delete it from the StringBuilder
                    
                    startIndex = readMessage.indexOf(Droid2InoConstants.START_READ_DELIMITER);
                    endIndex = readMessage.indexOf(Droid2InoConstants.END_READ_DELIMITER);
                    
//                    Log.e(LOG_TAG, "startindex: " + startIndex);
//                    Log.e(LOG_TAG, "endIndex: " + startIndex);
                    Log.d(LOG_TAG, "readMessage: " + readMessage);
                    
                    if((startIndex != -1) && (endIndex != -1) && (startIndex < endIndex)) {
                    	
                    	message = readMessage.substring(startIndex+2, endIndex);
                    	
                        // Send the obtained message to the UI Activity
                        mHandler.obtainMessage(Droid2InoConstants.MESSAGE_READ, bytes, -1, message)
                                .sendToTarget();
                        
                        // Delete from the first character to delete possible broken messages and not add with the next message
                        readMessage.delete(0, endIndex+1);
                    }
                    
                    startIndex = -1;
                    endIndex = -1;  


                } catch (IOException e) {
                    Log.e(LOG_TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothConnection.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Droid2InoConstants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "close() of connect socket failed", e);
            }
        }

        public InputStream getMmInStream() {
            return mmInStream;
        }

        public OutputStream getMmOutStream() {
            return mmOutStream;
        }
    }

}
