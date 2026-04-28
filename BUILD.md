# Сборка проекта

## Сборка

```bash
# Windows
.\gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

## APK
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## minSDK
Проект использует minSDK 34 (Android 14). Старые версии Android не поддерживаются.

## Среда
- Используется Java из Android Studio: `C:\Program Files\Android\Android Studio\jbr`
- Настройка в `gradle.properties`: `org.gradle.java.home`