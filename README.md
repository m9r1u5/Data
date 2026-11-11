# Data Tracker

An offline Android app that tracks your daily **mobile** and **Wi-Fi** data use per app.
No ads, no accounts, no analytics. Built with Kotlin + Jetpack Compose.

> Android only. iPhones don't allow any app to read per-app data usage.

## What it does

| Screen | What you get |
|---|---|
| **Today** | Mobile and Wi-Fi totals, then every app sorted by usage (icon, name, mobile MB, Wi-Fi MB, bar). Mobile / Wi-Fi / Both toggle. |
| **App detail** | Tap an app: last 30 days as a bar chart (foreground vs background), today, 30-day totals, daily average. |
| **History** | Pick any day (calendar or arrows) and see its apps plus the week and month totals. |
| **Bundle** | Enter bundle size (GB), reset day and price per GB (R). Shows used, remaining, days left, projected use, a safe daily amount, and the Rand cost per app. |
| **Settings** | Daily mobile limit alert, per-app limit alert, 80% / 100% bundle alerts, daily summary notification at your time, dark mode, show/hide system apps, Export CSV, Clear history, version number and **Check for update**. |

Mobile data from both SIMs on dual-SIM phones is added together and labelled "All SIMs combined".
Values under 1 GB show in MB, larger values in GB (1 GB = 1000 MB, like your network provider).

## Repo layout

```
.github/workflows/build.yml            builds the signed APK + GitHub release on every push
.github/workflows/create-keystore.yml  run once to make your signing key
settings.gradle.kts, build.gradle.kts, gradle.properties
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/...                   icon, theme, strings
app/src/main/java/com/m9r1u5/datatracker/
    MainActivity.kt, DataTrackerApp.kt
    data/   Room database, Android usage reader, repository, settings
    work/   hourly snapshot, midnight job, daily summary, alerts, notifications
    ui/     the screens
    util/   permissions, formatting, GitHub updater
```

## One-time setup (all from your phone, in the GitHub website)

You need a signing key so that every new build installs as an **update** over the old one.

1. **Add the password secret**
   Repo → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**
   - Name: `KEYSTORE_PASSWORD`
   - Secret: a strong password you'll remember (8+ characters, write it down)
2. **Create the key**
   Repo → **Actions** → **Create signing key (run once)** → **Run workflow**.
   Wait for the green tick (about 30 seconds).
3. **Download the key text**
   Open that finished run → scroll to **Artifacts** → tap **keystore-base64** to download the zip.
   Open the zip in your Files app, open `KEYSTORE_BASE64.txt`, **Select all** → **Copy**.
4. **Add the key secret**
   Settings → Secrets and variables → Actions → New repository secret
   - Name: `KEYSTORE_BASE64`
   - Secret: paste the text
5. Delete the downloaded zip from your phone. (The artifact on GitHub deletes itself after 1 day.)
6. **Build**: Actions → **Build APK** → **Run workflow** (it also runs on every push to `main`).

## Installing

- Open the repo's **Releases** (or the finished Build run's Artifacts), download `DataTracker-1.0.N.apk`, and open it.
  Allow "Install unknown apps" for your browser/Files app if Android asks.
- First launch: tap **Open Usage access settings**, switch on Data Tracker, come back.
- Allow notifications if you want alerts and the daily summary.

## Updating

Settings → **Check for update**. It checks the latest release in `m9r1u5/data`, downloads the newer APK and opens the installer.
The repo must be **public** for this to work (GitHub hides releases of private repos from apps).

## Good to know

- Android itself only keeps a few weeks of per-app history. Data Tracker copies it into its own database every hour, so your history grows from the day you install it.
- Numbers come from Android, so they match Settings → Data usage closely. Your network provider may count slightly differently.
- Alerts are checked every hour and whenever the app is open.
