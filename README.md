# FreshStart

A free, private, Noom-style weight-loss app for Android. Everything stays on your phone: no account, no ads, no subscription.

## Features
- **Personal plan**: enter your age, height, weight, goal and pace; get a daily calorie budget (Mifflin-St Jeor formula with a safe minimum).
- **Food log with colors**: ~100 common foods tagged green, yellow or red by calorie density, plus your own foods. Tracks calories left and your color mix (aim for 30% green, 45% yellow, 25% red).
- **Weight tracking**: log weigh-ins and see a trend chart against your goal, BMI, and a projected goal date.
- **Daily lessons**: 30 short lessons on habits and psychology, one highlighted each day.
- **Health Connect**: links with your Pixel's health apps. Weigh-ins from scales/Fitbit come in, your weigh-ins and meal calories go out, and steps plus active calories show on the Today screen. Turn it on in Settings.
- **Reminders**: optional nudges at 8:00, 12:30 and 19:30.

## Install
Every push to `main` builds a signed APK on GitHub Actions and publishes it as a release.
On your phone, open the latest release, download `FreshStart.apk`, and open it. Android will ask you to allow installs from your browser or Files app the first time.

Updates install over the old version and keep your data, because every build is signed with the same key (`app/freshstart.keystore`).

## Build locally
Requires the Android SDK (API 35) and JDK 17: `./gradlew assembleRelease`.
