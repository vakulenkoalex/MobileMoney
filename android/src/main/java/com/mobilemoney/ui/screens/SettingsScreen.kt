package com.mobilemoney.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.di.DI
import com.mobilemoney.service.NotificationReceiverService
import com.mobilemoney.ui.common.PermissionChecker
import com.mobilemoney.viewmodel.SettingsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
    val hasSms = PermissionChecker.hasSmsPermission(context)
    val hasNotif = PermissionChecker.hasNotificationPermission(context)
    val hasListener = PermissionChecker.hasNotificationListenerAccess(context)
    var smsEnabled by remember { mutableStateOf(
        featurePrefs.smsEnabled && hasSms && hasNotif
    ) }
    var debugMode by remember { mutableStateOf(
        featurePrefs.debugModeEnabled
    ) }
    var pushEnabled by remember { mutableStateOf(
        featurePrefs.pushEnabled && hasListener && hasNotif
    ) }
    var messageProcessingEnabled by remember { mutableStateOf(
        featurePrefs.messageProcessingEnabled
    ) }
    var dataMenuExpanded by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            GlobalScope.launch {
                com.mobilemoney.ui.common.ErrorHandler.emitError("Уведомления запрещены. Разрешите в настройках, чтобы получать оповещения об обработке")
            }
        }
    }

    fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            featurePrefs.smsEnabled = true
            smsEnabled = true
            checkNotificationPermission()
        } else {
            featurePrefs.smsEnabled = false
            smsEnabled = false
        }
    }

    LaunchedEffect(Unit) {
        if (!PermissionChecker.hasNotificationPermission(context)) {
            featurePrefs.smsEnabled = false
            smsEnabled = false
            featurePrefs.pushEnabled = false
            pushEnabled = false
        }
        if (!PermissionChecker.hasSmsPermission(context)) {
            featurePrefs.smsEnabled = false
            smsEnabled = false
        }
        if (!PermissionChecker.hasNotificationListenerAccess(context)) {
            featurePrefs.pushEnabled = false
            pushEnabled = false
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Чтение PUSH-уведомлений")
                Switch(
                    checked = pushEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val component = ComponentName(context, NotificationReceiverService::class.java)
                            val listeners = Settings.Secure.getString(
                                context.contentResolver,
                                "enabled_notification_listeners"
                            )
                            if (listeners == null || !listeners.contains(component.flattenToString())) {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } else {
                                featurePrefs.pushEnabled = true
                                pushEnabled = true
                                checkNotificationPermission()
                            }
                        } else {
                            featurePrefs.pushEnabled = false
                            pushEnabled = false
                        }
                    }
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Автосоздание по сообщениям")
                Switch(
                    checked = messageProcessingEnabled,
                    onCheckedChange = {
                        messageProcessingEnabled = it
                        featurePrefs.messageProcessingEnabled = it
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
                Text("Сообщения")
            }

            Button(
                onClick = onNavigateToSenders,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отправители")
            }

            Box {
                Button(
                    onClick = { dataMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Управление данными")
                }
                DropdownMenu(
                    expanded = dataMenuExpanded,
                    onDismissRequest = { dataMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Экспорт базы данных") },
                        onClick = {
                            dataMenuExpanded = false
                            exportLauncher.launch(viewModel.getDefaultFileName())
                        },
                        enabled = !uiState.isLoading
                    )
                    DropdownMenuItem(
                        text = { Text("Импорт базы данных") },
                        onClick = {
                            dataMenuExpanded = false
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        enabled = !uiState.isLoading
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить удалённые записи") },
                        onClick = {
                            dataMenuExpanded = false
                            viewModel.deleteAll()
                        },
                        enabled = !uiState.isLoading
                    )
                }
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