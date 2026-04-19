# ImNotADeveloper

一个用于 **Xposed / LSPosed** 的模块，用来隐藏设备上的开发相关状态，例如：

- 开发者选项
- USB 调试
- 无线调试
- 部分调试属性与系统属性

## 如何打开设置

你可以通过以下方式进入模块设置：

- 在 **LSPosed** 中打开模块详情页后点击“模块设置”
- 在系统的应用信息页中点击“应用内的设置”

## Hook 点

- **android.provider.Settings**
  - `Secure.getStringForUser()`
  - `System.getStringForUser()`
  - `Global.getStringForUser()`
  - `NameValueCache.getStringForUser()`
- **android.os.SystemProperties**
  - `native_get()`
  - `native_get_int()`
  - `native_get_long()`
  - `native_get_boolean()`
- **java.lang.ProcessManager**
  - `exec()`
- **java.lang.ProcessImpl**
  - `start()`
- **native**
  - `__system_property_get()`
  - `__system_property_find()`

## 被隐藏的键 ([latest](app/src/main/java/io/github/auag0/imnotadeveloper/common/PropKeys.kt))

- **property keys**
  - `sys.usb.ffs.ready`
  - `sys.usb.config`
  - `persist.sys.usb.config`
  - `sys.usb.state`
  - `init.svc.adbd`
- **variable keys**
  - `development_settings_enabled`
  - `adb_enabled`
  - `adb_wifi_enabled`

## English

ImNotADeveloper is an **Xposed / LSPosed** module that hides developer-related signals on Android devices, including:

- Developer options state
- USB debugging state
- Wireless debugging state
- Selected debug and system properties

### Open settings

You can open the module settings from:

- the **LSPosed** module details page
- the system app info page via the in-app settings entry

### Hook targets

- `android.provider.Settings`
- `android.os.SystemProperties`
- `java.lang.ProcessManager`
- `java.lang.ProcessImpl`
- native system property functions

### Hidden keys

See the latest list in `app/src/main/java/io/github/auag0/imnotadeveloper/common/PropKeys.kt`.

## 致谢 / References

- [**xfqwdsj/IAmNotADeveloper**](https://github.com/xfqwdsj/IAmNotADeveloper)
- [**rushiranpise/Hide-Debugging**](https://github.com/rushiranpise/Hide-Debugging)
