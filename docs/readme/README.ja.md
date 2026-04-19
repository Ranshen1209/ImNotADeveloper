# ImNotADeveloper

[リポジトリトップへ戻る](../../README.md) | [中文](README.zh-CN.md)

ImNotADeveloper は、Android 端末上の開発者向け状態を隠すための **Xposed / LSPosed** モジュールです。主な対象は次のとおりです。

- 開発者向けオプション
- USB デバッグ
- ワイヤレスデバッグ
- 一部のデバッグ用プロパティとシステムプロパティ

## 設定画面の開き方

モジュール設定は次の場所から開けます。

- **LSPosed** のモジュール詳細画面にある「モジュール設定」
- Android のアプリ情報画面にあるアプリ内設定エントリ

不要な外部公開面を減らすため、汎用 launcher エントリは提供していません。

## Hook 対象

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

## 隠蔽対象キー

最新の一覧は [PropKeys.kt](../../app/src/main/java/io/github/auag0/imnotadeveloper/common/PropKeys.kt) を参照してください。

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

Release ビルドでは、以下の環境変数から署名情報を読み込みます。

- `storePassword`
- `keyAlias`
- `keyPassword`

keystore のパスは次の順序で解決されます。

- Gradle プロパティ `-Pimnotadeveloper.keystore.path=/path/to/release-keystore.jks`
- 環境変数 `IMNOTADEVELOPER_KEYSTORE_PATH`
- 互換性維持用のフォールバック `app/release-keystore.jks`

セキュリティと運用性の観点から、keystore はリポジトリ外に保管し、パスを明示的に渡すことを推奨します。

## References

- [**xfqwdsj/IAmNotADeveloper**](https://github.com/xfqwdsj/IAmNotADeveloper)
- [**rushiranpise/Hide-Debugging**](https://github.com/rushiranpise/Hide-Debugging)
