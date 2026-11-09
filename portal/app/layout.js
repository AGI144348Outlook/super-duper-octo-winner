import './style.css';

export const metadata = { title: 'AIBubble Connect', description: 'Your AI. Your Phone. Your World.' };

export default function RootLayout({ children }) {
  return <html lang="en"><body><a className="skip" href="#main">Skip to content</a>
    <header><a className="brand" href="/"><span aria-hidden="true">◉</span> AIBubble Connect</a>
      <nav aria-label="Main"><a href="/#download">Download</a><a href="/board">NexusBoard</a></nav></header>
    <main id="main">{children}</main>
    <footer><a href="https://github.com/AGI144348Outlook/super-duper-octo-winner">Source</a><a href="https://github.com/AGI144348Outlook/super-duper-octo-winner/blob/main/LICENSE">License</a></footer>
  </body></html>;
}
