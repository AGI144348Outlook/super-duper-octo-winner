import Board from './board.js';

export const metadata = { robots: { index: false, follow: false } };

export default function BoardPage() {
  return <><div className="kicker">AIBUBBLE / PRIVATE WORKSPACE</div><h1>NexusBoard</h1><Board /></>;
}
