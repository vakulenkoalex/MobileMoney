# Сборка Android-приложения

## Сборка

```powershell
.\android\build.bat assembleDebug
```

**Windows**: используйте `;` или отдельные команды — НЕ используйте `&&`

## APK

- `android/build/outputs/apk/device/debug/app-device-debug.apk`

## minSDK

Проект использует minSDK 34 (Android 14). Старые версии Android не поддерживаются.

## Среда

- Используется Java из Android Studio: `C:\Program Files\Android\Android Studio\jbr`
- Настройка в `build.bat`: переменная `JAVA_HOME`
