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

import android.widget.ImageButton
import android.widget.TextView
import android.view.View
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.tabs.TabLayout

/**
 * Utils class for style the components of the scanned bluetooth devices dialog [BtDevicesListDialog].
 * @param bleSelectorTab will be null if the BLE scanner is not supported as the device api level
 * is less that LOLLIPOP (21).
 *
 * @param titleView                TextView of the title of the dialog for searching the bluetooth devices
 * @param btSelectorTabLayout      TabLayout container for the tabs with the BT options
 * @param btSocketSelectorTab      Tab used for select the BT socket connection type
 * @param bleSelectorTab           Tab used for select the BLE socket connection type.
 *                                 Null if BLE scanner is not supported
 * @param scanBtDevicesButton      Button used for scanning BT devices again
 * @param titleSeparatorView       Simple line separator between the title of the dialog and the content.
 */
data class DevicesListDialogStyle(val titleView: TextView,
                                  val btSelectorTabLayout: TabLayout,
                                  val btSocketSelectorTab: TabLayout.Tab,
                                  val bleSelectorTab: TabLayout.Tab? = null,
                                  val scanBtDevicesButton: ImageButton,
                                  val titleSeparatorView: View) {

    /**
     * Set a color scheme for the [BtDevicesListDialog]. The [primaryColor] will be used
     * for the title and the scan button backgrounds. The [secondaryColor] will be used for the
     * title text, the title separator and the scan devices button.
     * Also if [applyToBtTypeSelector] is true, both colors will be used for the background of the
     * bt type selector views using them in the selected or unselected states.
     */
    @JvmOverloads
    fun setColor(@ColorInt primaryColor: Int, @ColorInt secondaryColor: Int = -1,
                 applyToBtTypeSelector: Boolean = true) {
        (titleView.parent as? View)?.setBackgroundColor(primaryColor)

        if (secondaryColor != -1) {
            titleView.setTextColor(secondaryColor)
            DrawableCompat.setTint(scanBtDevicesButton.drawable.mutate(), secondaryColor)
            titleSeparatorView.setBackgroundColor(secondaryColor)
        }

        if (applyToBtTypeSelector) {
            btSelectorTabLayout.setSelectedTabIndicatorColor(primaryColor)
        }
    }

    /**
     * Set the [iconDrawable] param as the background drawable of the BT type selector views.
     */
    fun setBtSelectorTabIcon(iconDrawable: Drawable) {
            btSocketSelectorTab.icon = iconDrawable
            bleSelectorTab?.icon = iconDrawable
    }

    /**
     * Same method as [setColor] but using resources ids instead the resources themselves.
     */
    @JvmOverloads
    fun setColorRes(context: Context, @ColorRes primaryColorRes: Int, @ColorRes secondaryRes: Int = -1,
                    applyToBtTypeSelector: Boolean = true) {
        setColor(ContextCompat.getColor(context, primaryColorRes),
            if (secondaryRes != -1) ContextCompat.getColor(context, secondaryRes) else -1,
            applyToBtTypeSelector)
    }

    /**
     * Same method as [setBtSelectorTabIcon] but using resources ids instead the resources themselves.
     */
    fun setBtSelectorTabIconRes(context: Context, @DrawableRes backgroundDrawableRes: Int) {
        setBtSelectorTabIcon(ContextCompat.getDrawable(context, backgroundDrawableRes)!!)
    }

    /**
     * Get [StateListDrawable] given the `normalColor` and `pressedColor`
     * for dynamic button coloring
     *
     * @param primaryColor The color in pressed state.
     * @param secondaryColor  The color in normal state.
     */
    fun getStateListDrawable(primaryColor: Int, secondaryColor: Int): StateListDrawable {
        val stateListDrawable = StateListDrawable()
        stateListDrawable.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(primaryColor))
        stateListDrawable.addState(intArrayOf(android.R.attr.state_selected), ColorDrawable(primaryColor))
        stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), ColorDrawable(primaryColor))
        stateListDrawable.addState(intArrayOf(), ColorDrawable(secondaryColor))
        return stateListDrawable
    }
}