/*
 * Copyright (c) 2025 UniCro, Inc US. All rights reserved.
 * This software is proprietary and may not be copied, modified,
 * or distributed without explicit permission from UniCro, Inc US.
 */
package com.unicro.uniscout

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@RequiresApi(Build.VERSION_CODES.S) // API 31 (Android 12)
@SuppressLint("ObsoleteSdkInt", "UNCHECKED_CAST", "ServiceCast") // Suppress both warnings for the entire class
class BluetoothScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothManager: BluetoothAdapter? = BluetoothManager.adapter()

    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val _devices = MutableStateFlow<List<ScanResult>>(emptyList())
    val devices: StateFlow<List<ScanResult>> = _devices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val currentDevices = _devices.value.toMutableList()
                if (!currentDevices.contains(it)) {
                    currentDevices.add(it)
                    _devices.value = currentDevices
                    if (isPotentialTracker(it)) {
                        println("Potential tracker detected: ${it.device.address}")
                    }
                }
            }
        }
    }

    fun startScanning() {
        val hasScanPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasConnectPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasScanPermission && hasConnectPermission && hasLocationPermission && bluetoothAdapter?.isEnabled == true) {
            scanner?.startScan(scanCallback)
        } else {
            throw SecurityException("Bluetooth permissions not granted, location permission missing, or Bluetooth is disabled")
        }
    }

    fun stopScanning() {
        val hasScanPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasScanPermission) {
            scanner?.stopScan(scanCallback)
        } else {
            throw SecurityException("Bluetooth permissions not granted")
        }
    }

    private fun isPotentialTracker(result: ScanResult): Boolean {
        val manufacturerData = result.scanRecord?.manufacturerSpecificData
        return manufacturerData?.get(0x004C) != null
    }
}