package com.bq.robotic.droid2ino.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

/**
 * Location utils class.
 */
object LocationUtils {
    /**
     * Return true if the location services are enabled and the coarse location permission is
     * granted, false otherwise.
     */
    fun isLocationServicesAvailable(context: Context): Boolean {
        var locationMode = 0
        val locationProviders: String
        var isAvailable: Boolean

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }

            isAvailable = locationMode != Settings.Secure.LOCATION_MODE_OFF
        } else {
            locationProviders = Settings.Secure.getString(context.contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
            isAvailable = !TextUtils.isEmpty(locationProviders)
        }

        val coarsePermissionCheck = isLocationPermissionGranted(context)

        return isAvailable && coarsePermissionCheck
    }

    /**
     * Return true if the coarse location permission is granted, false if not.
     */
    fun isLocationPermissionGranted(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}