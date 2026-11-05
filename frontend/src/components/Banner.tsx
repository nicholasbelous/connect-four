import type { GameStatus } from '../types'
import './Banner.css'

interface BannerProps {
  /** A finished status: RED_WON, YELLOW_WON or DRAW. */
  status: GameStatus
  moveCount: number
  onNewGame: () => void
  busy: boolean
}

export default function Banner({ status, moveCount, onNewGame, busy }: BannerProps) {
  const isDraw = status === 'DRAW'
  const winner = status === 'RED_WON' ? 'red' : 'yellow'

  return (
    <div
      className={`banner ${isDraw ? 'is-draw' : `is-win player-${winner}`}`}
      role="status"
      aria-live="polite"
    >
      <div className="banner-art" aria-hidden="true">
        {isDraw ? (
          <>
            <span className="banner-disc player-red" />
            <span className="banner-disc player-yellow" />
          </>
        ) : (
          <span className="banner-disc" />
        )}
      </div>

      <div className="banner-text">
        <h2 className="banner-title">
          {isDraw ? "It's a draw" : `${winner === 'red' ? 'Red' : 'Yellow'} wins!`}
        </h2>
        <p className="banner-subtitle">
          {isDraw ? 'The board is full and nobody connected four.' : `Four in a row on move ${moveCount}.`}
        </p>
      </div>

      <button type="button" className="btn btn-primary banner-button" onClick={onNewGame} disabled={busy}>
        Play again
      </button>
    </div>
  )
}
