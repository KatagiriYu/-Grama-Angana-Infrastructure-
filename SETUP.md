# Setup Instructions for Grama Angana

## Prerequisites

- Android Studio (latest stable version)
- JDK 11 or higher
- Git
- Google Cloud project with Firebase configured
- Firebase project with:
  - Firestore enabled
  - Authentication enabled (Email/Password)
  - Proper storage rules configured

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/KatagiriYu/-Grama-Angana-Infrastructure-.git
cd -Grama-Angana-Infrastructure-
```

### 2. Set Up Firebase

1. Go to the [Firebase Console](https://console.firebase.google.com)
2. Create a new project or select an existing one
3. Add an Android app with package name: `com.example.grama_angana`
4. Download the `google-services.json` file
5. Place it in `app/` directory

### 3. Configure Dependencies

The project uses Gradle with version catalogs. Verify `gradle/libs.versions.toml` has:

```toml
[versions]
kotlin = "1.9.0"
agp = "8.2.0"
compose-bom = "2024.02.00"
firebase-bom = "33.1.0"
kotlinx-serialization = "1.6.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx", version.ref = "firebase-bom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx", version.ref = "firebase-bom" }
firebase-storage = { group = "com.google.firebase", name = "firebase-storage-ktx", version.ref = "firebase-bom" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
firebase-google-services = { id = "com.google.gms.google-services", version = "4.4.0" }
```

### 4. Sync and Build

Open the project in Android Studio:

1. `File` → `Open` → select the project directory
2. Wait for Gradle sync to complete
3. Ensure `google-services.json` is in `app/`

### 5. Run the App

1. Connect an Android device or start an emulator
2. Click `Run` (green play button)
3. Select your device

## Configuration Notes

- **Package name**: `com.example.grama_angana`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34
- **Firebase Project ID**: Set in `app/build.gradle.kts`:

```kotlin
def projectID = "your-firebase-project-id"
```

## Required Firebase Rules

Ensure Firestore rules allow read/write for authenticated users:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## Troubleshooting

- **Firebase connection issues**: Verify `google-services.json` is in the correct location
- **Build errors**: Run `./gradlew clean` then sync again
- **Login failures**: Ensure Firebase Auth is enabled with Email/Password provider

## Support

For issues, please open a GitHub issue.
