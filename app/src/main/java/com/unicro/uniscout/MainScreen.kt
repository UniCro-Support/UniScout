/*
 * Copyright (&#169;) 2025 UniCro, Inc US. All rights reserved. This software is proprietary and may not be copied, modified, or distributed without explicit permission from (&#169;) UniCro, Inc US.
 */

package com.unicro.uniscout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "@string/hello_uniscout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        ScanningButton(
            text = "@string/scan_nfc",
            containerColor = Color(0xFFFFEE58),
            onClick = { navController.navigate("scanner/NFC") }
        )
        ScanningButton(
            text = "@string/scan_bt",
            containerColor = Color(0xFFFFEE58),
            onClick = { navController.navigate("scanner/BT") }
        )
        ScanningButton(
            text = "@string/scan_uwb",
            containerColor = Color(0xFFFFEE58),
            onClick = { navController.navigate("scanner/UWB") }
        )
        ScanningButton(
            text = "@string/scan_ble",
            containerColor = Color(0xFFFFEE58),
            onClick = { navController.navigate("scanner/BLE") }
        )
        ScanningButton(
            text = "@string/scan_all",
            containerColor = Color(0xFFEB0909),
            contentColor = Color(0xFFFFEB3B),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
            onClick = { navController.navigate("scanner/ALL") }
        )
        ScanningButton(
            text = "@string/scan_wifi",
            containerColor = Color(0xFFACB0B1),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            onClick = { navController.navigate("scanner/WIFI") }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoButton(text = "@string/about") { /* Handle About */ }
            InfoButton(text = "@string/support") { /* Handle Support */ }
        }

        Text(
            text = getCopyrightNotice(LocalContext.current) ?: "Error loading copyright",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            softWrap = true
        )
    }
}

@Composable
fun ScanningButton(
    text: String,
    containerColor: Color,
    contentColor: Color = Color.Black,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InfoButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}