# Fluid Glass Launcher — mobile-only build guide

This zip is a complete, ready-to-build Android Studio project. You don't need a
computer — GitHub's servers will compile the APK for you via GitHub Actions
(already configured in `.github/workflows/build.yml`).

## Steps (all on your phone)

1. **Create a new empty repo** on github.com (name it anything, e.g. `fluid-glass-launcher`).
   Do NOT initialize it with a README.

2. **Get the project files into that repo.** Easiest mobile route — install **Termux**
   (from F-Droid or Play Store), then in Termux:

   ```
   pkg install git -y
   termux-setup-storage
   git clone https://github.com/<you>/fluid-glass-launcher.git
   cd fluid-glass-launcher
   ```

   Unzip this project into that cloned folder (move/extract the zip from your
   Downloads into the repo folder — a file manager app like "Files" or
   "Material Files" can extract zips directly into that path), then:

   ```
   git add .
   git commit -m "Initial project"
   git push
   ```

3. **Watch it build:** open your repo on github.com → **Actions** tab. A "Build APK"
   run should start automatically. It takes a few minutes.

4. **Download the APK:** once the run finishes (green check), open it → scroll to
   **Artifacts** → download `app-debug-apk` (a zip containing the `.apk`).

5. **Install on your phone:** open the downloaded apk from your Downloads/Files
   app. Android will ask to allow installs from that app — allow it, then install.

## If a build fails

Open the failed Actions run and check the red step's log — it'll usually be a
missing resource or a version mismatch. Paste the error back to Claude and it
can be fixed directly in the repo.
