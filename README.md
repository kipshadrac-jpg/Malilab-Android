# MaliLab for Android

Kotlin and Jetpack Compose app for MaliLab (https://malilab.online).

## How it is built
Nothing is compiled on a phone or server. Every push to `main` runs `.github/workflows/android.yml`,
which compiles the app on GitHub and publishes **MaliLab-debug.apk** on the repository's **Releases** page.

## How changes arrive
Claude prepares a patch zip. On the server: `tools/apply.sh ~/malilab_android_patchNN.zip "message"`.
That unpacks the files over the repository, commits and pushes. A patch zip holds files at their repository paths;
an optional `DELETE.txt` lists files to remove.

## Test signing
`debug.keystore` is a test-only signing key kept in the repository so every build has the same signature and
updates install over the previous one. It must be replaced by a private key before any Play Store release.

## Design
Teal and cream (see `app/src/main/java/online/malilab/app/ui/theme/Theme.kt`). Launcher icon: the MaliLab logo.
