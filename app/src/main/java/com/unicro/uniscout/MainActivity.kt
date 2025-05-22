/*
 * Copyright (c) 2025 UniCro, LLC US. All rights reserved.
 * This software is proprietary and may not be copied, modified,
 * or distributed without explicit permission from UniCro, LLC US.
 */
package com.unicro.uniscout

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.app.PendingIntent
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.unicro.uniscout.ui.theme.UniScoutTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

fun getCopyrightNotice(context: Context): String? {
    return try {
        val packageName = context.packageName
        val appInfo = context.packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
        appInfo.metaData?.getString("com.unicro.uniscout.copyright")
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}

@RequiresApi(Build.VERSION_CODES.S) // API 31 (Android 12)
@SuppressLint("ObsoleteSdkInt")
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            startScanning()
        } else {
            Toast.makeText(this, "All permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    private lateinit var bluetoothScanner: BluetoothScanner
    private val detectedDevices = mutableStateListOf<String>()
    private val detectedNFCTags = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothScanner = BluetoothScanner(application)
        setContent {
            UniScoutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.UWB_RANGING,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                }
            }
        }
    }

    private fun startScanning() {
        try {
            lifecycleScope.launch {
                bluetoothScanner.devices.collect { devices ->
                    detectedDevices.clear()
                    detectedDevices.addAll(devices.map { it.device.address })
                }
            }
            bluetoothScanner.startScanning()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null && nfcAdapter.isEnabled) {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val intentFilters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply { addDataType("*/*") },
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            )
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::bluetoothScanner.isInitialized) {
            try {
                bluetoothScanner.stopScanning()
            } catch (e: SecurityException) {
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_LONG).show()
            }
        }
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
            }
            tag?.let { t ->
                val tagId = t.id.toHexString()
                detectedNFCTags.add("NFC Tag: $tagId")
            }
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController = navController)
        }
        composable(
            "scanner/{scannerType}",
            arguments = listOf(navArgument("scannerType") { type = NavType.StringType })
        ) { backStackEntry ->
            val scannerTypeString = backStackEntry.arguments?.getString("scannerType")
            val scannerType = try {
                scannerTypeString?.let { ScannerType.valueOf(it) }
            } catch (e: IllegalArgumentException) {
                null // Handle invalid or missing enum gracefully
            }

            scannerType?.let {
                ScannerPage(scannerType = it)
            } ?: run {
                Text("Invalid or missing Scanner Type")
            }
        }
    }
}