
#EcoFriendlyApp README
Overview
EcoFriendlyApp is an Android application designed to help users report waste issues in Blantyre, Malawi, with features like a map interface to locate waste bins, a report submission system, eco tips and encouragement, user profile management, and authentication via Firebase. This app aims to promote eco-friendly practices by enabling users to report illegal dumpsites and uncollected wastes while encouraging sustainable habits.
Project Structure

Languages: Kotlin (with Jetpack Compose for UI)
Dependencies: Firebase (Authentication, Firestore, Storage), osmdroid (for maps), Accompanist Permissions
Target SDK: 35 (Android 15)
Minimum SDK: 23 (Android 6.0)

Features

Dashboard Screen: Displays a map centered on Blantyre, Malawi, with clickable waste bin markers showing coordinates. The map uses osmdroid with OpenStreetMap tiles.
Report Screen: Allows users to submit waste issue reports with a title, description, location, and optional photo upload to Firebase Storage, stored in Firestore.
Plans Screen: Provides eco tips (e.g., recycling guidelines) and encouragement messages to motivate users toward sustainable practices.
Profile Screen: Displays user information (name, email) and allows editing of the display name and profile details.
User Authentication: Uses Firebase Authentication to manage user sessions.
Offline Support: Map supports offline tile caching with osmdroid.

Setup Instructions
Prerequisites

Android Studio (latest version)
Java Development Kit (JDK) 17
Android Emulator or Physical Device (Android 6.0+)
Firebase Project with enabled Authentication, Firestore, and Storage

Installation

Clone the Repository:

git clone <repository-url> 


Set Up Firebase:

Create a Firebase project at console.firebase.google.com.
Enable Authentication, Firestore, and Storage in the Firebase Console.
Upgrade to the Blaze plan (required for Storage) by linking a Cloud Billing account in Project Settings > Upgrade. Note: The billing account cannot be deleted but can be closed to avoid charges if within the free tier.
Download google-services.json and place it in app/.


Configure Keystore:

Create a keystore using keytool -genkeypair -v -keystore mynewkeystore.keystore -alias mynewalias -keyalg RSA -keysize 2048 -validity 10000 (store securely with chosen password and alias).
Update app/build.gradle with the keystore path, password, and alias under signingConfigs.


Add Dependencies:

In app/build.gradle, include:implementation platform('com.google.firebase:firebase-bom:33.5.0')
implementation 'com.google.firebase:firebase-auth-ktx'
implementation 'com.google.firebase:firebase-firestore-ktx'
implementation 'com.google.firebase:firebase-storage-ktx'
implementation 'org.osmdroid:osmdroid-android:6.1.18'
implementation "com.google.accompanist:accompanist-permissions:0.34.0"


Sync the project.


Permissions:

Update AndroidManifest.xml with:<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />




Build the App:

Open Android Studio, load the project, and sync Gradle.
Generate a signed APK: Build > Generate Signed Bundle / APK, using your keystore.
Install the app-release.apk on a device/emulator.



Usage

Dashboard: View the map, tap markers for coordinates, and navigate to other screens.
Report: Select a title, enter description and location, upload a photo, and submit. Requires internet for photo uploads.
Plans: Access eco tips (e.g., "Reduce, Reuse, Recycle") and encouraging messages (e.g., "Every small step helps!").
Profile: View and edit user name and profile details, synced with Firebase.
Offline: Map works offline after caching tiles while online.

Known Issues and Fixes

Map Sizing: If the map doesn’t fit, adjust the height in DashboardScreen (e.g., 400.dp) or recalculate availableHeight.
Photo Upload: Ensure a billing account is linked for Firebase Storage. Verify photoUri and storage rules (allow write: if request.auth != null;).
Network Dependency: Cache tiles offline by pre-loading while online or use a static map image as a fallback.

Troubleshooting

Map Not Loading: Check internet or pre-cache tiles. Use adb logcat for errors.
Photo Upload Fails: Confirm photoUri is valid, check Firebase Storage rules, and ensure Blaze plan is active.
Installation Errors: Verify minSdk = 23 and targetSdk = 35 in build.gradle, and use a correct keystore.
Profile Edit Issues: Ensure Firebase Authentication is initialized and user is signed in.

Contributing
Feel free to fork and submit pull requests. Report issues on the repository.

Contact
For support, reach out via the project repository or email.
