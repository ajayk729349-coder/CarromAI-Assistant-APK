# Carrom AI Assistant (Android prototype)

This is a starter Android Studio project for a **screen-analysis assistant**. It does not modify Carrom Disc Pool, automate taps, read game memory, bypass protections, or submit moves.

## Current prototype
- Requests Android's built-in MediaProjection screen-sharing consent.
- Captures the screen while the foreground service is running.
- Samples a central screen region and reports rough blue, dark and red colour-pixel indicators.
- Displays observations in the assistant app.

## Important limitations
This is an early proof of concept, not a finished computer-vision model. The current detector does **not** reliably locate individual coins, identify the striker, find pockets, calculate collision paths, or recommend a mathematically verified shot. The screenshot can contain UI overlays, animations, shadows and different board layouts. The pixel totals are not coin counts.

## Build a downloadable APK with GitHub Actions
1. Create a new GitHub repository and upload the contents of this project folder (including the `.github` folder).
2. Open the repository's **Actions** tab.
3. Select **Build Android Debug APK** and click **Run workflow**, or push to the `main`/`master` branch.
4. Open the completed workflow run and download the **CarromAI-Assistant-debug** artifact.
5. Extract `app-debug.apk` and install it on your Android phone. Android may ask you to allow installation from that source.

## Build locally in Android Studio
1. Open this folder as an existing project in Android Studio.
2. Install Android SDK Platform 35 when prompted and let Gradle sync.
3. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Locate `app/build/outputs/apk/debug/app-debug.apk`.

## Use
1. Open the assistant and tap **Start live screen analysis**.
2. Approve Android's screen-sharing prompt.
3. Switch to the game; return to the assistant to view the latest analysis.
4. Tap **Stop analysis** when finished.

## Privacy
Screen capture is initiated only after the Android permission prompt. Stop the analysis service when finished. Avoid sharing screenshots containing personal notifications or private information.
