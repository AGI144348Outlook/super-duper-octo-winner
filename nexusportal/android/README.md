# NexusPortal Android shell — initial native milestone

Native Java Android shell for the system-level overlay, independent of the browser demo. No AI provider connection and no MediaProjection capture yet.

The app opens a screen with **Start overlay** and **Stop overlay**. Start asks the user to grant Android's Draw over other apps permission. After permission is granted, the app starts a foreground service with a draggable-looking floating `%` bubble (tap toggles a transparent touch-through annotation layer). Stop removes both windows and ends the service. A persistent notification provides another Stop action. This is a first native scaffold, not a production-safe release.

**Security:** no screen content is collected, no microphone permission, no network permission, no AI connection, and no keystore committed. The overlay is `FLAG_NOT_TOUCHABLE`; only the bubble receives taps. On Android 12+, untrusted overlay touch restrictions still apply to underlying applications.

**Build:** the opt-in `NexusPortal Android APK` GitHub Actions workflow assembles a debug APK; download the artifact from the run. See [pre-test Issue #2](https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/2). On-device validation is still required.
