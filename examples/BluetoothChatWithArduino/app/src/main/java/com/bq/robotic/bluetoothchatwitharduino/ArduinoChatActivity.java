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

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
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
import com.bq.robotic.droid2ino.utils.DeviceListDialogStyle;
import com.bq.robotic.droid2ino.utils.Droid2InoConstants;

public class ArduinoChatActivity extends BaseBluetoothConnectionActivity {


	// Debugging
	private static final String LOG_TAG = "ArduinoChatActivity";
	private static final boolean D = true;

	// Layout Views
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;

	// Array adapter for the chat list
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	
	private Menu menu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_arduino_chat);

		// Initialize the array adapter for the chat list
		mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		// Initialize the compose field with a listener for the return key
		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				sendMessage(message);

				// Reset out string buffer to zero and clear the edit text field
				mOutStringBuffer.setLength(0);
				mOutEditText.setText(mOutStringBuffer);
			}
		});

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
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
     * @return the styling class for the lists with the devices
     */
    protected void onDeviceListDialogStyleObtained(DeviceListDialogStyle deviceListDialogStyle) {
        deviceListDialogStyle.getSearchDevicesTitleView().setTextColor(Color.parseColor("#EDCEFF"));
        deviceListDialogStyle.getSearchDevicesTitleView().setBackgroundColor(Color.parseColor("#5F5266"));
        deviceListDialogStyle.getDevicesPairedTitleView().setBackgroundColor(Color.parseColor("#930CFF"));
        deviceListDialogStyle.getNewDevicesTitleView().setBackgroundColor(Color.parseColor("#930CFF"));
    }


	/**
	 * Callback for the changes of the connection status
	 */
	@Override
	public void onConnectionStatusUpdate(int connectionState) {
		switch (connectionState) {
			case Droid2InoConstants.STATE_CONNECTED:
				setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
				mConversationArrayAdapter.clear();
				menu.findItem(R.id.connect_scan).setEnabled(false);
				menu.findItem(R.id.disconnect).setEnabled(true);
				break;
			case Droid2InoConstants.STATE_CONNECTING:
				setStatus(R.string.title_connecting);
				break;
			case Droid2InoConstants.STATE_LISTEN:
			case Droid2InoConstants.STATE_NONE:
				setStatus(R.string.title_not_connected);
                if(menu != null) {
                    menu.findItem(R.id.connect_scan).setEnabled(true);
                    menu.findItem(R.id.disconnect).setEnabled(false);
                }
				break;
			}
	}


	/**
	 * Put the status of the connection in the action bar
	 * @param resId
	 */
	private final void setStatus(int resId) {
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setSubtitle(resId);
	}


	/**
	 * Put the status of the connection in the action bar
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
				mOutStringBuffer.setLength(0);
				mOutEditText.setText(mOutStringBuffer);
				return true;
			}
			if(D) Log.i(LOG_TAG, "END onEditorAction");
			return false;
		}
	};


	/**
	 * Callback for incoming messages from the arduino board
	 */
	@Override
	public void onNewMessage(String message) {
		mConversationArrayAdapter.add(mConnectedDeviceName+":  " + message);
	}


	/**
	 * Callback for messages sent to the arduino board
	 */
	@Override
	public void onWriteSuccess(String message) {
		mConversationArrayAdapter.add("Me:  " + message);
	}


}
