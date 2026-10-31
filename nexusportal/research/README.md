# Mashet Pad extraction notes

Source: user-supplied `mashet-pad.zip`, labeled v0.2. Inspected `README.md`, `LOGIC-MAP.md`, `PadBridge.java`, `MainActivity.java`, and APK workflow. The source ZIP is **not** committed here.

| Source pattern | NexusPortal abstraction | Boundary |
| --- | --- | --- |
| WebView loads local HTML and JS bridge | Shared browser/Android renderer | Restrict native bridge to trusted origins and explicit capabilities |
| Logical order vs bidi display order | Stable symbol IDs with visual placement adapter | Unicode layout does not define semantic meaning |
| `(_)ⁿ` viewer and timeline | Inspectable annotation/symbol history | Do not infer cognition from replay alone |
| 96 glyphs in six layers | Optional structured symbolic registry | User-defined meanings need explicit provenance |
| GitHub Actions APK build | Future Android build pipeline | No workflow imported; source keystore must never be copied |

Source README **reports** 108/108 bidi comparisons and states native Java was stub-compiled, not built against Android SDK. Those results have not been independently reproduced here.

See [Issue #1](https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/1) for the deeper research agenda. Any future GitHub tests require a published pre-test entry and follow-up evidence.
