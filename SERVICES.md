# Connecting the three services that are not started

Gmail import, Google Drive backup and Firebase sync are the only features in the README that
cannot be built without an account someone owns. Each needs a project registered to **you**, with
OAuth clients tied to **your** release signing certificate — there is nothing useful to commit
until those exist, which is why the code for them is absent rather than stubbed.

This is what to create, what to send back, and what gets built once it arrives.

Everything below stays inside a free tier at personal or family scale. None of it needs a card.

---

## 0. The one thing all three need first

**A Google Cloud project**, and the SHA-1 fingerprint of the signing key the app ships with.

1. Go to <https://console.cloud.google.com/> → **Select a project** → **New project**.
   Name it anything (`money-manager` is fine). Note the **project ID**.
2. Get the fingerprint of your signing key. For the debug key, which is what Android Studio
   builds with:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

   For a release build, point `-keystore` at your own keystore and use its alias and passwords.
   Copy the line beginning `SHA1:`.

3. You need this fingerprint **for every keystore you ship from**. A debug-signed build and a
   release-signed build are different apps as far as Google is concerned, and an OAuth client
   registered against one will silently reject the other.

> **Send me:** nothing yet. The fingerprint and project ID go into the console, not into the
> repository. Never paste a keystore, a password, or a `client_secret` into a chat or a commit.

---

## 1. Gmail receipt import

**What it does once wired:** finds order and invoice emails, reads the merchant and the amount,
and offers a pre-filled transaction. It never posts one by itself — same rule as receipt scanning.

**Free tier:** the Gmail API is free. The quota is 1,000,000,000 units/day; reading a few hundred
messages costs a few thousand. Not a concern at this scale.

### Steps

1. In your Cloud project: **APIs & Services → Library → Gmail API → Enable**.
2. **APIs & Services → OAuth consent screen**:
   - User type **External**, unless you have a Workspace domain.
   - Fill in app name, your support email, and a developer email.
   - **Scopes:** add `https://www.googleapis.com/auth/gmail.readonly` — read-only, nothing else.
     Anything broader will fail verification and is more access than this feature needs.
   - **Test users:** add your own Google account. While the app is unverified only listed test
     users can sign in, which is fine indefinitely for personal use.
3. **APIs & Services → Credentials → Create credentials → OAuth client ID**:
   - Type **Android**
   - Package name: `com.moneymanager`
   - SHA-1: the fingerprint from step 0.
4. If you ever publish beyond test users, the read-only Gmail scope is a *restricted* scope and
   needs Google's verification review (a security questionnaire, and for restricted scopes
   possibly a third-party assessment). For personal use, stay on test users and skip all of it.

> **Send me:** the **package name and that the client is created**, plus confirmation the scope is
> `gmail.readonly`. There is no secret to hand over — an Android OAuth client has no client
> secret; it authenticates by package name plus signature.

---

## 2. Google Drive backup

**What it does once wired:** writes an encrypted backup of the database to the app's own private
Drive folder, and restores from it. Files in that folder are invisible in the user's Drive UI and
count against their quota, not yours.

**Free tier:** free. Uses the user's own 15 GB.

### Steps

1. Same Cloud project: **APIs & Services → Library → Google Drive API → Enable**.
2. On the same OAuth consent screen, add the scope
   `https://www.googleapis.com/auth/drive.appdata`.
   This is the app-data scope — it can only see files this app created and cannot read the
   user's documents. It is **not** a restricted scope, so it needs no verification review.
3. The Android OAuth client from step 1 covers this too. No second client needed.

> **Send me:** confirmation that the Drive API is enabled and `drive.appdata` is on the consent
> screen. Again, no secret.

---

## 3. Firebase sync and shared wallets

**What it does once wired:** multi-device sync, and shared wallets — which is also what unlocks
the opt-in leaderboard the Progress screen currently explains is unavailable.

**Free tier:** Firebase **Spark** — no card, no time limit. 1 GiB stored, 50k reads and 20k writes
a day. A household is nowhere near that.

> **Watch out:** Spark is quota-capped, not contractually free. If this were ever distributed to
> thousands of people sharing one Firebase project, it could cross into paid Blaze. Fine for
> personal and family use; worth knowing before wider release.

### Steps

1. <https://console.firebase.google.com/> → **Add project** → pick the **existing Cloud project**
   from step 0 rather than making a second one.
2. **Add app → Android**:
   - Package name `com.moneymanager`
   - SHA-1 from step 0
   - Download **`google-services.json`**.
3. **Build → Firestore Database → Create database → Production mode.**
4. **Authentication → Sign-in method → enable Google.**
5. Set security rules so a wallet is readable only by its members. Start from this and tighten:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /wallets/{walletId} {
      allow read, write: if request.auth != null
        && request.auth.uid in resource.data.members;
      match /{document=**} {
        allow read, write: if request.auth != null
          && request.auth.uid in get(/databases/$(database)/documents/wallets/$(walletId)).data.members;
      }
    }
  }
}
```

   Do not ship test-mode rules. They are open to the world and this is a database of someone's
   spending.

> **Send me:** the **`google-services.json`** file. It belongs at `app/google-services.json`.
>
> It is not a secret in the password sense — it ships inside every copy of the APK — but it does
> identify your project, so keep it out of a public repository. Add it to `.gitignore` and hand it
> to me as a file rather than pasting it.
>
> I will also need two lines added to the build: the `com.google.gms.google-services` plugin, and
> the Firebase BoM. I can make both changes once the file exists.

---

## What I will build when each arrives

| You provide | I build |
|---|---|
| Gmail OAuth client | Sign-in, message search for order/invoice mail, merchant + amount extraction, a pre-filled transaction the user confirms |
| `drive.appdata` enabled | Encrypted backup and restore, with a visible "last backed up" and a restore that asks twice |
| `google-services.json` | Firestore sync of the ledger, shared wallets, and the opt-in leaderboard the Progress screen is holding a place for |

Each can land independently — none blocks the others.

---

## What never to send

- Your keystore file, or any keystore password.
- A `client_secret.json`. If you are ever offered one, you picked **Web application** instead of
  **Android** when creating the OAuth client; delete it and make an Android client.
- A service-account key. Nothing here needs one, and a leaked one is a server-side compromise.
- Screenshots of the Cloud console that include project numbers you have not chosen to share.

The fingerprint from step 0 is not sensitive — it is derived from a public certificate.
