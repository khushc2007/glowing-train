# Contineo Login (personal helper app)

A small Android app that wraps the real MSRIT Contineo parent portal
(parents.msrit.edu) in a WebView and auto-fills **your own saved**
USN, date of birth, and parent-verification digits into the login
form, so you don't have to retype them every time you check your
results/attendance from your phone.

- Your details are stored **only on your phone**, encrypted
  (`EncryptedSharedPreferences`).
- The app never talks to the Contineo site directly itself — it just
  fills in text boxes inside the real site's own page. All actual
  login/auth happens on msrit's servers exactly as if you typed it
  yourself.
- Nothing is uploaded anywhere, no backend, no analytics.

## 1. Get this building as an APK (no Android Studio needed)

You're on your laptop now, so do this bit today:

1. Create a **new GitHub repository** (make it **Private** — it's
   your personal login helper, no reason to make it public).
2. Push this whole folder to that repo:
   ```bash
   cd ContineoLogin
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<repo-name>.git
   git push -u origin main
   ```
3. On GitHub, open your repo → **Actions** tab. A workflow called
   "Build APK" will run automatically (takes ~3-5 min).
4. Once it finishes (green check), click into that run → scroll to
   **Artifacts** → download `ContineoLogin-debug-apk` (a zip
   containing `app-debug.apk`).
5. AirDrop/Bluetooth/email/Google Drive that APK to your phone,
   unzip if needed, tap it to install. You'll need to allow
   "Install unknown apps" for whatever app you used to open it
   (Android will prompt you — it's a one-time toggle).

After this, whenever you want a new build (e.g. after editing the
autofill selectors below), just `git push` again and grab the new
APK from Actions — no laptop compiling required.

## 2. First run on your phone

1. Open the app → it'll ask you to enter your USN, DOB, whether you
   verify with mother's or father's number, and the last 4 digits.
   Tap **Save & Continue**.
2. It loads the real Contineo login page and tries to auto-fill your
   USN + DOB. If the verification step (mother/father + last 4)
   appears after you submit, tap the **Auto-fill** button in the top
   bar to re-run the fill on that new step.
3. Double-check the filled fields look right, then tap the site's own
   Login/Submit button yourself.

## 3. If auto-fill doesn't find a field

The verification step loads dynamically, so I couldn't inspect its
exact field names in advance. If a field doesn't get filled:

1. On your phone, enable Developer Options → USB debugging.
2. Plug into a laptop with Chrome, open `chrome://inspect`, find the
   WebView, click **Inspect**.
3. In the Elements panel, find the actual input's `name` or `id`
   attribute.
4. Open `app/src/main/java/com/khush/contineologin/MainActivity.kt`,
   find `buildAutofillScript()`, and add that exact name/id into the
   relevant `querySelectorAll(...)` list (e.g. the "last 4 digits"
   one near the bottom).
5. Commit + push, grab the new APK from Actions.

## Notes

- This is a **debug build** (unsigned), fine for installing on your
  own device via "unknown sources." If you ever want to share it
  more widely you'd need to sign it properly — but there's no reason
  to, since this only makes sense with your own saved credentials.
- Keep the GitHub repo **private** since (encrypted or not) it's
  tied to your personal login flow.
