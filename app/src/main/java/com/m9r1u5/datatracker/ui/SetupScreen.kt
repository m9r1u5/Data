package com.m9r1u5.datatracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.m9r1u5.datatracker.util.Perms

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Scaffold { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Data Tracker", style = MaterialTheme.typography.headlineMedium)
            Text(
                "To show how much data each app uses, Android needs you to switch on Usage access for Data Tracker.",
                style = MaterialTheme.typography.bodyLarge
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Why is this needed?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Android already keeps a record of how much mobile and Wi-Fi data every app uses. " +
                            "Usage access lets Data Tracker read that record. Everything stays on your phone — " +
                            "no ads, no accounts, no analytics."
                    )
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How to switch it on", style = MaterialTheme.typography.titleMedium)
                    Text("1. Tap the button below.")
                    Text("2. Find Data Tracker in the list (on some phones you first tap it).")
                    Text("3. Turn on \"Permit usage access\" or \"Allow usage tracking\".")
                    Text("4. Come back to this app — it opens automatically.")
                }
            }
            Button(onClick = { Perms.openUsageAccessSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Usage access settings")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                OutlinedButton(
                    onClick = { phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Allow phone info (to add both SIMs together)") }
                Text(
                    "On Android 9 and older, Data Tracker needs phone info to read each SIM's mobile data. Newer phones don't need this.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
