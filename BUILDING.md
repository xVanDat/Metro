# Building Metro from PowerShell

This fork uses JDK 17, Android SDK Platform 33, and Android Build Tools 33.0.2. Android Studio is not required.

## Debug APK on a low-memory PC

Open PowerShell in the repository, then run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat `
  '-Dorg.gradle.jvmargs=-Xmx1536m -XX:+UseSerialGC -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  --no-daemon `
  --no-parallel `
  --max-workers=1 `
  --no-configuration-cache `
  :app:assembleDebug
```

The APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

## Signed release APK

Create a keystore once. Keep it and its passwords safe; future updates must use the same key.

```powershell
New-Item -ItemType Directory -Force keys
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair `
  -keystore keys\metro-release.jks `
  -alias metro `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
Copy-Item keystore.properties.example keystore.properties
```

Edit `keystore.properties` and replace all placeholder passwords. Both that file and the keystore are ignored by Git.

Build with the same low-memory options, changing the final task to:

```powershell
.\gradlew.bat `
  '-Dorg.gradle.jvmargs=-Xmx1536m -XX:+UseSerialGC -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process' `
  --no-daemon `
  --no-parallel `
  --max-workers=1 `
  --no-configuration-cache `
  :app:assembleRelease
```

The signed APK is written to `app\build\outputs\apk\release\app-release.apk`.
