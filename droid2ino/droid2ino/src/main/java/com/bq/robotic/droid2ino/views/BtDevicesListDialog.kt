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

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.Animation.INFINITE
import android.widget.*
import com.bq.robotic.droid2ino.R
import com.bq.robotic.droid2ino.communication.BluetoothManager.BtConnectionType
import com.bq.robotic.droid2ino.communication.BtScanner
import com.bq.robotic.droid2ino.communication.ble.BleScanner
import com.bq.robotic.droid2ino.communication.btsocket.BtSocketScanner
import com.bq.robotic.droid2ino.utils.Droid2InoConstants
import com.bq.robotic.droid2ino.utils.LocationUtils
import android.view.animation.RotateAnimation

class BtDevicesListDialog : DialogFragment() {
    private val LOG_TAG = this.javaClass.simpleName

    companion object {
        private const val BT_SCANNER_TYPE_ARG = "btScannerType"
        private const val SHOW_ONE_BT_OPTION_ARG = "showOneBtOption"

        /**
         * Create a new instance for the fragment with an argument.
         */
        @JvmOverloads
        fun newInstance(btScannerType: BtConnectionType, showOneBtOption: Boolean = false): BtDevicesListDialog {
            val args = Bundle()
            args.putSerializable(BT_SCANNER_TYPE_ARG, btScannerType)
            args.putBoolean(SHOW_ONE_BT_OPTION_ARG, showOneBtOption)
            val fragment = BtDevicesListDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var currentBtScanner: BtScanner? = null
    private var showOneBtOption: Boolean = false
    private val isBleScannerSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    private lateinit var pairedDevicesTitleView: TextView
    private lateinit var scannedDevicesTitleView: TextView
    private lateinit var scanDevicesButton: ImageButton
    private val scanBtDevicesButtonAnim by lazy {
        RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,
            0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            repeatCount = INFINITE
            duration = 2000
        }
    }

    private val BT_SOCKET_TAB_POSITION = 0
    private val BLE_TAB_POSITION = 1
    private lateinit var btSelectorTabLayout: TabLayout
    private lateinit var btSocketSelectorTab: TabLayout.Tab
    private var bleSelectorTab: TabLayout.Tab? = null

    private lateinit var pairedDevicesArrayAdapter: ArrayAdapter<String>
    private lateinit var scannedDevicesArrayAdapter: ArrayAdapter<String>
    private val btDevicesFound: ArrayList<BluetoothDevice> by lazy { ArrayList<BluetoothDevice>() }

    // Text to show when there isn't any devices paired to show in the list
    private lateinit var emptyPairedDevicesListItem: TextView
    // Text to show when none device was found when the discovery to show in the list
    private lateinit var emptyScannedDevicesListItem: TextView

    private var isInitialized = false

    /**
     * Styling of the search bluetooth device dialog.
     */
    lateinit var dialogStyle: DevicesListDialogStyle
        private set

    /**
     * Current bluetooth scanner type.
     */
    var btScannerType: BtConnectionType? = null
        private set // The setter is private and has the default implementation

    /**
     * Listener to deliver results of the action of the user in this dialog.
     */
    var listener: DialogListener? = null

    private val scannerListener by lazy {
        object : BtScanner.BtScanListener {
            override fun onDeviceFound(btDevice: BluetoothDevice) {
                // If it's already paired, skip it, because it's been listed already
                if (btDevice.bondState != BluetoothDevice.BOND_BONDED
                    && !btDevicesFound.contains(btDevice)) {
                    btDevicesFound.add(btDevice)
                    scannedDevicesArrayAdapter.add(btDevice.name + Droid2InoConstants.NEW_LINE_CHARACTER + btDevice.address)
                }
            }

            override fun onScanFinished() {
                if (scannedDevicesArrayAdapter.isEmpty)
                    Log.d(LOG_TAG, "BT scanning finished without finding devices")
                else
                    Log.d(LOG_TAG, "BT scanning finished with new devices")

                view?.let {
                    val dialogTitle = view!!.findViewById<View>(R.id.dialog_title) as TextView
                    dialogTitle.setText(R.string.select_device)
                    scanDevicesButton.animation?.cancel()
                }
            }
        }
    }

    // The on-click listener for all devices in the ListViews
    private val onDeviceSelectedListener by lazy {
        AdapterView.OnItemClickListener { _, v, _, _ ->
            currentBtScanner?.stopScan()

            // Get the device MAC address, which is the last 17 chars in the View
            val info = (v as TextView).text.toString()
            val address = info.substring(info.length - 17)
            listener?.onBtDeviceSelected(address)
            dismiss()
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        listener?.onCancel()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // request a window without the title
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.selectable_bt_type_device_list, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            showOneBtOption = it.getBoolean(SHOW_ONE_BT_OPTION_ARG)
            (it.getSerializable(BT_SCANNER_TYPE_ARG) as BtConnectionType?)?.let {
                selectBtScannerType(activity, it)
            }
        }

        view?.let { initCustomView(it) }
    }

    override fun onResume() {
        // Sets the height and the width of the DialogFragment
        setLayoutSize()
        super.onResume()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setLayoutSize()
    }

    private fun setLayoutSize() {
        val currentOrientation = resources.configuration.orientation
        val height = RelativeLayout.LayoutParams.WRAP_CONTENT

        val width = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
                    activity.resources.getDimensionPixelSize(R.dimen.dialog_width_land)
                else
                    activity.resources.getDimensionPixelSize(R.dimen.dialog_width_portrait)

        dialog.window!!.setLayout(width, height)
    }

    private fun initCustomView(contentView: View) {
        btSelectorTabLayout = contentView.findViewById<TabLayout>(R.id.select_bt_type_container)
        with(btSelectorTabLayout) {
            btSocketSelectorTab = getTabAt(BT_SOCKET_TAB_POSITION)!!

            if (!isBleScannerSupported)
                removeTabAt(BLE_TAB_POSITION)
            else
                bleSelectorTab = getTabAt(BLE_TAB_POSITION)!!

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab) {
                        btSocketSelectorTab -> selectBtScannerType(activity, BtConnectionType.BT_SOCKET)
                        bleSelectorTab -> selectBtScannerType(activity, BtConnectionType.BLE)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
            })
        }

        scanDevicesButton = contentView.findViewById<ImageButton>(R.id.scan_devices_btn)
        scanDevicesButton.setOnClickListener {
            obtainBtDevices(contentView)
        }

        pairedDevicesTitleView = contentView.findViewById(R.id.title_paired_devices)
        scannedDevicesTitleView = contentView.findViewById(R.id.title_scanned_devices)

        emptyPairedDevicesListItem = contentView.findViewById(R.id.paired_devices_empty_item)
        emptyScannedDevicesListItem = contentView.findViewById(R.id.scanned_devices_empty_item)

        // Initialize array adapters. One for already paired devices and one for newly discovered devices
        pairedDevicesArrayAdapter = ArrayAdapter(context, R.layout.device_name)
        scannedDevicesArrayAdapter = ArrayAdapter(context, R.layout.device_name)

        // Find and set up the ListView for paired devices
        val pairedListView = contentView.findViewById(R.id.paired_devices) as ListView
        with(pairedListView) {
            adapter = pairedDevicesArrayAdapter
            onItemClickListener = onDeviceSelectedListener
            emptyView = emptyPairedDevicesListItem
        }

        // Find and set up the ListView for newly discovered devices
        val scannedDevicesListView = contentView.findViewById(R.id.scanned_devices) as ListView
        with(scannedDevicesListView) {
            adapter = scannedDevicesArrayAdapter
            onItemClickListener = onDeviceSelectedListener
            emptyView = emptyScannedDevicesListItem
        }
        emptyScannedDevicesListItem.visibility = View.GONE

        // Initialize the object for the styling modifications of the search bluetooth device dialog
        dialogStyle = DevicesListDialogStyle(contentView.findViewById(R.id.dialog_title),
            btSelectorTabLayout,
            btSocketSelectorTab,
            bleSelectorTab,
            scanDevicesButton,
            contentView.findViewById(R.id.title_separator))
        listener?.onDevicesListDialogStyleCreated(dialogStyle)

        isInitialized = true

        if (currentBtScanner == null || btScannerType == null) {
            // In case `selectBtScannerType()` isn't being called yet
            selectBtScannerType(activity, btScannerType ?: BtConnectionType.DEFAULT)
        } else {
            updateBtTypeSelectorViews(btScannerType!!)
            obtainBtDevices(contentView)
        }
    }

    override fun onDestroyView() {
        isInitialized = false
        super.onDestroyView()
        currentBtScanner?.stopScan()
    }

    /**
     * Select a new BT scanner type. If the dialog is already shown, the current scanner stops and
     * the new one is started. If not, it is just configured for when the dialog is shown.
     */
    fun selectBtScannerType(context: Context, connectionType: BtConnectionType) {
        if (btScannerType == connectionType) return
        currentBtScanner?.stopScan()

        btScannerType = when {
            connectionType == BtConnectionType.BLE && !isBleScannerSupported -> {
                Log.e(LOG_TAG, "BLE can not be used with the android version of the current device. " +
                               "BLE scanner can be used only with Build.VERSION_CODES.LOLLIPOP or greater")
                if (currentBtScanner != null && btScannerType != null
                    && btScannerType == BtConnectionType.DEFAULT)
                    return // already configured with the default values

                BtConnectionType.DEFAULT
            }

            else -> connectionType
        }

        Log.d(LOG_TAG, "Selected $btScannerType scanner type")

        currentBtScanner = when (btScannerType!!) {
            BtConnectionType.BT_SOCKET -> BtSocketScanner(context)
            BtConnectionType.BLE -> BleScanner() // Ignore the lint warning. Already checked above
        }

        currentBtScanner?.setBtScanListener(scannerListener)

        listener?.onBtTypeSelectedChanged(connectionType)

        // Start scanning only if the dialog view has been already created. Also the buttons will
        // be selected when it should when the view is created
        view?.let {
            if (isInitialized) {
                updateBtTypeSelectorViews(btScannerType!!)
                obtainBtDevices(it)
            }
        }
    }

    private fun updateBtTypeSelectorViews(connectionType: BtConnectionType) {
        if (!isBleScannerSupported && btSelectorTabLayout.tabCount > 1) {
            btSelectorTabLayout.removeTabAt(BLE_TAB_POSITION)
        }

        when (connectionType) {
            BtConnectionType.BT_SOCKET -> {
                // Hide the other option if only one has to be shown
                if (showOneBtOption) {
                    if (btSelectorTabLayout.tabCount <= 1) {
                        btSelectorTabLayout.removeAllTabs()
                        btSelectorTabLayout.addTab(btSocketSelectorTab, BT_SOCKET_TAB_POSITION)
                    } else
                        btSelectorTabLayout.removeTabAt(BLE_TAB_POSITION)
                }

                if (!btSocketSelectorTab.isSelected) btSocketSelectorTab.select()
            }

            BtConnectionType.BLE -> {
                // Hide the other option if only one has to be shown
                if (showOneBtOption) {
                    if (btSelectorTabLayout.tabCount <= 1) {
                        btSelectorTabLayout.removeAllTabs()
                        bleSelectorTab?.let {
                            btSelectorTabLayout.addTab(it, BLE_TAB_POSITION)
                        }
                    } else
                        btSelectorTabLayout.removeTabAt(BT_SOCKET_TAB_POSITION)
                }

                if (bleSelectorTab?.isSelected == false) bleSelectorTab?.select()
            }
        }
    }

    private fun obtainBtDevices(contentView: View) {
        loadPairedDevices(contentView)
        scanForBtDevices(contentView)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun scanForBtDevices(contentView: View) {
        Log.d(LOG_TAG, "Scanning for BT devices...")

        // Indicate scanning in the title
        val dialogTitle = contentView.findViewById(R.id.dialog_title) as TextView
        dialogTitle.setText(R.string.scanning)

        // Turn on sub-title for scanned devices
        scannedDevicesTitleView.visibility = View.VISIBLE

        if (!LocationUtils.isLocationServicesAvailable(contentView.context)) {
            emptyScannedDevicesListItem.setText(
                if (!LocationUtils.isLocationPermissionGranted(contentView.context))
                    R.string.location_permission_not_granted_error
                else R.string.location_disabled_error)
            resetScannedLists()

        } else if (currentBtScanner?.scanForBtDevices() == true) {
            emptyScannedDevicesListItem.setText(R.string.none_device_found)
            resetScannedLists()
            scanDevicesButton.startAnimation(scanBtDevicesButtonAnim)
        }

    }

    private fun loadPairedDevices(contentView: View) {
        pairedDevicesArrayAdapter.clear()

        // Do not show paired devices for BLE connections
        if (btScannerType == BtConnectionType.BLE) {
            contentView.findViewById<View>(R.id.paired_devices_container).visibility = View.GONE
            return
        }

        contentView.findViewById<View>(R.id.paired_devices_container).visibility = View.VISIBLE

        // If there are paired devices, add each one to the ArrayAdapter
        val pairedDevices = currentBtScanner?.getPairedBtDevices()
        pairedDevices?.let {
            it.forEach {
                pairedDevicesArrayAdapter.add(it.name + Droid2InoConstants.NEW_LINE_CHARACTER +
                                              it.address)
            }
        }
    }

    private fun resetScannedLists() {
        scannedDevicesArrayAdapter.clear()
        btDevicesFound.clear()
    }

    interface DialogListener {
        /**
         * Called when a dialog completes with the address of the BT device which we want to connect.
         *
         * Executed by the thread that initiated the dialog.
         *
         * @param btDeviceAddress Address of the BT device which we want to connect
         */
        fun onBtDeviceSelected(btDeviceAddress: String)

        /**
         * Called when a dialog is canceled by the user.
         *
         * Executed by the thread that initiated the dialog.
         */
        fun onCancel()

        /**
         * The styling class for the lists with the devices has been created.
         * @param deviceListDialogStyle
         */
        fun onDevicesListDialogStyleCreated(deviceListDialogStyle: DevicesListDialogStyle)

        /**
         * Callback invoked when a new bluetooth connection type has been selected in this dialog.
         */
        fun onBtTypeSelectedChanged(selectedBtType: BtConnectionType)
    }

}