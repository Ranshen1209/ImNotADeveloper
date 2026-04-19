# ImNotADeveloper

[返回仓库首页](../../README.md) | [日本語](README.ja.md)

ImNotADeveloper 是一个用于 **Xposed / LSPosed** 的模块，用来隐藏设备上的开发相关状态，例如：

- 开发者选项
- USB 调试
- 无线调试
- 部分调试属性与系统属性

## 如何打开设置

你可以通过以下方式进入模块设置：

- 在 **LSPosed** 中打开模块详情页后点击“模块设置”
- 在系统的应用信息页中点击“应用内的设置”

应用不再提供通用 launcher 入口，以减少不必要的外部暴露面。

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

## 被隐藏的键

最新列表见 [PropKeys.kt](../../app/src/main/java/io/github/auag0/imnotadeveloper/common/PropKeys.kt)。

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

## Release signing

Release 构建读取以下环境变量：

- `storePassword`
- `keyAlias`
- `keyPassword`

keystore 路径按以下顺序解析：

- Gradle 属性 `-Pimnotadeveloper.keystore.path=/path/to/release-keystore.jks`
- 环境变量 `IMNOTADEVELOPER_KEYSTORE_PATH`
- 兼容回退路径 `app/release-keystore.jks`

从安全和维护角度出发，更推荐把 keystore 放在仓库目录之外，并显式传入路径。

## 致谢 / References

- [**xfqwdsj/IAmNotADeveloper**](https://github.com/xfqwdsj/IAmNotADeveloper)
- [**rushiranpise/Hide-Debugging**](https://github.com/rushiranpise/Hide-Debugging)
