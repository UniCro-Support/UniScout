package com.example.unitrack

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.app.PendingIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
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
            MaterialTheme {
                MainScreen(
                    devices = detectedDevices,
                    nfcTags = detectedNFCTags,
                    onStartScan = { startScanning() }
                )
            }
        }

        // Request permissions
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.UWB_RANGING,
                Manifest.permission.COMPANION_DEVICE_PRESENCE,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun startScanning() {
        bluetoothScanner = BluetoothScanner()
        try {
            lifecycleScope.launch {
                bluetoothScanner.devices.collectLatest { devices ->
                    detectedDevices.clear()
                    detectedDevices.addAll(devices.map { it.device.address })
                }
            }
            bluetoothScanner.startScanning(this)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null && nfcAdapter.isEnabled) {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
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
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
        try {
            bluetoothScanner.stopScanning(this)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (NfcAdapter.ACTION_TAG_DISCOVERED == it.action) {
                val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                tag?.let { t ->
                    val tagId = t.id.toHexString()
                    detectedNFCTags.add("NFC Tag: $tagId")
                }
            }
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }
}

@Composable
fun MainScreen(
    devices: List<String>,
    nfcTags: List<String>,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "UniTrack - Device Scanner",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onStartScan) {
            Text("Start Scanning")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Bluetooth Devices: ${devices.size}")
        devices.forEach { device ->
            Text("Device: $device")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("NFC Tags: ${nfcTags.size}")
        nfcTags.forEach { tag ->
            Text(tag)
        }
    }
}