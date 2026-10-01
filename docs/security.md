# Security and privacy

## Privacy
- No account, no analytics, no server. Data stays on the device.
- The only network call is the optional update check, which asks GitHub for the latest release and sends none of your data. The app has the internet permission only for this.

## Locking
- **Unlocking** uses the system `BiometricPrompt`: fingerprint, face, or the device PIN/pattern/password. TaskNest never stores a password of its own for this.
- **Scope:** only items you lock are protected. The app opens normally.
- **Session:** an unlocked item stays open until the app leaves the foreground, then locks again.

## Encryption at rest
- Locked items' private text is encrypted before it is saved: a task's description, checklist and attachment list, and a note's content, checklist and attachment list.
- Algorithm: AES-256-GCM, key held in the Android Keystore under the alias `tasknest_lock_key`. The key is not exportable and is tied to the device.
- Titles, dates and the other task fields are **not** encrypted, so lists stay usable.

### Limits
- The key is not bound to a fresh authentication. The unlock prompt is enforced by the app, so someone who can run code as the app or has root can read it. This protects against casual access and against reading the stored data off the device, not against a compromised phone.
- Decrypted text of a locked item is kept in memory while the app is running.
- **Attachment files** (photos, videos, audio, files) are stored as ordinary files on the device and are **not** encrypted there. Only the list of attachments is.
- If the Keystore key is lost, for example when a cloud backup of the app data is restored to another phone, locked text comes back empty. Use the in-app backup to move data.
- Locked task titles can appear in notifications.
- Search can still match words in locked task descriptions.

## Exports
- Exporting locked items needs one authentication plus an **export password** (at least 6 characters).
- The password is turned into a key with PBKDF2 (HMAC-SHA256, 200,000 iterations, random 16-byte salt; SHA-1 on phones older than Android 8). Locked fields and their media are encrypted with AES-GCM. Media is encrypted in 64 KB authenticated chunks.
- Restoring asks for the password first. A wrong password or tampered data fails the check and nothing is imported.
- Unlocked items and their media are stored in the zip as normal.
- **The export password cannot be recovered.** A forgotten password means the locked items in that file are lost.

## Releases and updates
- Releases are signed with a fixed key supplied as repository secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`). Android only installs an update over an existing app when the signing key matches, so keep the keystore backed up and never commit it.
- Update checks only link to the GitHub release page. The app does not download or install APKs itself.

## Reporting a vulnerability
Open a private security advisory on the repository, or contact the maintainer directly. Please don't post details in a public issue.
