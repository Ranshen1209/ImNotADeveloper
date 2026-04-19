# ImNotADeveloper

[中文](docs/readme/README.zh-CN.md) | [日本語](docs/readme/README.ja.md)

ImNotADeveloper is an **LSPosed / Xposed** module that hides developer-related signals on Android devices.

## Highlights

- Hides developer options, USB debugging, and wireless debugging indicators
- Filters selected system settings and system properties
- Hooks Java and native property access paths
- Provides a dedicated settings screen through LSPosed and the Android app-info settings entry

## Open settings

You can open the module settings from:

- the **LSPosed** module details page
- the system app info page via the in-app settings entry

The app no longer exposes a generic launcher entry, which reduces unnecessary external surface area.

## Build

- Debug build: `bash ./gradlew assembleDebug`
- Full build: `bash ./gradlew build`
- Lint: `bash ./gradlew lint`

## Full documentation

- [中文文档](docs/readme/README.zh-CN.md)
- [日本語ドキュメント](docs/readme/README.ja.md)

## References

- [**xfqwdsj/IAmNotADeveloper**](https://github.com/xfqwdsj/IAmNotADeveloper)
- [**rushiranpise/Hide-Debugging**](https://github.com/rushiranpise/Hide-Debugging)
