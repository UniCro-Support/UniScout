/*
 * Copyright (c) 2025 UniCro, Inc US. All rights reserved.
 * This software is proprietary and may not be copied, modified,
 * or distributed without explicit permission from UniCro, Inc US.
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.unicro.uniscout.ui.theme.UniScoutTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.pm.PackageManager

fun getCopyrightNotice(context: Context): String? {
    return try {
        val packageName = context.packageName
        val appInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        appInfo.metaData?.getString("com.unicro.uniscout.copyright")
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}

@RequiresApi(Build.VERSION_CODES.S) // API 31 (Android 12)
@SuppressLint("ObsoleteSdkInt") // Suppress warning for the entire class
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
        setContent {
            UniScoutTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    try {
                        AppNavigation()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Navigation Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }
                val bluetoothScanner = BluetoothScanner(this) // Request permissions
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

    private fun startScanning() {
        bluetoothScanner = BluetoothScanner(this) // Initialize BluetoothScanner
        try {
            lifecycleScope.launch {
                bluetoothScanner.devices.collectLatest { devices ->
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
            bluetoothScanner.stopScanning()
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
        try {
            bluetoothScanner.stopScanning()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_LONG).show()
        }
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
                // Proceed if scannerType is valid
                ScannerPage(scannerType = it)
            } ?: run {
                // Show an error screen or handle invalid input here
                Text("Invalid or missing Scanner Type")
            }
        }

    }
}