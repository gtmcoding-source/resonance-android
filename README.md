# Resonance

An offline Android meditation frequency generator by GTM Studios.

## Project details

- Native Android application
- Kotlin and Jetpack Compose
- Audio generated locally with Android `AudioTrack`
- Minimum Android version: API 26
- Cloud APK builds with GitHub Actions

## Build an APK

Push this repository to GitHub using the `main` branch. The workflow in
`.github/workflows/android-build.yml` builds a debug APK on each push.

You can also run it manually from **Actions → Build Resonance APK → Run workflow**.

After a successful build, download the `Resonance-Android-APK` artifact.

## Current prototype scope

- Sine-wave tone generation
- Adjustable frequency (20–2,000 Hz)
- Preset frequency buttons
- Volume control
- Foreground-only playback

Background playback, session timer, additional waveforms, favorites and
binaural audio are planned for later milestones.

## Safety

Start at a low volume and increase only to a comfortable listening level.
Frequency presets are listening choices, not medical treatments.
"# resonance-android" 
