const apk = 'https://raw.githubusercontent.com/AGI144348Outlook/super-duper-octo-winner/834a4b0d18f5120bddf3172021a4e5eb49483b8a/downloads/AIBubble-debug-2026-11-04.apk';

export default function Home() {
  return <>
    <section className="hero">
      <div className="kicker">ANDROID COMPANION</div>
      <h1>Your AI. Your Phone. Your World.</h1>
      <p>Find the existing AIBubble build, manage your own device registrations, and connect an authorized AI client in NexusBoard.</p>
      <div className="actions"><a className="button primary" href="#download">Download AIBubble</a><a className="button" href="/board#ai">Connect my AI</a></div>
    </section>
    <section id="download" className="panel">
      <div className="kicker">ANDROID DOWNLOAD</div><h2>Archived debug build</h2>
      <p>The November 4 build uses the original relay setup. NexusBoard pairing requires a compatible app build.</p>
      <a className="button primary" href={apk}>Download debug APK</a>
      <dl className="details"><div><dt>Version</dt><dd>0.3 (build 3)</dd></div><div><dt>Build date</dt><dd>November 4, 2026</dd></div><div><dt>Size</dt><dd>837,914 bytes</dd></div>
        <div><dt>SHA-256</dt><dd><code>30926cfb48b4a0e5c4c2c3a6af946869a30eaf3a7664800ac13d0e2c5021085e</code></dd></div></dl>
      <details><summary>Install on Android</summary><ol><li>Download the APK from the pinned GitHub commit.</li><li>Open the download and review the Android install prompt and app permissions.</li><li>If needed, grant your browser permission to install this file, then turn that permission off after installation.</li></ol></details>
      <details><summary>Build history</summary><p>The archived app includes separate ע (Sight) and פ (Sound) controls and original relay configuration. See <a href="https://github.com/AGI144348Outlook/super-duper-octo-winner/actions/runs/41755229384">the originating build</a> and <a href="https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/58">artifact provenance</a>.</p></details>
    </section>
    <section className="panel"><div className="kicker">PRIVATE COCKPIT</div><h2>Open NexusBoard</h2>
      <p>Sign in with your own account to manage device approval, credentials, and activity. Device, relay, and AI status are shown separately.</p>
      <a className="button" href="/board">Open NexusBoard</a>
    </section>
    <section className="panel"><div className="kicker">CONNECTION STATUS</div><h2>Check each connection</h2>
      <dl className="details"><div><dt>Portal API</dt><dd><a href="/api/connect/health">Check portal availability</a></dd></div>
        <div><dt>Android and device pairing</dt><dd>Sign in to see your phone’s status.</dd></div>
        <div><dt>MCP and AI client</dt><dd>Sign in to see the last successful tool call.</dd></div></dl>
    </section>
  </>;
}
