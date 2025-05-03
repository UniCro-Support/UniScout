/*
 * Copyright (c) 2025 UniScout by UniCro, Inc US. All rights reserved. This software is proprietary and may not be copied, modified, or distributed without explicit permission from (c) UniCro, Inc US.
 */

package com.unicro.uniscout

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ScannerPage(scannerType: ScannerType) {
    val viewModel: ScannerViewModel = viewModel(factory = ScannerViewModelFactory(scannerType))

    LaunchedEffect(key1 = Unit) {
        viewModel.startScanning()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${scannerType.name} Scanner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { viewModel.progress.value },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Devices found: ${viewModel.deviceCount.value}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(viewModel.devices) { device ->
                Text(
                    text = device.name,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        CopyrightNotice()
    }
}

@Composable
fun CopyrightNotice() {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val copyrightText = try {
        val appInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        appInfo.applicationInfo?.metaData?.getString("com.unicro.uniscout.copyright") ?: "Copyright not found"
    } catch (_: PackageManager.NameNotFoundException) {
        "Copyright not found"
    }

    Text(
        text = copyrightText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp)
    )
}