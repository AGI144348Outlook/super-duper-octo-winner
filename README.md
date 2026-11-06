# AIBubble / NexusPortal — Android project

This repository contains the AIBubble Android project archive, Android build
workflows, and other project experiments.

## Download AIBubble for Android

**[Download AIBubble APK (November 4, 2026 debug build)](downloads/AIBubble-debug-2026-11-04.apk)**

Direct GitHub download:
https://raw.githubusercontent.com/AGI144348Outlook/super-duper-octo-winner/main/downloads/AIBubble-debug-2026-11-04.apk

The versioned APK is kept on the repository's `main` branch instead of only in
an expiring Actions artifact. The download remains available while the public
repository and file remain hosted; it is not an unconditional archival guarantee.

Build provenance: [GitHub Actions run 41755229384](https://github.com/AGI144348Outlook/super-duper-octo-winner/actions/runs/41755229384),
artifact `aibubble-debug-apk` (created November 4, 2026).

APK SHA-256:
`30926cfb48b4a0e5c4c2c3a6af946869a30eaf3a7664800ac13d0e2c5021085e`

### Install on an Android phone

1. Open the download link in your Android browser and save the `.apk` file.
2. Open the download. Android may ask you to enable **Install unknown apps** for
   that browser/file manager; enable it only if you trust the file.
3. Review permissions and install. You can switch the unknown-app installation
   permission back off afterward.

This is a **debug-signed build** from an existing workflow, not a Play Store
release. It has not been independently security-audited or tested on every
device. If Android refuses to update an existing installation due to a
different signing certificate, avoid uninstalling until you have backed up any
important app data.

The pre-existing `AIBubble.zip` is the **source-project archive**, not an
installable Android application. APKs produced by Actions alone have an expiry
date; the file in `downloads/` does not depend on Actions retention.

## Licensing

**Free for noncommercial hobbyists, learners, educators, and researchers:**
use, study, copy, modify, build, and share with attribution and the license.

**Commercial use requires a separately negotiated and signed written
agreement before monetizing or commercially exploiting the code or APK.**

Read [LICENSE](LICENSE) and
[COMMERCIAL-LICENSING.md](COMMERCIAL-LICENSING.md).
This is a **custom source-available noncommercial license**, not an
OSI-approved open-source license. Third-party dependencies keep their own terms.

To ask about a commercial license, open a
[repository issue](https://github.com/AGI144348Outlook/super-duper-octo-winner/issues).

## Build and verification history

The existing GitHub Actions workflow `.github/workflows/aibubble.yml` builds
`AIBubble.zip`. Publication checks and provenance for the stable APK are
recorded in [Issue #58](https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/58).
