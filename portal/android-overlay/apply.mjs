import { readFile, writeFile, copyFile } from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = process.argv[2];
if (!root) throw new Error('Pass the directory containing the extracted AIBubble.zip project.');
const java = join(root,'app/src/main/java/com/aibubble');
const main = join(java,'MainActivity.kt');
const gradle = join(root,'app/build.gradle.kts');
const original = await readFile(main,'utf8');
const build = await readFile(gradle,'utf8');
const marker = '        root.addView(tv("1. Relay URL';
if (!original.includes(marker) || original.includes('PortalPairing') || !build.includes('versionCode = 3') || !build.includes('versionName = "0.3"'))
  throw new Error('Expected the unmodified AIBubble 0.3 archive.');
const ui = `        root.addView(tv("NexusBoard device registration"))
        root.addView(btn("Pair with NexusBoard") { PortalPairing.show(this) })
        root.addView(btn("Check NexusBoard pairing") { PortalPairing.check(this, true) })
        root.addView(btn("Forget local pairing") {
            android.app.AlertDialog.Builder(this)
                .setMessage("Clear the credential from this phone? Revoke the device in NexusBoard to invalidate server access.")
                .setPositiveButton("Forget") { _, _ -> PortalPairing.forget(this) }
                .setNegativeButton("Cancel", null)
                .show()
        })

`;
const patched = original.replace(marker,ui+marker)
  .replace('AI Bubble 0.3','AI Bubble 0.4 preview')
  .replace('        h.post(refresh)','        PortalPairing.check(this)\n        h.post(refresh)');
await copyFile(fileURLToPath(new URL('./PortalPairing.kt',import.meta.url)), join(java,'PortalPairing.kt'));
await writeFile(main,patched);
await writeFile(gradle,build.replace('versionCode = 3','versionCode = 4').replace('versionName = "0.3"','versionName = "0.4-connect-preview"'));
