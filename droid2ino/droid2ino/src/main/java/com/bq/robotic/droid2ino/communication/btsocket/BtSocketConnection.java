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

package com.bq.robotic.droid2ino.communication.btsocket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.bq.robotic.droid2ino.R;
import com.bq.robotic.droid2ino.utils.ConnectionErrorFeedback;
import com.bq.robotic.droid2ino.utils.Droid2InoConstants;
import com.bq.robotic.droid2ino.utils.Droid2InoConstants.ConnectionState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.bq.robotic.droid2ino.utils.Droid2InoConstants.ConnectionState.*;


/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 * <p>
 * based on http://developer.android.com/resources/samples/BluetoothChat/index.html
 */

public class BtSocketConnection {
   private static final String LOG_TAG = BtSocketConnection.class.getSimpleName();

   /**
    * Name of the threads
    */
   private static final String ACCEPT_THREAD_NAME = "AcceptThread";
   private static final String CONNECT_THREAD_NAME = "ConnectThread";
   private static final String CONNECTED_THREAD_NAME = "ConnectedThread";
   /**
    * Unique UUID for this application
    */
   private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

   // Member fields
   private final BluetoothAdapter adapter;
   private final Handler handler;
   private AcceptThread acceptThread;
   private ConnectThread connectThread;
   private ConnectedThread connectedThread;
   private ConnectionState state;
   private Context context;
   private boolean isDuplexConnection = true;

   /**
    * Constructor. Prepares a new BluetoothConnect session.
    *
    * @param context The UI Activity Context
    * @param handler A Handler to send messages back to the UI Activity
    */
   public BtSocketConnection(Context context, Handler handler) {
      adapter = BluetoothAdapter.getDefaultAdapter();
      state = DISCONNECTED;
      this.handler = handler;
      this.context = context;
   }

   /**
    * Set the current state of the connection
    *
    * @param state An integer defining the current connection state
    */
   private synchronized void setState(ConnectionState state) {
      this.state = state;

      // Give the new state to the Handler so the UI Activity can update
      handler.obtainMessage(Droid2InoConstants.MESSAGE_STATE_CHANGE, state).sendToTarget();
   }

   /**
    * Return the current connection state.
    */
   public synchronized ConnectionState getState() {
      return state;
   }


   /**
    * When the connection is only for sending from the mobile device, not in both directions, the
    * thread mustn't be listening to the inputStream.
    * Returns whether this connection is full duplex or not
    *
    * @return
    */
   public boolean isDuplexConnection() {
      return isDuplexConnection;
   }


   /**
    * Set if this connection to full duplex or not
    *
    * @param isDuplexConnection is full duplex or not
    */
   public void setDuplexConnection(boolean isDuplexConnection) {
      this.isDuplexConnection = isDuplexConnection;
   }

   /**
    * Start the connectivity service. Specifically start AcceptThread to begin a
    * session in listening (server) mode. Called by the Activity onResume()
    */
   public synchronized void start() {

      // Cancel any thread attempting to make a connection
      if (connectThread != null) {
         connectThread.cancel();
         connectThread = null;
      }

      // Cancel any thread currently running a connection
      if (connectedThread != null) {
         connectedThread.cancel();
         connectedThread = null;
      }

      setState(LISTENING);

      // Start the thread to listen on a BluetoothServerSocket
      if (acceptThread == null) {
         acceptThread = new AcceptThread();
         acceptThread.start();
      }
   }

   /**
    * Start the ConnectThread to initiate a connection to a remote device.
    *
    * @param device The BluetoothDevice to connect
    */
   public synchronized void connect(BluetoothDevice device) {

      // Cancel any thread attempting to make a connection
      if (state == CONNECTING) {
         if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
         }
      }

      // Cancel any thread currently running a connection
      if (connectedThread != null) {
         connectedThread.cancel();
         connectedThread = null;
      }

      // Start the thread to connect with the given device
      connectThread = new ConnectThread(device);
      connectThread.start();
      setState(CONNECTING);
   }

   /**
    * Start the ConnectedThread to begin managing a Bluetooth connection
    *
    * @param socket The BluetoothSocket on which the connection was made
    * @param device The BluetoothDevice that has been connected
    */
   public synchronized void connected(BluetoothSocket socket, BluetoothDevice
      device) {

      // Cancel the thread that completed the connection
      if (connectThread != null) {
         connectThread.cancel();
         connectThread = null;
      }

      // Cancel any thread currently running a connection
      if (connectedThread != null) {
         connectedThread.cancel();
         connectedThread = null;
      }

      // Cancel the accept thread because we only want to connect to one device
      if (acceptThread != null) {
         acceptThread.cancel();
         acceptThread = null;
      }

      // Start the thread to manage the connection and perform transmissions
      connectedThread = new ConnectedThread(socket);
      connectedThread.start();

      // Send the name of the connected device back to the UI Activity
      handler.obtainMessage(Droid2InoConstants.MESSAGE_DEVICE_NAME, device.getName()).sendToTarget();

      setState(CONNECTED_CONFIGURED);
   }

   /**
    * Stop all threads
    */
   public synchronized void stop() {

      if (connectThread != null) {
         connectThread.cancel();
         connectThread = null;
      }

      if (connectedThread != null) {
         connectedThread.cancel();
         connectedThread = null;
      }

      if (acceptThread != null) {
         acceptThread.cancel();
         acceptThread = null;
      }

      setState(DISCONNECTED);
   }

   /**
    * Write to the ConnectedThread in an unsynchronized manner
    *
    * @param out The bytes to write
    * @see ConnectedThread#write(byte[])
    */
   public void write(byte[] out) {
      // Create temporary object
      ConnectedThread r;
      // Synchronize a copy of the ConnectedThread
      synchronized (this) {
         if (state != CONNECTED_CONFIGURED) return;
         r = connectedThread;
      }
      // Perform the write unsynchronized
      r.write(out);
   }


   public OutputStream getBTOutputStream() {
      // Create temporary object
      ConnectedThread r;
      // Synchronize a copy of the ConnectedThread
      synchronized (this) {
         if (state != CONNECTED_CONFIGURED) return null;
         r = connectedThread;
      }
      // Perform the write unsynchronized
      return r.getMmOutStream();
   }


   public InputStream getBTInputStream() {
      // Create temporary object
      ConnectedThread r;
      // Synchronize a copy of the ConnectedThread
      synchronized (this) {
         if (state != CONNECTED_CONFIGURED) return null;
         r = connectedThread;
      }
      // Perform the write unsynchronized
      return r.getMmInStream();
   }


   /**
    * Indicate that the connection attempt failed and notify the UI Activity.
    */
   private void connectionFailed(Exception e) {
      // Send a failure message back to the Activity
      handler.obtainMessage(Droid2InoConstants.MESSAGE_ERROR,
         new ConnectionErrorFeedback(context.getString(R.string.connecting_bluetooth_error),
            ERROR_CONNECTING, e)).sendToTarget();

      // Start the service over to restart listening mode
      BtSocketConnection.this.start();
   }

   /**
    * Indicate that the connection was lost and notify the UI Activity.
    */
   private void connectionLost(Exception e) {
      // Send a failure message back to the Activity
      handler.obtainMessage(Droid2InoConstants.MESSAGE_ERROR,
         new ConnectionErrorFeedback(context.getString(R.string.connection_lost_error),
            ERROR_CONNECTING, e)).sendToTarget();

      state = DISCONNECTED;

      // Start the service over to restart listening mode
      BtSocketConnection.this.start();
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
            tmp = adapter.listenUsingRfcommWithServiceRecord(Droid2InoConstants.SOCKET_NAME,
               MY_UUID);
         } catch (IOException e) {
            Log.e(LOG_TAG, "Socket listen() failed", e);
         }
         mmServerSocket = tmp;
         state = LISTENING;
      }

      public void run() {
         setName(ACCEPT_THREAD_NAME);

         BluetoothSocket socket = null;

         //FIXME:
         if (mmServerSocket == null) {
            Log.e(LOG_TAG, "mmServerSocket in run of AcceptThread = null");
            return;
         }

         // Listen to the server socket if we're not connected
         while (state != CONNECTED_CONFIGURED) {
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
               synchronized (BtSocketConnection.this) {
                  switch (state) {
                     case LISTENING:
                     case CONNECTING:
                        // Situation normal. Start the connected thread.
                        connected(socket, socket.getRemoteDevice());
                        break;
                     case DISCONNECTED:
                     case CONNECTED_CONFIGURED:
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
            if (mmServerSocket != null) {
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
               MY_UUID);
         } catch (IOException e) {
            Log.e(LOG_TAG, "Socket create() failed", e);
         }
         mmSocket = tmp;
         state = CONNECTING;
      }

      public void run() {
         Log.d(LOG_TAG, "BEGIN connectThread");
         setName(CONNECT_THREAD_NAME);

         // Always cancel discovery because it will slow down a connection
         adapter.cancelDiscovery();

         //FIXME:
         if (mmSocket == null) {
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
            connectionFailed(e);
            return;
         }

         // Reset the ConnectThread because we're done
         synchronized (BtSocketConnection.this) {
            connectThread = null;
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
         state = CONNECTED_CONFIGURED;
      }

      public void run() {
         setName(CONNECTED_THREAD_NAME);

         Log.d(LOG_TAG, "BEGIN connectedThread");
         byte[] buffer = new byte[1024];
         int bytes;
         int startIndex = -1;
         int endIndex = -1;
         String message;

         // Keep listening to the InputStream while connected
         while (isDuplexConnection && state == CONNECTED_CONFIGURED) {
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

               if ((startIndex != -1) && (endIndex != -1) && (startIndex < endIndex)) {

                  message = readMessage.substring(startIndex + 2, endIndex);

                  // Send the obtained message to the UI Activity
                  handler.obtainMessage(Droid2InoConstants.MESSAGE_RECEIVED, message).sendToTarget();

                  // Delete from the first character to delete possible broken messages and not add with the next message
                  readMessage.delete(0, endIndex + 1);
               }

               startIndex = -1;
               endIndex = -1;


            } catch (IOException e) {
               Log.e(LOG_TAG, "disconnected", e);
               connectionLost(e);
               // Start the service over to restart listening mode
               BtSocketConnection.this.start();
               break;
            }
         }
      }

      /**
       * Write to the connected OutStream.
       *
       * @param buffer The bytes to write
       */
      public void write(byte[] buffer) {
         try {
            mmOutStream.write(buffer);

            // Share the sent message back to the UI Activity
            handler.obtainMessage(Droid2InoConstants.MESSAGE_SENT, buffer).sendToTarget();
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
