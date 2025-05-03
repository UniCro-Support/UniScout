/*
 * Copyright (c) 2025 UniScout by UniCro, Inc US. All rights reserved. This software is proprietary and may not be copied, modified, or distributed without explicit permission from (c) UniCro, Inc US.
 */

package com.unicro.uniscout

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScannerViewModel(private val scannerType: ScannerType) : ViewModel() {
    val progress = mutableStateOf(0f)
    val deviceCount = mutableStateOf(0)
    val devices = mutableStateListOf<Device>()

    fun startScanning() {
        viewModelScope.launch {
            for (i in 1..100) {
                delay(50)
                progress.value = i / 100f
                if (i == 100) {
                    repeat(5) { index ->
                        devices.add(Device("$scannerType Device ${index + 1}"))
                        deviceCount.value += 1
                    }
                }
            }
        }
    }
}

class ScannerViewModelFactory(private val scannerType: ScannerType) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            return ScannerViewModel(scannerType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class Device(val name: String)