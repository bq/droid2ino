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

package com.bq.robotic.droid2ino.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.bq.robotic.droid2ino.communication.BluetoothManager;
import com.bq.robotic.droid2ino.communication.ble.BleProfile;
import com.bq.robotic.droid2ino.views.BtDevicesListDialog;
import com.bq.robotic.droid2ino.views.DevicesListDialogStyle;
import com.bq.robotic.droid2ino.R;
import com.bq.robotic.droid2ino.communication.BtCommunicationListener;
import com.bq.robotic.droid2ino.communication.BtCommunicationListenerAdapter;
import com.bq.robotic.droid2ino.utils.Droid2InoConstants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseBluetoothConnectionActivity extends AppCompatActivity {

   /**
    * This is the main abstract Activity that can be used by clients to setup Bluetooth.
    * It provides helper methods that can be used to find and connect devices.
    */

   private static final String LOG_TAG = BaseBluetoothConnectionActivity.class.getSimpleName();

   /**
    * Name of the connected device.
    */
   protected String connectedDeviceName = null;
   /**
    * The user requested the list of the bluetooth devices available.
    */
   protected boolean deviceConnectionWasRequested = false;
   // Used for not trying to show the BT dialog fragment when the app is in background as it can crash
   private boolean isAppInBackground = true;

   private BluetoothManager bluetoothManager;
   private BtDevicesListDialog btDevicesListDialog;
   private boolean showOneBtOptionDialog = false;
   private static final String DEVICE_DIALOG_FRAGMENT_TAG = "deviceDialog";

   private BtDevicesListDialog.DialogListener dialogListener = new BtDevicesListDialog.DialogListener() {
      @Override public void onBtDeviceSelected(@NotNull final String btDeviceAddress) {
         Log.d(LOG_TAG, "BT device selected from list");
         btDevicesListDialog = null;
         bluetoothManager.connectDevice(btDeviceAddress);
      }

      @Override public void onCancel() {
         Log.d(LOG_TAG, "BT devices list dialog was cancelled");
         btDevicesListDialog = null;
      }

      @Override
      public void onDevicesListDialogStyleCreated(DevicesListDialogStyle deviceListDialogStyle) {
         onDeviceListDialogStyleObtained(deviceListDialogStyle);
      }

      @Override
      public void onBtTypeSelectedChanged(@NotNull BluetoothManager.BtConnectionType selectedBtType) {
         selectBtConnectionType(selectedBtType);
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      bluetoothManager = new BluetoothManager(this);

      if (savedInstanceState != null) {
         bluetoothManager.onEnableBluetoothIsAllowed(savedInstanceState
            .getBoolean(Droid2InoConstants.WAS_BLUETOOTH_ALLOWED_KEY));
      }

      bluetoothManager.onCreate(this);
      bluetoothManager.setBtCommunicationListener(new BtCommunicationListenerAdapter() {
         @Override public void onBluetoothAdapterWasEnabled() {
            // If the user requested the list of the bluetooth devices available, show it
            if (deviceConnectionWasRequested) {
               showListDialog();
            }
         }

         @Override
         public void onConnectionStatusUpdated(@NotNull Droid2InoConstants.ConnectionState connectionState) {
            BaseBluetoothConnectionActivity.this.onConnectionStatusUpdated(connectionState);
         }

         @Override public void onMessageSent(@NotNull String message) {
            BaseBluetoothConnectionActivity.this.onMessageSent(message);
         }

         @Override public void onMessageReceived(@NotNull String message) {
            BaseBluetoothConnectionActivity.this.onMessageReceived(message);
         }

         @Override public void onValueReceived(@NotNull byte[] value) {
            BaseBluetoothConnectionActivity.this.onValueReceived(value);
         }

         @Override public void onDeviceNameObtained(@NotNull String deviceName) {
            BaseBluetoothConnectionActivity.this.onDeviceNameObtained(deviceName);
         }

         @Override
         public void onPreConnectionChangesTo(@NotNull BluetoothManager.BtConnectionType connectionTypeChangedTo) {
            BaseBluetoothConnectionActivity.this.onPreConnectionChangesTo(connectionTypeChangedTo);
         }

         @Override public void onError(@Nullable String errorMessage,
                                       @Nullable Droid2InoConstants.ConnectionState errorState,
                                       @Nullable Exception e) {
            BaseBluetoothConnectionActivity.this.onError(errorMessage, errorState, e);
         }
      });

      setupSession();
   }

   @Override
   protected void onResume() {
      super.onResume();
      Log.v(LOG_TAG, "onResume");
      bluetoothManager.onResume();
   }

   @Override protected void onResumeFragments() {
      super.onResumeFragments();

      // Don't let the user show the fragment until the activity state is recover, if not, the app can crash
      isAppInBackground = false;
      // If the user requested the list of the bluetooth devices available, show it
      if (deviceConnectionWasRequested) {
         showListDialog();
      }
   }

   @Override
   protected void onPause() {
      super.onPause();
      Log.v(LOG_TAG, "onPause");
      isAppInBackground = true;
      bluetoothManager.onPause();
      btDevicesListDialog = null;
   }

   @Override protected void onDestroy() {
      super.onDestroy();
      Log.v(LOG_TAG, "onDestroy");
      bluetoothManager.onDestroy();
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      Log.v(LOG_TAG, "onSaveInstanceState");
      // Save UI state changes to the savedInstanceState.
      // This bundle will be passed to onCreate if the process is
      // killed and restarted.
      savedInstanceState.putBoolean(Droid2InoConstants.WAS_BLUETOOTH_ALLOWED_KEY,
         bluetoothManager.isEnableBluetoothAllowed());
   }

   @Override
   public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      Log.v(LOG_TAG, "onRestoreInstanceState");
      // Restore UI state from the savedInstanceState.
      // This bundle has also been passed to onCreate.
      bluetoothManager.onEnableBluetoothIsAllowed(savedInstanceState
         .getBoolean(Droid2InoConstants.WAS_BLUETOOTH_ALLOWED_KEY));
   }

   /**
    * This method will be called at the end of onCreate(), overwrite it in order to select another
    * connection type or other configurations. The type of connection can be changed after using
    * {@link BaseBluetoothConnectionActivity#selectBtConnectionType(BluetoothManager.BtConnectionType)}
    * {@link BluetoothManager.BtConnectionType#BT_SOCKET} is selected by default with a DUPLEX connection.
    * Here we can configure the future BT socket and BLE connections without starting either of them.
    * This is useful for when the user changes between both types in the {@link BtDevicesListDialog}
    * but we want to maintain the configuration of being simplex or duplex or the BLE profile between
    * those changes. In any case, we can change it when the
    * {@link BtCommunicationListener#onPreConnectionChangesTo(BluetoothManager.BtConnectionType)}
    * is called.
    */
   protected void setupSession() {
      configureBtSocketConnectionType(true);
   }

   /**
    * Callback that will be invoked when Bluetooth connectivity state changes
    *
    * @param connectionState Message types sent from the BluetoothConnectService Handler
    */
   protected void onConnectionStatusUpdated(Droid2InoConstants.ConnectionState connectionState) {
      Log.d(LOG_TAG, "Connection status updated  : " + connectionState);
   }

   /**
    * Callback that will be invoked after message was sent successfully.
    *
    * @param message data that was sent to remote device
    */
   protected void onMessageSent(String message) {
      Log.d(LOG_TAG, "Message sent: " + message);
   }

   /**
    * Callback that will be invoked when new message is received.
    *
    * @param message new message string
    */
   protected void onMessageReceived(String message) {
      Log.d(LOG_TAG, "Message received: " + message);
   }

   /**
    * Callback that will be invoked when a new value is received.
    *
    * @param value new byte[] value
    */
   protected void onValueReceived(byte[] value) {
      Log.d(LOG_TAG, "Value received: " + value);
   }

   /**
    * Callback that will be invoked when we obtain the name of the device to which we are connected to.
    */
   protected void onDeviceNameObtained(String deviceName) {
      connectedDeviceName = deviceName;
      Toast.makeText(getApplicationContext(), getString(R.string.connected_to) + connectedDeviceName,
         Toast.LENGTH_SHORT).show();
   }

   /**
    * Callback called when the connection type is going to be changed, so if another extra configuration
    * over that connection has to be set, such as setting as simplex or a custom BLE profile.
    */
   protected void onPreConnectionChangesTo(BluetoothManager.BtConnectionType connectionTypeChangedTo) {
      Log.d(LOG_TAG, "Connection is going to be changed to: " + connectionTypeChangedTo);
   }

   /**
    * Callback that will be invoked when an error appears.
    */
   protected void onError(String errorMessage, Droid2InoConstants.ConnectionState errorState,
               Exception e) {
      Log.d(LOG_TAG, "Error: " + errorMessage + " with error state: " + errorState);
      if (e != null) Log.d(LOG_TAG, "Trace: " + e);
   }

   /**
    * Stops the current bluetooth connection but doesn't disable the BT adapter as the
    * {@link BaseBluetoothConnectionActivity#stopApp()} method does.
    */
   protected final void stopBluetoothConnection() {
      bluetoothManager.stopBluetoothConnection();
   }

   /**
    * This method stops all threads of the BluetoothConnection and disable the Bluetooth in the
    * mobile device if it was disabled before this app
    * This is protected in case that a child wants to close when the user press a button or other view
    */
   protected final void stopApp() {
      bluetoothManager.fullStop();
   }

   /**
    * Selects a new connection type (BT socket, BLE...) by closing the before connection type and
    * opening a new one of the required type. This method will use default configure values for the
    * connection types or the ones previously configured.
    * {@link BluetoothManager.BtConnectionType#BT_SOCKET} is selected by default with a duplex
    * connection. If you want to use non default configuration values for the bt socket or ble call
    * to {@link BaseBluetoothConnectionActivity#configureBtSocketConnectionType(boolean)} or
    * {@link BaseBluetoothConnectionActivity#configureBleConnectionType(BleProfile)} and then just
    * call to this method in consecutive changes between types.
    * Also, instead of calling this method, a connection type can be selected and configured by calling
    * to {@link BaseBluetoothConnectionActivity#selectBtSocketConnectionType(boolean)} or
    * {@link BaseBluetoothConnectionActivity#selectBleConnectionType(BleProfile)}.
    *
    * Take into account that when selecting a type of connection in the {@link BtDevicesListDialog},
    * it will call to this method, so please configure the connection types before that or when the
    * {@link BtCommunicationListener#onPreConnectionChangesTo(BluetoothManager.BtConnectionType)}
    * callback in invoked.
    */
   protected final void selectBtConnectionType(BluetoothManager.BtConnectionType connectionType) {
      if (connectionType == null || connectionType == bluetoothManager.getBtConnectionType()) return;

      if (connectionType == BluetoothManager.BtConnectionType.BLE)
         bluetoothManager.selectBleConnectionType();
      else
         bluetoothManager.selectBtSocketConnectionType();

      if (btDevicesListDialog != null && btDevicesListDialog.isVisible()) {
         btDevicesListDialog.selectBtScannerType(this, bluetoothManager.getBtConnectionType());
      }
   }

   /**
    * Select {@link BluetoothManager.BtConnectionType#BT_SOCKET} as the connection type to use.
    * A simplex connection can be set by passing false as parameter if reading anything from the
    * connected device isn't needed.
    *
    * @param isDuplex   True if a duplex connection is required, false if a simplex connection is required
    */
   protected final void selectBtSocketConnectionType(boolean isDuplex) {
      bluetoothManager.selectBtSocketConnectionType(isDuplex);
   }

   /**
    * Configure a future possible BT socket connection to be duplex or simplex. This doesn't select,
    * start or prepare that connection if it isn't has started, just configure it for a future connection.
    * This method is also useful to be called when a configuration is required after changed to a connection
    * type. If there is an ongoing BT socket connection it would change it to a DUPLEX or SIMPLEX one.
    * See [{@link BtCommunicationListener#onPreConnectionChangesTo(BluetoothManager.BtConnectionType)}].
    *
    * @param isBtSocketTypeDuplex   True to configure a future BT socket connection to be duplex
    */
   protected final void configureBtSocketConnectionType(boolean isBtSocketTypeDuplex) {
      bluetoothManager.configureBtSocketConnectionType(isBtSocketTypeDuplex);
   }

   /**
    * Configure a future possible BLE connection to use a custom {@link BleProfile}.
    * This doesn't start or prepare that connection if it isn't has started, just configure it for
    * a future connection. If there is an ongoing BLE connection, it will be stopped.
    * This method is also useful to be called when a configuration is required after changed to a
    * connection type.
    * See [{@link BtCommunicationListener#onPreConnectionChangesTo(BluetoothManager.BtConnectionType)}].
    *
    * @param bleProfile   The {@link BleProfile} to use in a future BLE connection
    */
   protected final void configureBleConnectionType(BleProfile bleProfile) {
      bluetoothManager.configureBleConnectionType(bleProfile);
   }

   /**
    * Select {@link BluetoothManager.BtConnectionType#BT_SOCKET} as the connection type to use.
    * A {@link BleProfile} can be passed to be used with this connection. The {@link BleProfile}
    * contains the UUIDs for the services and characteristic we want to read in the connected device.
    * The default profile is the {@link com.bq.robotic.droid2ino.communication.ble.BqZumCoreProfile}
    * used to communicate with the BqZum Core boards.
    *
    * @param bleProfile A BleProfile to use in this BLE connection
    */
   protected final void selectBleConnectionType(BleProfile bleProfile) {
      bluetoothManager.selectBleConnectionType(bleProfile);
   }

   /**
    * Obtain the current {@link BluetoothManager.BtConnectionType}.
    *
    * @return  The current {@link BluetoothManager.BtConnectionType}
    */
   protected final BluetoothManager.BtConnectionType getConnectionType() {
      return bluetoothManager.getBtConnectionType();
   }

   /**
    * Method that should be called when the user allows or forbids the app to enable the bluetooth,
    * so the app enables it when it's necessary.
    *
    * @param allowed True if the allows the app to enable the bluetooth, false if is forbidden
    */
   protected final void onEnableBluetoothAllowed(boolean allowed) {
      bluetoothManager.onEnableBluetoothIsAllowed(allowed);
   }

   /**
    * Method that checks if the user has allowed the app to enable the bluetooth.
    *
    * @return  True is the user has allowed the app to enable the bluetooth
    */
   protected final boolean isEnableBluetoothAllowed() {
      return bluetoothManager.isEnableBluetoothAllowed();
   }

   /**
    * Sends a message to the connected device if any.
    *
    * @param message A string of text to send.
    */
   protected final void sendMessage(final String message) {
      bluetoothManager.sendMessage(message);
   }

   /**
    * Sends a message to the connected device if any.
    *
    * @param messageBuffer A string of text to send.
    */
   protected final void sendMessage(byte[] messageBuffer) {
      bluetoothManager.sendMessage(messageBuffer);
   }

   /**
    * Checks if the mobile device is connected to another device.
    */
   protected final boolean isConnected() {
      return bluetoothManager.isConnected();
   }

   /**
    * Checks if the mobile device is connected to another device and show a toast if it isn't connected.
    */
   protected boolean isConnectedWithToast() {
      if (!bluetoothManager.isConnected()) {
         runOnUiThread(new Runnable() {
            @Override public void run() {
               Toast.makeText(BaseBluetoothConnectionActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            }
         });
         return false;
      }
      return true;
   }

   protected final void showOneBtOptionDialog(boolean showOneBtOption) {
      showOneBtOptionDialog = showOneBtOption;
   }

   /**
    * Set a dialog listener that will be used in a {@link BtDevicesListDialog}.
    *
    * @param dialogListener   Listener that will be used in a {@link BtDevicesListDialog}
    */
   protected final void setDialogListener(BtDevicesListDialog.DialogListener dialogListener) {
      this.dialogListener = dialogListener;
   }

   /**
    * Helper to launch {@link BtDevicesListDialog}.
    */
   private BtDevicesListDialog createAndShowBtDeviceList(BtDevicesListDialog.DialogListener listener) {
      if (btDevicesListDialog == null) {
         btDevicesListDialog = BtDevicesListDialog.Companion.newInstance(bluetoothManager.getBtConnectionType(), showOneBtOptionDialog);
      }

      if (!btDevicesListDialog.isVisible())
         btDevicesListDialog.show(getSupportFragmentManager(), DEVICE_DIALOG_FRAGMENT_TAG);

      btDevicesListDialog.setListener(listener);
      return btDevicesListDialog;
   }

   /**
    * Launch the {@link BtDevicesListDialog} to see devices and do scan.
    */
   protected void requestDeviceConnection() {
      if (bluetoothManager.isBtAdapterEnabled() && !isAppInBackground) {
         createAndShowBtDeviceList(dialogListener);

      } else {
         deviceConnectionWasRequested = true;
         bluetoothManager.enableBluetooth(this);
      }
   }

   /**
    * Shows the Bluetooth devices available list to the user, when the user requested it
    * but the Bluetooth wasn't enable, and the list must wait to the Bluetooth being enable
    * for showing it.
    */
   private void showListDialog() {
      if (bluetoothManager.isBtAdapterEnabled() && !isAppInBackground) {
         createAndShowBtDeviceList(dialogListener);
         deviceConnectionWasRequested = false;

      } else {
         deviceConnectionWasRequested = false;
      }
   }

   /**
    * Style the lists with the bluetooth devices.
    *
    * @return the styling class for the lists with the devices
    */
   protected void onDeviceListDialogStyleObtained(DevicesListDialogStyle deviceListDialogStyle) {
      // By default, do nothing
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      switch (requestCode) {
         case Droid2InoConstants.REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
               // User accepted to enable the bluetooth. Bluetooth is now enabled, so prepare the Bluetooth environment
               bluetoothManager.onEnableBluetoothIsAllowed(true);
               Log.v(LOG_TAG, "BT enabling allowed");

               // This is called before onResume() is called, so, if wait until onResume to show
               // the bt devices dialog
            } else {
               // User did not enable Bluetooth or an error occurred
               deviceConnectionWasRequested = false;
               bluetoothManager.onEnableBluetoothIsAllowed(false);


               Log.d(LOG_TAG, "BT enabling forbidden");
               Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            }
      }
   }

}
