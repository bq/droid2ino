package com.bq.robotic.androidino;

import android.widget.TextView;

public class DeviceListDialogStyle {
	
	private TextView searchDevicesTitleView;
	private TextView devicesPairedTitleView;
	private TextView newDevicesTitleView;
	
	
	public DeviceListDialogStyle(TextView searchDevicesTitleView, TextView devicesPairedTitleView, TextView newDevicesTitleView) {
		this.searchDevicesTitleView = searchDevicesTitleView;
		this.devicesPairedTitleView = devicesPairedTitleView;
		this.newDevicesTitleView = devicesPairedTitleView;
	}

	
    /**
     * Get the TextView of the title of the dialog for searching the bluetooth devices
     * @return 
     */
    public TextView getSearchDevicesTitleView() {
    	return searchDevicesTitleView;
    }

    
    /**
     * Get the TextView of the title of the dialog for the paired bluetooth devices
     * @return 
     */
    public TextView getDevicesPairedTitleView() {
    	return devicesPairedTitleView;
    }

    
    /**
     * Get the TextView of the title of the dialog for the new bluetooth devices
     * @return 
     */
    public TextView getNewDevicesTitleView() {
    	return newDevicesTitleView;
    }
	
}
