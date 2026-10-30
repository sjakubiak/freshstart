# FreshStart

A private, color-coded weight-loss app for Android. No ads, and your data is never sold. The Google Play version is a subscription ($2/month after a 14-day free trial); the sideloaded APK is free.

## Features
- **Personal plan**: enter your age, height, weight, goal and pace; get a daily calorie budget (Mifflin-St Jeor formula with a safe minimum).
- **Food log with colors**: ~100 common foods tagged green, yellow or red by calorie density, plus your own foods. Tracks calories left and your color mix (aim for 30% green, 45% yellow, 25% red).
- **Meal plan**: suggests breakfast, lunch, dinner and a snack that fit your budget; tap "I ate this" to log with no typing, or "Swap" for another idea.
- **My kitchen**: list the foods you usually have (or scan a photo of your fridge with your own free Gemini key) and the plan favors meals you can make. With a key, Gemini can also invent meals from your kitchen.
- **Weight tracking**: log weigh-ins and see a trend chart against your goal, BMI, and a projected goal date.
- **Daily lessons**: 30 short lessons on habits and psychology, one highlighted each day.
- **Hero game**: pick one of six heroes (or use your own photo as the avatar) who levels up from what you already log. Green foods, days within budget, steps and weigh-ins earn XP; red foods and days well over budget cost XP. Earn 23 badges for streaks, steps, weight milestones, lessons and levels. Your hero, photo and badges back up with the rest of your data.
- **Health Connect**: links with your Pixel's health apps. Weigh-ins from scales/Fitbit come in, your weigh-ins and meal calories go out, and steps plus active calories show on the Today screen. Turn it on in Settings.
- **Reminders**: optional nudges at 8:00, 12:30 and 19:30.

- **Accounts and backup**: optional sign-in with Google or email; data is backed up to Firebase (Cloud Firestore) and restored on a new phone. Settings has sign out and account deletion.

## Install
Every push to `main` builds a signed APK on GitHub Actions and publishes it as a release.
On your phone, open the latest release, download `FreshStart.apk`, and open it. Android will ask you to allow installs from your browser or Files app the first time.

Updates install over the old version and keep your data, because every build is signed with the same key (`app/freshstart.keystore`).

## Build locally
Requires the Android SDK (API 35) and JDK 17: `./gradlew assembleRelease`.

## Google Play build
`./gradlew bundleRelease -Pstore=true` builds the Play version (app ID `com.sjakubiak.freshstart`), signed with the private upload key.
CI builds it automatically when the `UPLOAD_KEYSTORE_BASE64` and `UPLOAD_KEYSTORE_PASSWORD` repository secrets are set, and attaches `FreshStart-play.aab` to the run's artifacts.

Firebase is enabled by adding `app/google-services.json` (registered for both `com.freshstart.app` and `com.sjakubiak.freshstart`). Firestore security rules are in `firestore.rules`.
The privacy policy and account deletion pages are in `docs/`, published with GitHub Pages.

### Subscription and AI (Play build only)
- The Play build shows a paywall until the user has an active `freshstart_premium` subscription (Google Play Billing; see `data/Subscription.kt`). Base plans, prices and the 14-day free-trial offer are set up in Play Console, not in code. The sideload build is free and has no paywall.
- AI features (kitchen photo scan, AI meal ideas, AI coach) run through Firebase AI Logic on the developer's Firebase project, so subscribers don't need a key. The sideload build still uses the user's own Gemini key.
- App Check (Play Integrity) is installed in the Play build; switch on enforcement for AI Logic in the Firebase console once the app is live on Play.
