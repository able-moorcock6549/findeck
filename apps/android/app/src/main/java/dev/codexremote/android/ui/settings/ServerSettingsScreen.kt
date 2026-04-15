package dev.codexremote.android.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.codexremote.android.R
import dev.codexremote.android.data.model.Server
import dev.codexremote.android.data.network.ApiClient
import dev.codexremote.android.data.repository.ServerRepository
import dev.codexremote.android.ui.sessions.RuntimeControlSheetContent
import dev.codexremote.android.ui.sessions.RuntimeControlTarget
import dev.codexremote.android.ui.sessions.runtimeControlLabel
import dev.codexremote.android.ui.sessions.ShimmerBlock
import dev.codexremote.android.ui.sessions.TimelineNoticeCard
import dev.codexremote.android.ui.sessions.TimelineNoticeTone
import dev.codexremote.android.ui.theme.PrecisionConsoleSnackbarHost
import dev.codexremote.android.ui.theme.ThemePreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ServerSettingsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val server: Server? = null,
    val runtimeDefaultModel: String? = null,
    val runtimeDefaultReasoningEffort: String? = null,
    val error: String? = null,
)

class ServerSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ServerRepository(application)

    private val _uiState = MutableStateFlow(ServerSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private fun userFacingMessage(error: Throwable, fallback: String): String {
        val resolved = ApiClient.describeNetworkFailure(error)
        return if (resolved.isBlank() || resolved == "连接失败，请稍后重试") fallback else resolved
    }

    fun load(serverId: String) {
        viewModelScope.launch {
            _uiState.value = ServerSettingsUiState(loading = true)
            try {
                val servers = repo.servers.first()
                val server = servers.find { it.id == serverId }
                    ?: throw IllegalStateException(
                        getApplication<Application>().getString(R.string.server_settings_error_server_missing)
                    )
                _uiState.value = ServerSettingsUiState(
                    loading = false,
                    server = server,
                    runtimeDefaultModel = repo.getRuntimeDefaultModel(serverId),
                    runtimeDefaultReasoningEffort = repo.getRuntimeDefaultReasoningEffort(serverId),
                )
            } catch (error: Exception) {
                _uiState.value = ServerSettingsUiState(
                    loading = false,
                    error = userFacingMessage(
                        error,
                        getApplication<Application>().getString(R.string.server_settings_error_load_failed),
                    ),
                )
            }
        }
    }

    fun updateRuntimeDefaults(
        serverId: String,
        model: String?,
        reasoningEffort: String?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val server = _uiState.value.server ?: run {
                onError(getApplication<Application>().getString(R.string.server_settings_error_server_missing))
                return@launch
            }
            try {
                repo.setRuntimeDefaultModel(serverId, model)
                repo.setRuntimeDefaultReasoningEffort(serverId, reasoningEffort)
                _uiState.value = _uiState.value.copy(
                    runtimeDefaultModel = model,
                    runtimeDefaultReasoningEffort = reasoningEffort,
                    server = server,
                )
                onSuccess(getApplication<Application>().getString(R.string.server_settings_runtime_saved))
            } catch (error: Exception) {
                onError(
                    userFacingMessage(
                        error,
                        getApplication<Application>().getString(R.string.server_settings_error_update_failed),
                    ),
                )
            }
        }
    }

    fun resetRuntimeDefaults(
        serverId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        updateRuntimeDefaults(
            serverId = serverId,
            model = null,
            reasoningEffort = null,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    fun updateTrustedReconnect(
        serverId: String,
        enabled: Boolean,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val server = _uiState.value.server ?: run {
                onError(getApplication<Application>().getString(R.string.server_settings_error_server_missing))
                return@launch
            }
            try {
                repo.setTrustedAutoReconnectEnabled(serverId, enabled)
                load(serverId)
                onSuccess(
                    if (enabled) {
                        getApplication<Application>().getString(R.string.server_settings_trust_enabled)
                    } else {
                        getApplication<Application>().getString(R.string.server_settings_trust_disabled)
                    }
                )
            } catch (error: Exception) {
                val message = userFacingMessage(
                    error,
                    getApplication<Application>().getString(R.string.server_settings_error_update_failed),
                )
                onError(message)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    saving = false,
                    server = server,
                    error = message,
                )
            }
        }
    }

    fun clearTrustedHost(
        serverId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val server = _uiState.value.server ?: run {
                onError(getApplication<Application>().getString(R.string.server_settings_error_server_missing))
                return@launch
            }
            try {
                repo.clearTrustedHostMetadata(serverId)
                load(serverId)
                onSuccess(getApplication<Application>().getString(R.string.server_settings_trust_cleared))
            } catch (error: Exception) {
                val message = userFacingMessage(
                    error,
                    getApplication<Application>().getString(R.string.server_settings_error_update_failed),
                )
                onError(message)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    saving = false,
                    server = server,
                    error = message,
                )
            }
        }
    }

    fun changePassword(
        serverId: String,
        currentPassword: String,
        newPassword: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val snapshot = _uiState.value
            val server = snapshot.server ?: run {
                onError(getApplication<Application>().getString(R.string.server_settings_error_server_missing))
                return@launch
            }
            val token = server.token ?: run {
                onError(getApplication<Application>().getString(R.string.server_settings_error_not_logged_in))
                return@launch
            }

            _uiState.value = snapshot.copy(saving = true, error = null)
            try {
                val client = ApiClient(server.baseUrl)
                try {
                    val health = client.validateConnection()
                    if (!health.ok) {
                        throw IllegalStateException(
                            buildString {
                                append(health.summary)
                                health.detail?.let {
                                    append('\n')
                                    append(it)
                                }
                            }
                        )
                    }
                    client.changePassword(
                        token = token,
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                    )
                } finally {
                    client.close()
                }

                var refreshedToken: String? = null
                var reloginError: Throwable? = null
                repeat(8) { attempt ->
                    delay(if (attempt == 0) 1_500L else 1_000L)
                    try {
                        val loginClient = ApiClient(server.baseUrl)
                        try {
                            val response = loginClient.login(newPassword, deviceLabel = "android")
                            refreshedToken = response.token
                        } finally {
                            loginClient.close()
                        }
                        return@repeat
                    } catch (error: Exception) {
                        reloginError = error
                    }
                }

                if (refreshedToken != null) {
                    repo.updateCredentials(
                        serverId = serverId,
                        token = refreshedToken,
                        appPassword = newPassword,
                    )
                    load(serverId)
                    onSuccess(
                        getApplication<Application>().getString(
                            R.string.server_settings_password_updated_reconnected
                        )
                    )
                } else {
                    repo.updateCredentials(
                        serverId = serverId,
                        token = server.token,
                        appPassword = newPassword,
                    )
                    load(serverId)
                    onSuccess(
                        userFacingMessage(
                            reloginError ?: IllegalStateException(
                                getApplication<Application>().getString(
                                    R.string.server_settings_password_updated_restarting,
                                )
                            ),
                            getApplication<Application>().getString(
                                R.string.server_settings_password_updated_restarting,
                            ),
                        )
                    )
                }
            } catch (error: Exception) {
                val message = userFacingMessage(
                    error,
                    getApplication<Application>().getString(R.string.server_settings_error_update_failed),
                )
                _uiState.value = snapshot.copy(
                    loading = false,
                    saving = false,
                    error = message,
                )
                onError(message)
                return@launch
            }

            _uiState.update { it.copy(saving = false, error = null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    serverId: String,
    themePreference: ThemePreference,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onOpenPairing: () -> Unit,
    viewModel: ServerSettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var runtimeSheetTarget by remember { mutableStateOf<RuntimeControlTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(serverId) {
        viewModel.load(serverId)
    }

    LaunchedEffect(uiState.server?.appPassword) {
        if (currentPassword.isBlank() && !uiState.server?.appPassword.isNullOrBlank()) {
            currentPassword = uiState.server?.appPassword.orEmpty()
        }
    }

    val screenTitle = stringResource(R.string.server_settings_title)
    val backDescription = stringResource(R.string.content_desc_back)
    val loadingTitle = stringResource(R.string.server_settings_loading_title)
    val loadingMessage = stringResource(R.string.server_settings_loading_message)
    val loadingFooter = stringResource(R.string.server_settings_loading_footer)
    val loadingStateLabel = stringResource(R.string.server_settings_loading_state_label)
    val errorTitle = stringResource(R.string.server_settings_error_title)
    val errorFooter = stringResource(R.string.server_settings_error_footer)
    val errorStateLabel = stringResource(R.string.server_settings_error_state_label)
    val retryButtonLabel = stringResource(R.string.server_settings_retry_button)
    val description = stringResource(R.string.server_settings_description)
    val currentPasswordLabel = stringResource(R.string.server_settings_current_password_label)
    val newPasswordLabel = stringResource(R.string.server_settings_new_password_label)
    val confirmPasswordLabel = stringResource(R.string.server_settings_confirm_password_label)
    val validationCurrentPasswordRequired = stringResource(R.string.server_settings_validation_current_password_required)
    val validationNewPasswordRequired = stringResource(R.string.server_settings_validation_new_password_required)
    val validationConfirmPasswordRequired = stringResource(R.string.server_settings_validation_confirm_password_required)
    val validationPasswordMismatch = stringResource(R.string.server_settings_validation_password_mismatch)
    val validationSamePassword = stringResource(R.string.server_settings_validation_same_password)
    val saveButtonLabel = stringResource(R.string.server_settings_save_button)
    val trustTitle = stringResource(R.string.server_settings_trust_title)
    val trustMessage = stringResource(R.string.server_settings_trust_message)
    val trustBadgeTrusted = stringResource(R.string.server_settings_trust_badge_trusted)
    val trustBadgeUnpaired = stringResource(R.string.server_settings_trust_badge_unpaired)
    val trustAutoReconnect = stringResource(R.string.server_settings_trust_auto_reconnect)
    val trustClear = stringResource(R.string.server_settings_trust_clear)
    val appearanceTitle = stringResource(R.string.server_settings_appearance_title)
    val appearanceMessage = stringResource(R.string.server_settings_appearance_message)
    val notificationsTitle = stringResource(R.string.server_settings_notifications_title)
    val notificationsEnabled = stringResource(R.string.server_settings_notifications_enabled)
    val notificationsDisabled = stringResource(R.string.server_settings_notifications_disabled)
    val notificationsManage = stringResource(R.string.server_settings_notifications_manage)
    val runtimeTitle = stringResource(R.string.server_settings_runtime_title)
    val runtimeMessage = stringResource(R.string.server_settings_runtime_message)
    val runtimeReset = stringResource(R.string.server_settings_runtime_reset)
    val passwordTitle = stringResource(R.string.server_settings_password_title)
    val passwordMessage = stringResource(R.string.server_settings_password_message)
    val trustLabelTitle = stringResource(R.string.server_settings_trust_label_title)
    val trustMethodTitle = stringResource(R.string.server_settings_trust_method_title)
    val trustPairedAtTitle = stringResource(R.string.server_settings_trust_paired_at_title)
    val trustLastReconnectTitle = stringResource(R.string.server_settings_trust_last_reconnect_title)

    val notificationsHelper = remember { dev.codexremote.android.notifications.RunCompletedNotificationHelper(context) }
    var canPostNotifications by remember { mutableStateOf(notificationsHelper.canPostNotifications()) }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, notificationsHelper) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                canPostNotifications = notificationsHelper.canPostNotifications()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDescription)
                    }
                }
            )
        },
        snackbarHost = { PrecisionConsoleSnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.loading -> {
                    TimelineNoticeCard(
                        title = loadingTitle,
                        message = loadingMessage,
                        footer = loadingFooter,
                        tone = TimelineNoticeTone.Neutral,
                        stateLabel = loadingStateLabel,
                        content = {
                            ShimmerBlock(lines = 2)
                        },
                    )
                }

                uiState.server == null -> {
                    TimelineNoticeCard(
                        title = errorTitle,
                        message = uiState.error ?: stringResource(R.string.server_settings_current_server_unavailable),
                        footer = errorFooter,
                        tone = TimelineNoticeTone.Error,
                        stateLabel = errorStateLabel,
                        content = {
                            Button(
                                onClick = { viewModel.load(serverId) },
                            ) {
                                Text(retryButtonLabel)
                            }
                        },
                    )
                }

                else -> {
                    val server = uiState.server!!
                    val trustedHost = server.trustedHost

                    Text(
                        text = server.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = server.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    TimelineNoticeCard(
                        title = appearanceTitle,
                        message = appearanceMessage,
                        tone = TimelineNoticeTone.Neutral,
                        stateLabel = when (themePreference) {
                            ThemePreference.AUTO -> stringResource(R.string.server_settings_theme_auto)
                            ThemePreference.LIGHT -> stringResource(R.string.server_settings_theme_light)
                            ThemePreference.DARK -> stringResource(R.string.server_settings_theme_dark)
                        },
                        content = {
                            Button(onClick = onToggleTheme) {
                                Text(stringResource(R.string.server_settings_theme_cycle))
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimelineNoticeCard(
                        title = notificationsTitle,
                        message = if (canPostNotifications) notificationsEnabled else notificationsDisabled,
                        tone = TimelineNoticeTone.Neutral,
                        stateLabel = if (canPostNotifications) {
                            stringResource(R.string.server_settings_notifications_state_on)
                        } else {
                            stringResource(R.string.server_settings_notifications_state_off)
                        },
                        content = {
                            TextButton(
                                onClick = {
                                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    } else {
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    }
                                    context.startActivity(intent)
                                },
                            ) {
                                Text(notificationsManage)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimelineNoticeCard(
                        title = runtimeTitle,
                        message = runtimeMessage,
                        tone = TimelineNoticeTone.Neutral,
                        stateLabel = stringResource(R.string.server_settings_runtime_state_label),
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextButton(
                                    onClick = { runtimeSheetTarget = RuntimeControlTarget.Model },
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.session_control_model_format,
                                            runtimeControlLabel(RuntimeControlTarget.Model, uiState.runtimeDefaultModel),
                                        ),
                                    )
                                }
                                TextButton(
                                    onClick = { runtimeSheetTarget = RuntimeControlTarget.ReasoningEffort },
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.session_control_reasoning_format,
                                            runtimeControlLabel(RuntimeControlTarget.ReasoningEffort, uiState.runtimeDefaultReasoningEffort),
                                        ),
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.resetRuntimeDefaults(
                                            serverId = serverId,
                                            onSuccess = { message ->
                                                scope.launch { snackbarHostState.showSnackbar(message) }
                                            },
                                            onError = { message ->
                                                scope.launch { snackbarHostState.showSnackbar(message) }
                                            },
                                        )
                                    },
                                ) {
                                    Text(runtimeReset)
                                }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimelineNoticeCard(
                        title = trustTitle,
                        message = trustMessage,
                        tone = TimelineNoticeTone.Neutral,
                        stateLabel = if (trustedHost == null) trustBadgeUnpaired else trustBadgeTrusted,
                        content = {
                            if (trustedHost == null) {
                                Button(onClick = onOpenPairing) {
                                    Text(stringResource(R.string.server_settings_trust_pair_button))
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = trustAutoReconnect,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Switch(
                                            checked = trustedHost.autoReconnectEnabled,
                                            onCheckedChange = { enabled ->
                                                viewModel.updateTrustedReconnect(
                                                    serverId = serverId,
                                                    enabled = enabled,
                                                    onSuccess = { message ->
                                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                                    },
                                                    onError = { message ->
                                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                                    },
                                                )
                                            },
                                        )
                                    }
                                    trustedHost.trustLabel?.takeIf { it.isNotBlank() }?.let { label ->
                                        SettingsMetaRow(trustLabelTitle, label)
                                    }
                                    trustedHost.pairingMethod?.takeIf { it.isNotBlank() }?.let { method ->
                                        SettingsMetaRow(trustMethodTitle, method)
                                    }
                                    trustedHost.pairedAt?.let { pairedAt ->
                                        SettingsMetaRow(trustPairedAtTitle, formatSettingsTimestamp(pairedAt))
                                    }
                                    trustedHost.lastAutoReconnectAt?.let { lastReconnect ->
                                        SettingsMetaRow(trustLastReconnectTitle, formatSettingsTimestamp(lastReconnect))
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.clearTrustedHost(
                                                serverId = serverId,
                                                onSuccess = { message ->
                                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                                },
                                                onError = { message ->
                                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                                },
                                            )
                                        },
                                    ) {
                                        Text(trustClear)
                                    }
                                }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    TimelineNoticeCard(
                        title = passwordTitle,
                        message = passwordMessage,
                        footer = description,
                        tone = TimelineNoticeTone.Neutral,
                        stateLabel = stringResource(R.string.server_settings_password_state_label),
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = currentPassword,
                                    onValueChange = {
                                        currentPassword = it
                                        localError = null
                                    },
                                    label = { Text(currentPasswordLabel) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = {
                                        newPassword = it
                                        localError = null
                                    },
                                    label = { Text(newPasswordLabel) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        localError = null
                                    },
                                    label = { Text(confirmPasswordLabel) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = {
                                        val validation = when {
                                            currentPassword.isBlank() -> validationCurrentPasswordRequired
                                            newPassword.isBlank() -> validationNewPasswordRequired
                                            confirmPassword.isBlank() -> validationConfirmPasswordRequired
                                            newPassword != confirmPassword -> validationPasswordMismatch
                                            currentPassword == newPassword -> validationSamePassword
                                            else -> null
                                        }

                                        if (validation != null) {
                                            localError = validation
                                            return@Button
                                        }

                                        viewModel.changePassword(
                                            serverId = serverId,
                                            currentPassword = currentPassword,
                                            newPassword = newPassword,
                                            onSuccess = { message ->
                                                currentPassword = ""
                                                newPassword = ""
                                                confirmPassword = ""
                                                localError = null
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(message)
                                                }
                                            },
                                            onError = { message ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(message)
                                                }
                                            },
                                        )
                                    },
                                    enabled = !uiState.saving,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (uiState.saving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.height(20.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Text(saveButtonLabel)
                                    }
                                }
                            }
                        }
                    )

                    val errorMessage = localError ?: uiState.error
                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (runtimeSheetTarget != null) {
        val target = runtimeSheetTarget ?: RuntimeControlTarget.Model
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { runtimeSheetTarget = null },
            sheetState = sheetState,
        ) {
            RuntimeControlSheetContent(
                target = target,
                currentValue = when (target) {
                    RuntimeControlTarget.Model -> uiState.runtimeDefaultModel
                    RuntimeControlTarget.ReasoningEffort -> uiState.runtimeDefaultReasoningEffort
                },
                onSelect = { value ->
                    val nextModel = when (target) {
                        RuntimeControlTarget.Model -> value
                        RuntimeControlTarget.ReasoningEffort -> uiState.runtimeDefaultModel
                    }
                    val nextReasoning = when (target) {
                        RuntimeControlTarget.Model -> uiState.runtimeDefaultReasoningEffort
                        RuntimeControlTarget.ReasoningEffort -> value
                    }
                    viewModel.updateRuntimeDefaults(
                        serverId = serverId,
                        model = nextModel,
                        reasoningEffort = nextReasoning,
                        onSuccess = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        onError = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                    runtimeSheetTarget = null
                },
            )
        }
    }
}

@Composable
private fun SettingsMetaRow(
    title: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatSettingsTimestamp(raw: String): String = runCatching {
    Instant.parse(raw)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}.getOrElse { raw }
