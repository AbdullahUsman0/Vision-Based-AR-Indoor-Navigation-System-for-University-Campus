# AR Navigation Starter (Java + ARCore + Sceneform)

Minimal, production-ready starter for indoor AR navigation:
- ARCore camera + tracking
- Real-time device pose debug overlay
- Mock pose JSON loader from assets
- 3D arrow rendering
- Quaternion rotation application from JSON

## Project Structure

```text
AR_NavigationSystem/
  app/
    build.gradle
    src/main/
      AndroidManifest.xml
      assets/pose_mock.json
      java/com/mahad/arnavigation/
        MainActivity.java
        ar/
          ArController.java
          NavigationArFragment.java
        data/
          PoseMock.java
          PoseRepository.java
          Position.java
          Rotation.java
        render/
          ArrowFactory.java
        util/
          PoseSmoother.java
      res/
        layout/activity_main.xml
        values/strings.xml
        values/themes.xml
  build.gradle
  settings.gradle
  gradle.properties
```

## Setup

1. Install Java 17.
2. Install Android SDK command-line tools.
3. Install SDK packages:
   - `platform-tools`
   - `platforms;android-34`
   - `build-tools;34.0.0`
4. Create `local.properties` in project root:

```properties
sdk.dir=C:\\Users\\<your-user>\\AppData\\Local\\Android\\Sdk
```

5. Build from terminal:

```powershell
.\tools\gradle-8.6\bin\gradle.bat --no-daemon assembleDebug
```

6. Install on device:

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Notes

- Use a physical ARCore-supported Android phone for reliable AR behavior.
- `ArController` logs applied JSON pose for coordinate-system debugging.
- `PoseSmoother` provides simple low-pass smoothing to reduce jitter.
- This is a fully native Android app; it will not run in Expo Go.
