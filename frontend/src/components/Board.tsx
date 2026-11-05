import { useId, useState, type CSSProperties } from 'react'
import type { Player, Position } from '../types'
import './Board.css'

interface BoardProps {
  /** Rows top to bottom, as returned by the API. */
  board: (Player | null)[][]
  currentPlayer: Player | null
  winningCells: Position[]
  /** The disc to animate falling in, or null to show the board as-is (e.g. after a refresh). */
  droppedCell: Position | null
  /** Whether the board accepts moves (game in progress and loaded). */
  interactive: boolean
  onPlay: (column: number) => void
}

// Board face geometry in SVG units: one cell is 100 units. Board.css mirrors these ratios.
const CELL = 100
const PAD = 18
const HOLE_RADIUS = 41
const CORNER_RADIUS = 30

function playerClass(player: Player): string {
  return player === 'RED' ? 'player-red' : 'player-yellow'
}

function playerName(player: Player): string {
  return player === 'RED' ? 'Red' : 'Yellow'
}

export default function Board({
  board,
  currentPlayer,
  winningCells,
  droppedCell,
  interactive,
  onPlay,
}: BoardProps) {
  const [hoveredColumn, setHoveredColumn] = useState<number | null>(null)

  const rows = board.length
  const columns = board[0]?.length ?? 0
  const winning = new Set(winningCells.map((cell) => `${cell.row}:${cell.column}`))
  const winner = winningCells.length > 0 ? board[winningCells[0].row][winningCells[0].column] : null

  const discs = []
  for (let row = 0; row < rows; row++) {
    for (let column = 0; column < columns; column++) {
      const player = board[row][column]
      if (!player) {
        continue
      }
      const isDropping = droppedCell?.row === row && droppedCell.column === column
      const isWinning = winning.has(`${row}:${column}`)
      discs.push(
        <div
          key={`${row}:${column}`}
          className={[
            'disc',
            playerClass(player),
            isDropping ? 'dropping' : '',
            isWinning ? 'winning' : '',
          ].join(' ')}
          style={{ '--row': row, '--col': column } as CSSProperties}
        />,
      )
    }
  }

  const showGhost = interactive && currentPlayer !== null && hoveredColumn !== null

  return (
    <div
      className={['board-area', winner ? 'has-winner' : '', interactive ? 'is-interactive' : '']
        .join(' ')}
      style={{ '--rows': rows, '--columns': columns } as CSSProperties}
    >
      <div className="board">
        <div className="board-back" />

        {currentPlayer && (
          <div
            // Remounted after each drop so it can wait for the falling disc to land
            // before fading back in, instead of hovering on top of it.
            key={droppedCell ? `after-${droppedCell.row}:${droppedCell.column}` : 'ghost'}
            className={[
              'disc',
              'ghost',
              playerClass(currentPlayer),
              showGhost ? 'visible' : '',
              droppedCell ? 'after-drop' : '',
            ].join(' ')}
            style={
              {
                '--col': hoveredColumn ?? Math.floor(columns / 2),
                '--drop-row': droppedCell?.row ?? 0,
              } as CSSProperties
            }
            aria-hidden="true"
          />
        )}

        <div className="discs" aria-hidden="true">
          {discs}
        </div>

        <BoardFace rows={rows} columns={columns} />

        {winner && (
          <div className={`win-rings ${playerClass(winner)}`} aria-hidden="true">
            {winningCells.map((cell) => (
              <div
                key={`${cell.row}:${cell.column}`}
                className="win-ring"
                style={{ '--row': cell.row, '--col': cell.column } as CSSProperties}
              />
            ))}
          </div>
        )}

        <div className="column-hits" role="group" aria-label="Board columns">
          {Array.from({ length: columns }, (_, column) => {
            const full = board[0][column] !== null
            const label =
              `Drop ${currentPlayer ? `a ${playerName(currentPlayer).toLowerCase()}` : 'a'} disc` +
              ` in column ${column + 1}${full ? ' (full)' : ''}`
            return (
              <button
                key={column}
                type="button"
                className="column-hit"
                style={{ '--col': column } as CSSProperties}
                aria-label={label}
                aria-disabled={!interactive}
                onClick={() => {
                  if (interactive) {
                    onPlay(column)
                  }
                }}
                onPointerEnter={() => setHoveredColumn(column)}
                onPointerLeave={() =>
                  setHoveredColumn((current) => (current === column ? null : current))
                }
                onFocus={() => setHoveredColumn(column)}
                onBlur={() => setHoveredColumn((current) => (current === column ? null : current))}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
}

/** The blue board face: a gradient panel with the holes masked out, plus bevels. */
function BoardFace({ rows, columns }: { rows: number; columns: number }) {
  // useId can contain characters that don't work inside url(#...), so keep it simple.
  const id = `board-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
  const width = columns * CELL + 2 * PAD
  const height = rows * CELL + 2 * PAD

  const holes: { x: number; y: number }[] = []
  for (let row = 0; row < rows; row++) {
    for (let column = 0; column < columns; column++) {
      holes.push({ x: PAD + column * CELL + CELL / 2, y: PAD + row * CELL + CELL / 2 })
    }
  }

  return (
    <svg
      className="board-face"
      viewBox={`0 0 ${width} ${height}`}
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id={`${id}-face`} x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0" stopColor="#4a76ff" />
          <stop offset="0.45" stopColor="#2449d8" />
          <stop offset="1" stopColor="#152c8c" />
        </linearGradient>
        <linearGradient id={`${id}-gloss`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#ffffff" stopOpacity="0.22" />
          <stop offset="0.35" stopColor="#ffffff" stopOpacity="0.04" />
          <stop offset="1" stopColor="#ffffff" stopOpacity="0" />
        </linearGradient>
        <mask id={`${id}-holes`} maskUnits="userSpaceOnUse" x="0" y="0" width={width} height={height}>
          <rect width={width} height={height} fill="white" />
          {holes.map((hole) => (
            <circle key={`${hole.x}:${hole.y}`} cx={hole.x} cy={hole.y} r={HOLE_RADIUS} fill="black" />
          ))}
        </mask>
      </defs>

      <g mask={`url(#${id}-holes)`}>
        <rect width={width} height={height} rx={CORNER_RADIUS} fill={`url(#${id}-face)`} />
        <rect width={width} height={height} rx={CORNER_RADIUS} fill={`url(#${id}-gloss)`} />
      </g>

      {/* Bevel: a dark lip just inside each hole and a soft highlight on its lower edge. */}
      {holes.map((hole) => (
        <g key={`${hole.x}:${hole.y}`}>
          <circle
            cx={hole.x}
            cy={hole.y}
            r={HOLE_RADIUS - 1.5}
            fill="none"
            stroke="rgba(4, 10, 40, 0.55)"
            strokeWidth="4"
          />
          <circle
            cx={hole.x}
            cy={hole.y + 1.5}
            r={HOLE_RADIUS + 2}
            fill="none"
            stroke="rgba(255, 255, 255, 0.12)"
            strokeWidth="1.5"
          />
        </g>
      ))}

      {/* Inner frame highlight */}
      <rect
        x="1.5"
        y="1.5"
        width={width - 3}
        height={height - 3}
        rx={CORNER_RADIUS - 1.5}
        fill="none"
        stroke="rgba(255, 255, 255, 0.2)"
        strokeWidth="3"
      />
    </svg>
  )
}
