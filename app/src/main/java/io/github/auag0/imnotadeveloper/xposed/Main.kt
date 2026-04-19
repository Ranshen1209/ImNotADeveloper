package io.github.auag0.imnotadeveloper.xposed

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.auag0.imnotadeveloper.common.Logger.logD
import io.github.auag0.imnotadeveloper.common.Logger.logE
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_DEBUG_PROPERTIES
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_DEBUG_PROPERTIES_IN_NATIVE
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_DEVELOPER_MODE
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_USB_DEBUG
import io.github.auag0.imnotadeveloper.common.PrefKeys.HIDE_WIRELESS_DEBUG
import io.github.auag0.imnotadeveloper.common.PrefKeys.REMOTE_PREFERENCES_GROUP
import io.github.auag0.imnotadeveloper.common.PropKeys
import io.github.auag0.imnotadeveloper.common.PropKeys.ADB_ENABLED
import io.github.auag0.imnotadeveloper.common.PropKeys.ADB_WIFI_ENABLED
import io.github.auag0.imnotadeveloper.common.PropKeys.DEVELOPMENT_SETTINGS_ENABLED
import java.lang.reflect.Method

class Main : XposedModule() {
    private val settingsSource by lazy {
        RemotePreferencesSettingsSource { getRemotePreferences(REMOTE_PREFERENCES_GROUP) }
    }
    private val hookRegistrar by lazy {
        ModernHookRegistrar(
            settingsSource = settingsSource,
            propOverrides = PROP_OVERRIDES,
            xposed = this
        )
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        hookRegistrar.hookNativeMethods()
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage) return
        hookRegistrar.registerHooks(param.classLoader)
    }

    private companion object {
        val PROP_OVERRIDES = mapOf(
            PropKeys.SYS_USB_FFS_READY to "0",
            PropKeys.SYS_USB_CONFIG to "mtp",
            PropKeys.PERSIST_SYS_USB_CONFIG to "mtp",
            PropKeys.SYS_USB_STATE to "mtp",
            PropKeys.INIT_SVC_ADBD to "stopped"
        )
    }
}

private data class ModuleConfig(
    val hideDeveloperMode: Boolean,
    val hideUsbDebug: Boolean,
    val hideWirelessDebug: Boolean,
    val hideDebugProperties: Boolean,
    val hideDebugPropertiesInNative: Boolean,
) {
    fun bannedSettingsKeys(): Set<String> = buildSet {
        if (hideDeveloperMode) add(DEVELOPMENT_SETTINGS_ENABLED)
        if (hideUsbDebug) add(ADB_ENABLED)
        if (hideWirelessDebug) add(ADB_WIFI_ENABLED)
    }
}

private interface SettingsSource {
    fun loadConfig(): ModuleConfig
}

private class RemotePreferencesSettingsSource(
    private val prefsProvider: () -> SharedPreferences
) : SettingsSource {
    private val prefs by lazy(prefsProvider)

    override fun loadConfig(): ModuleConfig {
        return ModuleConfig(
            hideDeveloperMode = prefs.getBoolean(HIDE_DEVELOPER_MODE, true),
            hideUsbDebug = prefs.getBoolean(HIDE_USB_DEBUG, true),
            hideWirelessDebug = prefs.getBoolean(HIDE_WIRELESS_DEBUG, true),
            hideDebugProperties = prefs.getBoolean(HIDE_DEBUG_PROPERTIES, true),
            hideDebugPropertiesInNative = prefs.getBoolean(HIDE_DEBUG_PROPERTIES_IN_NATIVE, true),
        )
    }
}

private class ModernHookRegistrar(
    private val settingsSource: SettingsSource,
    private val propOverrides: Map<String, String>,
    private val xposed: XposedInterface,
) {
    fun registerHooks(classLoader: ClassLoader) {
        hookSettingsMethods(classLoader)
        hookSystemPropertiesMethods(classLoader)
        hookProcessMethods(classLoader)
    }

    fun hookNativeMethods() {
        if (!currentConfig().hideDebugPropertiesInNative) return
        try {
            System.loadLibrary("ImNotADeveloper")
            NativeFun.setProps(propOverrides)
        } catch (e: LinkageError) {
            logE(e.message)
        } catch (e: Exception) {
            logE(e.message)
        }
    }

    private fun hookProcessMethods(classLoader: ClassLoader) {
        hookAllMethods(classLoader, "java.lang.ProcessImpl", "start") { chain ->
            if (!currentConfig().hideDebugProperties) return@hookAllMethods chain.proceed()
            hookedLog(chain)
            val commandArgs = (chain.getArg(0) as? Array<*>)
                ?.filterIsInstance<String>()
                ?: return@hookAllMethods chain.proceed()
            val rewrittenArgs = rewriteGetPropCommand(commandArgs) ?: return@hookAllMethods chain.proceed()
            val newArgs = chain.getArgs().toMutableList().toTypedArray()
            newArgs[0] = rewrittenArgs
            chain.proceed(newArgs)
        }

        hookAllMethods(classLoader, "java.lang.ProcessManager", "exec") { chain ->
            if (!currentConfig().hideDebugProperties) return@hookAllMethods chain.proceed()
            hookedLog(chain)
            val commandArgs = (chain.getArg(0) as? Array<*>)
                ?.filterIsInstance<String>()
                ?: return@hookAllMethods chain.proceed()
            val rewrittenArgs = rewriteGetPropCommand(commandArgs) ?: return@hookAllMethods chain.proceed()
            val newArgs = chain.getArgs().toMutableList().toTypedArray()
            newArgs[0] = rewrittenArgs
            chain.proceed(newArgs)
        }
    }

    private fun hookSystemPropertiesMethods(classLoader: ClassLoader) {
        val className = "android.os.SystemProperties"
        val methods = arrayOf(
            "native_get",
            "native_get_int",
            "native_get_long",
            "native_get_boolean"
        )
        methods.forEach { methodName ->
            hookAllMethods(classLoader, className, methodName) { chain ->
                val key = chain.getArg(0) as? String ?: return@hookAllMethods chain.proceed()
                if (!currentConfig().hideDebugProperties) return@hookAllMethods chain.proceed()
                hookedLog(chain)
                val method = chain.getExecutable() as? Method ?: return@hookAllMethods chain.proceed()
                val value = propOverrides[key] ?: return@hookAllMethods chain.proceed()
                try {
                    when (method.returnType) {
                        String::class.java -> value
                        Int::class.javaPrimitiveType, Int::class.javaObjectType -> value.toInt()
                        Long::class.javaPrimitiveType, Long::class.javaObjectType -> value.toLong()
                        Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> value.toBooleanValue()
                        else -> chain.proceed()
                    }
                } catch (e: NumberFormatException) {
                    logE(e.message)
                    chain.proceed()
                }
            }
        }
    }

    private fun hookSettingsMethods(classLoader: ClassLoader) {
        val settingsClassNames = arrayOf(
            "android.provider.Settings\$Secure",
            "android.provider.Settings\$System",
            "android.provider.Settings\$Global",
            "android.provider.Settings\$NameValueCache"
        )
        settingsClassNames.forEach { className ->
            hookAllMethods(classLoader, className, "getStringForUser") { chain ->
                val bannedKeys = currentConfig().bannedSettingsKeys()
                if (bannedKeys.isEmpty()) return@hookAllMethods chain.proceed()
                hookedLog(chain)
                val name = chain.getArg(1) as? String
                if (name in bannedKeys) "0" else chain.proceed()
            }
        }
    }

    private fun hookAllMethods(
        classLoader: ClassLoader,
        className: String,
        methodName: String,
        interceptor: (XposedInterface.Chain) -> Any?
    ) {
        val clazz = runCatching { Class.forName(className, false, classLoader) }
            .getOrElse {
                logE(it.message)
                return
            }
        clazz.declaredMethods
            .filter { it.name == methodName }
            .forEach { method ->
                runCatching {
                    xposed.hook(method).intercept(object : XposedInterface.Hooker {
                        override fun intercept(chain: XposedInterface.Chain): Any? = interceptor(chain)
                    })
                }.onFailure {
                    logE(it.message)
                }
            }
    }

    private fun currentConfig(): ModuleConfig = settingsSource.loadConfig()

    private fun rewriteGetPropCommand(commandArgs: List<String>): Array<String>? {
        val firstCommand = commandArgs.getOrNull(0)
        val secondCommand = commandArgs.getOrNull(1)
        if (firstCommand != "getprop" || secondCommand !in propOverrides) {
            return null
        }
        val rewrittenCommand = ArrayList(commandArgs)
        rewrittenCommand[1] = "Dummy${System.currentTimeMillis()}"
        return rewrittenCommand.toTypedArray()
    }

    private fun hookedLog(chain: XposedInterface.Chain) {
        val method = chain.getExecutable() as? Method ?: return
        val message = buildString {
            appendLine("Hooked ${method.declaringClass.name}$${method.name} -> ${method.returnType.name}")
            chain.getArgs().forEachIndexed { index, arg ->
                appendLine("    $index:${arg.stringValue()}")
            }
        }
        logD(message)
    }
}

private fun Any?.stringValue(): String {
    return when (this) {
        is List<*> -> joinToString(prefix = "[", postfix = "]")
        is Array<*> -> joinToString(prefix = "[", postfix = "]")
        else -> toString()
    }
}

private fun String.toBooleanValue(): Boolean {
    return when {
        equals("true", true) || equals("1", true) -> true
        equals("false", true) || equals("0", true) -> false
        else -> throw NumberFormatException(this)
    }
}
