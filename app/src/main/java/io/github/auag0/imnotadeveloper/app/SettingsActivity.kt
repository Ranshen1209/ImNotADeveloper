package io.github.auag0.imnotadeveloper.app

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.auag0.imnotadeveloper.R
import io.github.auag0.imnotadeveloper.app.ui.theme.ImNotADeveloperTheme
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_DEBUG_PROPERTIES
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_DEBUG_PROPERTIES_IN_NATIVE
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_DEVELOPER_MODE
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_USB_DEBUG
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_WIRELESS_DEBUG
import io.github.auag0.imnotadeveloper.common.PrefKeys.REMOTE_PREFERENCES_GROUP
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.lang.ref.WeakReference

class SettingsActivity : ComponentActivity() {
    private val options = listOf(
        Option(R.string.hide_developer_mode, HIDE_DEVELOPER_MODE, true),
        Option(R.string.hide_usb_debug, HIDE_USB_DEBUG, true),
        Option(R.string.hide_wireless_debug, HIDE_WIRELESS_DEBUG, true),
        Option(R.string.hide_debug_properties, HIDE_DEBUG_PROPERTIES, true),
        Option(R.string.hide_debug_properties_in_native, HIDE_DEBUG_PROPERTIES_IN_NATIVE, true),
    )
    private var service: XposedService? = null
    private var preferences: SharedPreferences? = null
    private var isLoading by mutableStateOf(true)
    private var optionStates by mutableStateOf(emptyList<OptionState>())
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == null) return@OnSharedPreferenceChangeListener
        runOnUiThread {
            optionStates = optionStates.map { state ->
                if (state.key == key) {
                    state.copy(isChecked = sharedPreferences.getBoolean(state.key, state.defaultValue))
                } else {
                    state
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivity = WeakReference(this)
        setContent {
            ImNotADeveloperTheme {
                SettingsScreen(
                    isLoading = isLoading,
                    optionStates = optionStates,
                    onToggle = ::updatePreference
                )
            }
        }
        if (!isServiceListenerRegistered) {
            XposedServiceHelper.registerListener(serviceListener)
            isServiceListenerRegistered = true
        }
        cachedService?.let(::bindService)
    }

    override fun onStart() {
        super.onStart()
        currentActivity = WeakReference(this)
        cachedService?.let(::bindService)
    }

    override fun onDestroy() {
        if (currentActivity?.get() === this) {
            currentActivity = null
        }
        preferences?.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        preferences = null
        service = null
        super.onDestroy()
    }

    private fun bindService(service: XposedService) {
        this.service = service
        val remotePreferences = service.getRemotePreferences(REMOTE_PREFERENCES_GROUP)
        preferences?.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        preferences = remotePreferences
        remotePreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        optionStates = options.map { option ->
            OptionState(
                titleRes = option.titleRes,
                key = option.key,
                defaultValue = option.defaultValue,
                isChecked = remotePreferences.getBoolean(option.key, option.defaultValue)
            )
        }
        isLoading = false
    }

    private fun handleServiceDied(service: XposedService) {
        if (this.service !== service) return
        this.service = null
        preferences?.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        preferences = null
        runOnUiThread {
            isLoading = false
            Toast.makeText(this, R.string.sp_is_not_available, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updatePreference(key: String, isChecked: Boolean) {
        val currentPreferences = preferences ?: return
        optionStates = optionStates.map { state ->
            if (state.key == key) state.copy(isChecked = isChecked) else state
        }
        currentPreferences.edit {
            putBoolean(key, isChecked)
        }
    }

    private companion object {
        private var currentActivity: WeakReference<SettingsActivity>? = null
        private var cachedService: XposedService? = null
        private var isServiceListenerRegistered = false
        private val serviceListener = object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                cachedService = service
                currentActivity?.get()?.runOnUiThread {
                    currentActivity?.get()?.bindService(service)
                }
            }

            override fun onServiceDied(service: XposedService) {
                if (cachedService === service) {
                    cachedService = null
                }
                currentActivity?.get()?.handleServiceDied(service)
            }
        }
    }
}

private data class Option(
    @param:StringRes val titleRes: Int,
    val key: String,
    val defaultValue: Boolean,
)

private data class OptionState(
    @param:StringRes val titleRes: Int,
    val key: String,
    val defaultValue: Boolean,
    val isChecked: Boolean,
)

@Composable
private fun SettingsScreen(
    isLoading: Boolean,
    optionStates: List<OptionState>,
    onToggle: (String, Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings_title))
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(optionStates, key = { it.key }) { optionState ->
                OptionRow(optionState = optionState, onToggle = onToggle)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun OptionRow(
    optionState: OptionState,
    onToggle: (String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = optionState.isChecked,
                role = Role.Switch,
                onValueChange = { checked -> onToggle(optionState.key, checked) }
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(optionState.titleRes),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = optionState.isChecked,
            onCheckedChange = null
        )
    }
}
