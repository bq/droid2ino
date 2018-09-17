/*
 * This file is part of the Androidino
 *
 * Copyright (C) 2017 Mundo Reader S.L.
 *
 * Date: August 2018
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

package com.bq.robotic.droid2ino.views

import android.content.Context
import android.graphics.PorterDuff
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.widget.ImageButton
import android.widget.TextView
import android.view.View
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.support.annotation.DrawableRes

/**
 * Utils class for style the components of the scanned bluetooth devices dialog [BtDevicesListDialog].
 * @param bleSelectorView will be null if the BLE scanner is not supported as the device api level
 * is less that LOLLIPOP (21).
 *
 * @param titleView                 TextView of the title of the dialog for searching the bluetooth devices
 * @param btSocketSelectorView      TextView used for select the BT socket connection type
 * @param bleSelectorView           TextView used for select the BLE socket connection type.
 *                                  Null if BLE scanner is not supported
 * @param scanBtDevicesButton       Button used for scanning BT devices again
 * @param titleSeparatorView        Simple line separator between the title of the dialog and the content.
 */
data class DevicesListDialogStyle(val titleView: TextView,
                                  val btSocketSelectorView: TextView,
                                  val bleSelectorView: TextView? = null,
                                  val scanBtDevicesButton: ImageButton,
                                  val titleSeparatorView: View) {

    /**
     * Set a color scheme for the [BtDevicesListDialog]. The [primaryColor] will be used for the
     * title text, the title separator and the scan devices button. The [secondaryColor] will be used
     * for the title and the scan button backgrounds.
     * Also if [applyToBtTypeSelector] is true, both colors will be used for the background of the
     * bt type selector views using them in the selected or unselected states.
     */
    @JvmOverloads
    fun setColor(@ColorInt primaryColor: Int, @ColorInt secondaryColor: Int = -1,
                 applyToBtTypeSelector: Boolean = true) {
        titleView.setTextColor(primaryColor)
        scanBtDevicesButton.setColorFilter(primaryColor, PorterDuff.Mode.MULTIPLY)
        titleSeparatorView.setBackgroundColor(primaryColor)

        if (secondaryColor != -1) {
            titleView.setBackgroundColor(secondaryColor)
            scanBtDevicesButton.setBackgroundColor(secondaryColor)

            if (applyToBtTypeSelector) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    btSocketSelectorView.background = getStateListDrawable(primaryColor, secondaryColor)
                    bleSelectorView?.background = getStateListDrawable(primaryColor, secondaryColor)
                } else {
                    btSocketSelectorView.setBackgroundDrawable(getStateListDrawable(primaryColor, secondaryColor))
                    bleSelectorView?.setBackgroundDrawable(getStateListDrawable(primaryColor, secondaryColor))
                }
            }
        }
    }

    /**
     * Set the [backgroundDrawable] param as the background drawable of the BT type selector views.
     */
    fun setBtSelectorBgDrawable(backgroundDrawable: Drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            btSocketSelectorView.background = backgroundDrawable
            bleSelectorView?.background = backgroundDrawable
        } else {
            btSocketSelectorView.setBackgroundDrawable(backgroundDrawable)
            bleSelectorView?.setBackgroundDrawable(backgroundDrawable)
        }
    }

    /**
     * Same method as [setColor] but using resources ids instead the resources themselves.
     */
    @JvmOverloads
    fun setColorRes(context: Context, @ColorRes primaryColorRes: Int, @ColorRes secondaryRes: Int = -1,
                    applyToBtTypeSelector: Boolean = true) {
        setColor(ContextCompat.getColor(context, primaryColorRes),
            if (secondaryRes != 1) ContextCompat.getColor(context, secondaryRes) else -1,
            applyToBtTypeSelector)
    }

    /**
     * Same method as [setBtSelectorBgDrawable] but using resources ids instead the resources themselves.
     */
    fun setButtonsBgDrawableRes(context: Context, @DrawableRes backgroundDrawableRes: Int) {
        setBtSelectorBgDrawable(ContextCompat.getDrawable(context, backgroundDrawableRes))
    }

    /**
     * Get [StateListDrawable] given the `normalColor` and `pressedColor`
     * for dynamic button coloring
     *
     * @param primaryColor The color in pressed state.
     * @param secondaryColor  The color in normal state.
     */
    private fun getStateListDrawable(primaryColor: Int, secondaryColor: Int): StateListDrawable {
        val stateListDrawable = StateListDrawable()
        stateListDrawable.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(primaryColor))
        stateListDrawable.addState(intArrayOf(android.R.attr.state_selected), ColorDrawable(primaryColor))
        stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), ColorDrawable(primaryColor))
        stateListDrawable.addState(intArrayOf(), ColorDrawable(secondaryColor))
        return stateListDrawable
    }
}