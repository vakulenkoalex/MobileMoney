package com.mobilemoney.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.di.DI
import com.mobilemoney.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToRegexes: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToSenders: () -> Unit = {},
    viewModel: SettingsViewModel = DI.settingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.export(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.import(it) }
    }

    val context = LocalContext.current
    val featurePrefs = remember { FeaturePreferences(DI.context) }
    var clipboardEnabled by remember { mutableStateOf(
        featurePrefs.clipboardParsingEnabled
    ) }
    var smsEnabled by remember { mutableStateOf(
        featurePrefs.smsEnabled
    ) }
    var debugMode by remember { mutableStateOf(
        featurePrefs.debugModeEnabled
    ) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            featurePrefs.smsEnabled = true
            smsEnabled = true
        } else {
            featurePrefs.smsEnabled = false
            smsEnabled = false
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", style = MaterialTheme.typography.titleSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Копирование из буфера обмена")
                Switch(
                    checked = clipboardEnabled,
                    onCheckedChange = {
                        clipboardEnabled = it
                        featurePrefs.clipboardParsingEnabled = it
                    }
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Чтение SMS")
                Switch(
                    checked = smsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val hasSmsPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECEIVE_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasSmsPermission) {
                                featurePrefs.smsEnabled = true
                                smsEnabled = true
                                checkNotificationPermission()
                            } else {
                                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            }
                        } else {
                            featurePrefs.smsEnabled = false
                            smsEnabled = false
                        }
                    }
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Режим отладки")
                Switch(
                    checked = debugMode,
                    onCheckedChange = {
                        debugMode = it
                        featurePrefs.debugModeEnabled = it
                    }
                )
            }

            Button(
                onClick = onNavigateToCategories,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Категории")
            }

            Button(
                onClick = onNavigateToRegexes,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Управление регулярками")
            }

            Button(
                onClick = onNavigateToMessages,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сообщения SMS")
            }

            Button(
                onClick = onNavigateToSenders,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отправители SMS")
            }

            Button(
                onClick = { exportLauncher.launch(viewModel.getDefaultFileName()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Экспорт базы данных")
            }

            Button(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Импорт базы данных")
            }

            Button(
                onClick = { viewModel.deleteAll() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить удалённые записи")
            }

            if (uiState.isSyncing) {
                Text(
                    text = "Синхронизация...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = { viewModel.sync() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Синхронизация")
                }
            }
        }
    }
}