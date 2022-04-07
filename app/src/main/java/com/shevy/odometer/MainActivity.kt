package com.shevy.odometer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var odometer: OdometerService
    private var bound = false
    private val PERMISSION_REQUEST_CODE = 698
    private val NOTIFICATION_ID = 423

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            val odometerBinder: OdometerService.OdometerBinder = binder as OdometerService.OdometerBinder
            odometer = odometerBinder.odometer
            bound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        displayDistance()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        vararg permissions: String?, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = Intent(this, OdometerService::class.java)
                    bindService(intent, connection, BIND_AUTO_CREATE)
                } else {
                    val builder = NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.ic_menu_compass)
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setContentText(resources.getString(R.string.permission_denied))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(longArrayOf(1000, 1000))
                        .setAutoCancel(true)
                    val actionIntent = Intent(this, MainActivity::class.java)
                    val actionPendingIntent = PendingIntent.getActivity(
                        this, 0,
                        actionIntent, PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.setContentIntent(actionPendingIntent)

                    val notificationManager =
                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                this,
                OdometerService.PERMISSION_STRING
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(OdometerService.PERMISSION_STRING),
                PERMISSION_REQUEST_CODE
            )
        } else {
            val intent = Intent(this, OdometerService::class.java)
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    private fun displayDistance() {
        val distanceView = findViewById<View>(R.id.distance) as TextView
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                var distance = 0.0
                if (bound && odometer != null) {
                    distance = odometer.distance
                }
                val distanceStr = String.format(
                    Locale.getDefault(),
                    "%1$,.3f miles", distance
                )
                distanceView.text = distanceStr
                handler.postDelayed(this, 1000)
            }
        })
    }
}