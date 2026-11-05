import type { Player } from '../types'
import './StatusBar.css'

interface StatusBarProps {
  /** Whose turn it is, or null while the game is loading. */
  player: Player | null
  moveNumber: number
}

export default function StatusBar({ player, moveNumber }: StatusBarProps) {
  if (!player) {
    return (
      <div className="status-bar is-loading" role="status">
        <span className="turn-disc" />
        <span className="turn-text">Setting up the board…</span>
      </div>
    )
  }

  const name = player === 'RED' ? 'Red' : 'Yellow'

  return (
    <div className={`status-bar player-${player.toLowerCase()}`} role="status" aria-live="polite">
      {/* Keyed by player so the disc pops in again on every turn change. */}
      <span key={player} className="turn-disc" />
      <span className="turn-text">
        <strong>{name}</strong> to move
      </span>
      <span className="move-count">Move {moveNumber}</span>
    </div>
  )
}
