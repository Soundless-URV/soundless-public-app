# Soundless

This repository contains the public Soundless Android app codebase.

**Maintainer:** 
Daniel Alejandro Coll -  danielalejandro.coll@urv.cat

**Requirements:**
- A developer account on Fitbit.
- All services configured in Google Cloud.

More information [here](https://github.com/Soundless-URV).


### Release 1.0.0 (October 2022)

- Soundless collects health data (sleep status, heart rate, etc) from wearables, whereas sound data comes from phone microphone.
- Sound collection works in a foreground, started, bound service.
- Soundless does not perform sound classification (labelling...).
- Recording information is separated into Metadata and Measurements.
  - Metadata objects are created at the beginning of the recording.
  - 10-second time windows are summarized into average Measurements that are sent to the API implemented in Firebase Cloud Functions. 
- Authentication to Fitbit is a standard PKCE process implemented by scratch using [Fitbit reference guides](https://dev.fitbit.com/).
- Heart Rate is obtained from Fitbit's [Intraday](https://dev.fitbit.com/build/reference/web-api/intraday/) API. (Obtained approval)
- Sleep is obtained from Fitbit's [Sleep](https://dev.fitbit.com/build/reference/web-api/sleep/) API.
- Application design: Single Activity, MVI-MVVM architecture. Tools: Jetpack Navigation, Hilt Dependency Injection, Room, Retrofit, KotlinX serialization.

### Backend architecture

Soundless uses several services within Firebase and Google Cloud Platform.

![soundless-diagram](https://user-images.githubusercontent.com/54351560/198060137-1418226e-7d81-4dd1-bd74-a5fb73b193b4.png)


### Building and Running the Application

#### Prerequisites
1. [Android Studio](https://developer.android.com/studio) installed on your machine
2. Firebase project set up with the necessary services

#### Import the Project
Follow the [instructions to import an existing project](https://developer.android.com/studio/projects/create-project#ImportAProject) into Android Studio:
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the soundless-public-app directory and click "OK"

#### Firebase Setup
1. Set up Firebase for the Soundless app following the [Firebase Android setup guide](https://firebase.google.com/docs/android/setup)
2. Download the `google-services.json` file from your Firebase project console
3. Place the `google-services.json` file in the app/ directory of the project

#### Building and Running
1. Connect a physical device or set up an emulator
2. Follow the [guide to run apps on Android devices](https://developer.android.com/studio/run)
3. Click the "Run" button in Android Studio, or use Shift+F10

### Contribution

We welcome support in the following tasks:
- Improve the sound collection mechanisms and apply A-weighting filters for dB measurements.
- Find smart ways to introduce sound classification.
- Improve design and UX, especially in the Fitbit authentication and pairing process.
