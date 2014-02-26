package com.bq.robotic.androidino.activities;

public abstract class BaseBluetoothSendOnlyActivity extends BaseBluetoothConnectionActivity {
	
	
    /**
     * Callback method invoked when the device receives a message from the Arduino
     * through the bluetooth connection
     * 
     * @param message The message received from the Arduino
     */
	@Override
	public final void onNewMessage(String message) {
		// This activity don't receive never from the Arduino
	}

}
