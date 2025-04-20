package com.example.unitrack

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothScanner {

    private val bluetoothAdapter: BluetoothAdapter? = ContextCompat.getSystemService(
        ContextCompat.getSystemServiceName(Context.BLUETOOTH_SERVICE)!!
    ) as BluetoothAdapter?
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val _devices = MutableStateFlow<List<ScanResult>>(emptyList())
    val devices: StateFlow<List<ScanResult>> = _devices

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

    fun startScanning(context: Context) {
        val hasScanPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasConnectPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasScanPermission && hasConnectPermission && bluetoothAdapter?.isEnabled == true) {
            scanner?.startScan(scanCallback)
        } else {
            throw SecurityException("Bluetooth permissions not granted or Bluetooth is disabled")
        }
    }

    fun stopScanning(context: Context) {
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
        return manufacturerData?.get(0x004C) != null // AirTags use Apple’s manufacturer ID (0x004C)
    }
}