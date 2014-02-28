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


package com.bq.robotic.droid2ino.utils;

import android.widget.TextView;

/**
 * Class for styling the dialog UI of the list of the available and paired bluetooth devices
 */

public class DeviceListDialogStyle {
	
	private TextView searchDevicesTitleView;
	private TextView devicesPairedTitleView;
	private TextView newDevicesTitleView;
	
	
	public DeviceListDialogStyle(TextView searchDevicesTitleView, TextView devicesPairedTitleView, 
			TextView newDevicesTitleView) {
		this.searchDevicesTitleView = searchDevicesTitleView;
		this.devicesPairedTitleView = devicesPairedTitleView;
		this.newDevicesTitleView = newDevicesTitleView;
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
