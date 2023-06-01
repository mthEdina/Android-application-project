package com.android.example.searchwifi

import android.Manifest
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var scanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.scanBtn)
        scanButton.setOnClickListener {
            checkPermissionAndScanWifi()
        }
    }

    private fun checkPermissionAndScanWifi() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted. Request it from the user.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
            scanWifiNetworks()
        } else {
            // Permission is already granted. Start scanning Wi-Fi networks.
            scanWifiNetworks()
        }
    }

    private fun scanWifiNetworks() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.startScan()
            val scanResults = wifiManager.scanResults
            displayWifiNetworks(scanResults)
        } catch (e: SecurityException) {
            // Handle the exception gracefully
            Toast.makeText(this, "Permission denied to access WiFi information.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayWifiNetworks(scanResults: List<ScanResult>) {
        val filteredWifiList = scanResults.filter { it.SSID.contains("DIGI") }.map { it.SSID }
        val wifiArray = filteredWifiList.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Available 'DIGI' WiFi networks")
        if (wifiArray.isNotEmpty()) {
            builder.setItems(wifiArray) { dialog, which ->
                connectToWifi(wifiArray[which])
            }
        } else {
            builder.setMessage("There is no 'DIGI' WiFi network in the area.")
        }
        builder.setPositiveButton("Ok") { dialog, which ->
            // Handle "OK" button click if necessary
        }

        builder.show()
    }

    private fun connectToWifi(ssid: String) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CHANGE_WIFI_STATE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CHANGE_WIFI_STATE),
                PERMISSION_WIFI_REQUEST_CODE
            )
            return
        }
        val config = WifiConfiguration()
        config.SSID = "\"$ssid\""


        val networkId = wifiManager.addNetwork(config)
        if (networkId != -1) {
            wifiManager.disconnect()
            wifiManager.enableNetwork(networkId, true)
            wifiManager.reconnect()
            Toast.makeText(this, "Connect to '$ssid'...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error in connecting.", Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val PERMISSION_WIFI_REQUEST_CODE = 2
    }
}
 