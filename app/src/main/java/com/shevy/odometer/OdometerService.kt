package com.shevy.odometer

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat


class OdometerService : Service() {
    private val binder: IBinder = OdometerBinder()
    private var listener: LocationListener? = null
    private var locManager: LocationManager? = null

    inner class OdometerBinder : Binder() {
        val odometer: OdometerService
            get() = this@OdometerService
    }

   override fun onCreate() {
        super.onCreate()
         listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                distanceInMeters += location.distanceTo(lastLocation)
                    .toDouble()
                lastLocation = location
            }

            override fun onProviderDisabled(arg0: String) {}
            override fun onProviderEnabled(arg0: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(arg0: String, arg1: Int, bundle: Bundle) {}
        }
        locManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val provider = locManager!!.getBestProvider(Criteria(), true)
            if (provider != null) {
                locManager!!.requestLocationUpdates(provider, 1000, 1f,
                    listener as LocationListener
                )
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        if (locManager != null && listener != null) {
        if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
            == PackageManager.PERMISSION_GRANTED
        ) {
            locManager!!.removeUpdates(listener!!)
        }
        locManager = null
        listener = null
        }
    }


    val distance: Double
        get() = distanceInMeters / 1609.344

    companion object {
        private var distanceInMeters = 0.0
        private lateinit var lastLocation: Location
        const val PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION
    }
}