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

import android.os.Bundle;


/**
 * Listener for the dialog that lists the paired and new devices detected 
 *
 */

public interface DialogListener {

    /**
     * Called when a dialog completes.
     *
     * Executed by the thread that initiated the dialog.
     *
     * @param values
     *            Key-value string pairs extracted from the response.
     */
    public void onComplete(Bundle values);

    
    /**
     * Called when a dialog is canceled by the user.
     *
     * Executed by the thread that initiated the dialog.
     *
     */
    public void onCancel();
}
