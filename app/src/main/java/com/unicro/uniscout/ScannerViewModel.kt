/*
 * Copyright (c) 2025 UniScout by UniCro, LLC US. All rights reserved. This software is proprietary and may not be copied, modified, or distributed without explicit permission from (c) UniCro, LLC US.
 */

package com.unicro.uniscout

import android.app.Application
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScannerViewModel(private val scannerType: ScannerType, private val application: Application) : ViewModel() {
    val progress = mutableFloatStateOf(0f)
    val deviceCount = mutableIntStateOf(0)
    val devices = mutableStateListOf<Device>()
    private var activeScanner: Any? = null

    fun startScanning() {
        viewModelScope.launch {
            try {
                when (scannerType) {
                    ScannerType.BT, ScannerType.BLE -> {
                        val bluetoothScanner = BluetoothScanner(application)
                        activeScanner = bluetoothScanner
                        bluetoothScanner.startScanning()
                        bluetoothScanner.devices.collect { scanResults ->
                            devices.clear()
                            devices.addAll(scanResults.map { result ->
                                Device(
                                    name = result.device.name ?: "Unknown",
                                    address = result.device.address,
                                    signalStrength = result.rssi,
                                    type = scannerType,
                                    manufacturerData = result.scanRecord?.manufacturerSpecificData?.toString()
                                )
                            })
                            deviceCount.intValue = devices.size
                            progress.floatValue =
                                if (devices.isNotEmpty()) 1f else progress.floatValue
                        }
                    }

                    ScannerType.NFC -> {
                        // TODO: Implement NFC scanning
                        // 1. Use NfcAdapter to detect tags (typically handled in Activity)
                        // 2. Pass detected tags to ViewModel via a callback or flow
                        // 3. Update devices list with NFC tag data (e.g., tag ID)
                    }

                    ScannerType.UWB -> {
                        val uwbScanner = UwbScanner(application)
                        activeScanner = uwbScanner
                        uwbScanner.startScanning()
                        // TODO: Collect UWB devices from uwbScanner.trackers when implemented
                        // For now, using placeholder data
                        devices.addAll(uwbScanner.trackers.value.map {
                            Device(
                                it.address,
                                null,
                                null,
                                scannerType,
                                "Distance: ${it.distance}m, Angle: ${it.angle}°"
                            )
                        })
                        deviceCount.intValue = devices.size
                        progress.floatValue = 1f
                    }

                    ScannerType.WIFI -> {
                        // TODO: Implement WiFi scanning
                        // 1. Use WifiManager to startScan()
                        // 2. Register a BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION
                        // 3. Collect ScanResult objects and map to Device data class
                    }

                    ScannerType.ALL -> {
                        // TODO: Implement combined scanning
                        // 1. Initialize all scanners (Bluetooth, NFC, UWB, WiFi)
                        // 2. Collect devices from all sources into a single list
                        // 3. Update progress based on combined scan completion
                    }
                }
            } catch (_: SecurityException) {
                progress.floatValue = 0f
                // Optionally, notify UI of permission issues via a state update
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        when (activeScanner) {
            is BluetoothScanner -> (activeScanner as BluetoothScanner).stopScanning()
            is UwbScanner -> (activeScanner as UwbScanner).stopScanning()
            // Add other scanner types as implemented
        }
    }
}

class ScannerViewModelFactory(private val scannerType: ScannerType, private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            return ScannerViewModel(scannerType, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class Device(
    val name: String,
    val address: String? = null, // e.g., MAC address for Bluetooth
    val signalStrength: Int? = null, // RSSI in dBm
    val type: ScannerType, // Type of device
    val manufacturerData: String? = null // Additional data
)