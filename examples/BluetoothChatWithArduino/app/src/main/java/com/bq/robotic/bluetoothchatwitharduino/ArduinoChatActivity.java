/*
* This file is part of the GamePad
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

package com.bq.robotic.bluetoothchatwitharduino;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.bq.robotic.droid2ino.activities.BaseBluetoothConnectionActivity;
import com.bq.robotic.droid2ino.utils.Droid2InoConstants;
import com.bq.robotic.droid2ino.views.DevicesListDialogStyle;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ArduinoChatActivity extends BaseBluetoothConnectionActivity
   implements EasyPermissions.PermissionCallbacks {

   // Debugging
   private static final String LOG_TAG = "ArduinoChatActivity";
   private static final boolean D = true;

   // Layout Views
   private ListView conversationView;
   private EditText outEditText;
   private Button sendButton;

   // Array adapter for the chat list
   private ArrayAdapter<String> conversationArrayAdapter;
   // String buffer for outgoing messages
   private StringBuffer outStringBuffer;

   private Menu menu;

   // Permissions
   // Location permission is now needed in order to scan for near bluetooth devices
   private static final int RC_LOCATION_PERM = 124;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_arduino_chat);

      // Initialize the array adapter for the chat list
      conversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
      conversationView = (ListView) findViewById(R.id.in);
      conversationView.setAdapter(conversationArrayAdapter);

      // Initialize the compose field with a listener for the return key
      outEditText = (EditText) findViewById(R.id.edit_text_out);
      outEditText.setOnEditorActionListener(mWriteListener);

      // Initialize the send button with a listener for click events
      sendButton = (Button) findViewById(R.id.button_send);
      sendButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            // Send a message using content of the edit text widget
            TextView view = (TextView) findViewById(R.id.edit_text_out);
            String message = view.getText().toString();
            sendMessage(message);

            // Reset out string buffer to zero and clear the edit text field
            outStringBuffer.setLength(0);
            outEditText.setText(outStringBuffer);
         }
      });

      // Initialize the buffer for outgoing messages
      outStringBuffer = new StringBuffer("");

      requestPermissions();
   }

   @Override protected void setupSession() {
      super.setupSession();

      showOneBtOptionDialog(true);
   }

   /**
    * Callback for the menu options
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.arduino_chat, menu);
      this.menu = menu;
      menu.findItem(R.id.disconnect).setEnabled(false);
      return true;
   }


   /**
    * When click on connect button of the menu in the action bar, initialize the dialog for searching
    * the bluetooth devices (paired and new ones). The title views of that dialog can be stylized
    * as it is shown in this example.
    */
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.connect_scan:

            requestDeviceConnection();
            return true;

         case R.id.disconnect:

            stopBluetoothConnection();
            return true;
      }

      return false;
   }


   /**
    * Style the lists with the bluetooth devices
    *
    * @return the styling class for the lists with the devices
    */
   protected void onDeviceListDialogStyleObtained(DevicesListDialogStyle deviceListDialogStyle) {
      deviceListDialogStyle.setColorRes(this, R.color.primary_color, R.color.title_background_color);
   }

   @Override
   protected void onConnectionStatusUpdated(Droid2InoConstants.ConnectionState connectionState) {
      super.onConnectionStatusUpdated(connectionState);
      Log.d(LOG_TAG, "onConnectionStatusUpdated = " + connectionState);
      switch (connectionState) {
         case CONNECTED_CONFIGURED:
            setStatus(getString(R.string.title_connected_to, connectedDeviceName));
            conversationArrayAdapter.clear();
            menu.findItem(R.id.connect_scan).setEnabled(false);
            menu.findItem(R.id.disconnect).setEnabled(true);
            break;
         case CONNECTING:
            setStatus(R.string.title_connecting);
            break;
         case LISTENING:
         case DISCONNECTED:
            setStatus(R.string.title_not_connected);
            if (menu != null) {
               menu.findItem(R.id.connect_scan).setEnabled(true);
               menu.findItem(R.id.disconnect).setEnabled(false);
            }
            break;
      }
   }

   @Override protected void onMessageSent(String message) {
      super.onMessageSent(message);
      conversationArrayAdapter.add("Me:  " + message);
   }

   @Override protected void onMessageReceived(String message) {
      super.onMessageReceived(message);
      conversationArrayAdapter.add(connectedDeviceName + ":  " + message);
   }

   /**
    * Put the status of the connection in the action bar
    *
    * @param resId
    */
   private final void setStatus(int resId) {
      final ActionBar actionBar = getSupportActionBar();
      actionBar.setSubtitle(resId);
   }


   /**
    * Put the status of the connection in the action bar
    *
    * @param subTitle subtitle text
    */
   private final void setStatus(CharSequence subTitle) {
      final ActionBar actionBar = getSupportActionBar();
      actionBar.setSubtitle(subTitle);
   }


   // The action listener for the EditText widget, to listen for the return key
   private TextView.OnEditorActionListener mWriteListener =
      new TextView.OnEditorActionListener() {
         public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
               String message = view.getText().toString();
               sendMessage(message);

               // Reset out string buffer to zero and clear the edit text field
               outStringBuffer.setLength(0);
               outEditText.setText(outStringBuffer);
               return true;
            }
            if (D) Log.i(LOG_TAG, "END onEditorAction");
            return false;
         }
      };


   /************************************ PERMISSIONS **********************************************/
   @AfterPermissionGranted(RC_LOCATION_PERM)
   private void requestPermissions() {
      String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION};

      // If permission is already granted don't do anything else here
      if (EasyPermissions.hasPermissions(this, perms)) return;

      EasyPermissions.requestPermissions(this, getString(R.string.permission_location),
         RC_LOCATION_PERM, perms);
   }

   @Override public void onPermissionsGranted(int requestCode, List<String> perms) {
      Log.d(LOG_TAG, "Location permission granted");
   }

   @Override
   public void onPermissionsDenied(int requestCode, List<String> perms) {
      Log.d(LOG_TAG, "Location permission denied");

      // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
      if (!EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
         new AlertDialog.Builder(this)
            .setMessage(getString(R.string.rationale_location))
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .show();
      }
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);

      // Forward results to EasyPermissions
      EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
   }

}
