# Releasing DadCalc to Google Play

## One-time: create the upload key

Run this from the project root (keytool comes with the JDK):

```
keytool -genkeypair -v -keystore dadcalc-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Then create `keystore.properties` in the project root:

```
storeFile=dadcalc-upload.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=upload
keyPassword=YOUR_KEY_PASSWORD
```

Both files are gitignored. Back them up somewhere safe outside this folder, e.g. a password manager.
Play App Signing (on by default) keeps the real app signing key, so a lost upload key can be reset
through Play support, but it is slow.

## Every release

1. In `app/build.gradle.kts`, increase `versionCode` by 1 and update `versionName` (0.1, 0.2, ...).
2. Build the bundle:
   ```
   gradlew bundleRelease
   ```
3. Upload `app/build/outputs/bundle/release/app-release.aab` in Play Console.

Without `keystore.properties` the bundle still builds but is unsigned, and Play will reject it.
