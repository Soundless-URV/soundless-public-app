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


### Contribution

We welcome support in the following tasks:
- Improve the sound collection mechanisms and apply A-weighting filters for dB measurements.
- Find smart ways to introduce sound classification.
- Improve design and UX, especially in the Fitbit authentication and pairing process.
