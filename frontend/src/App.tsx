import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, createGame, getGame, playMove } from './api'
import Banner from './components/Banner'
import Board from './components/Board'
import StatusBar from './components/StatusBar'
import type { Game, Position } from './types'
import './App.css'

type View =
  | { kind: 'loading' }
  | { kind: 'game'; game: Game }
  | { kind: 'not-found' }
  | { kind: 'error'; message: string }

const EMPTY_BOARD: null[][] = Array.from({ length: 6 }, () => Array<null>(7).fill(null))
const NOTICE_MS = 2600

/** "/" means start a new game; "/games/{id}" is a game; anything else is not found. */
function parsePath(pathname: string): 'new' | number | null {
  if (pathname === '/') {
    return 'new'
  }
  const match = /^\/games\/(\d+)\/?$/.exec(pathname)
  return match ? Number(match[1]) : null
}

function gamePath(id: number): string {
  return `/games/${id}`
}

/** Where the last move's disc landed: the top occupied cell of the last move's column. */
function lastMovePosition(game: Game): Position | null {
  const column = game.moves.at(-1)
  if (column === undefined) {
    return null
  }
  const row = game.board.findIndex((cells) => cells[column] !== null)
  return row === -1 ? null : { row, column }
}

function viewForPath(pathname: string): View {
  return parsePath(pathname) === null ? { kind: 'not-found' } : { kind: 'loading' }
}

export default function App() {
  const [view, setView] = useState<View>(() => viewForPath(window.location.pathname))
  const [droppedCell, setDroppedCell] = useState<Position | null>(null)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<{ id: number; message: string } | null>(null)

  // Refs, so rapid clicks and key presses see the latest values without waiting for a render.
  const busyRef = useRef(false)
  const initialLoadStarted = useRef(false)

  const flash = useCallback((message: string) => {
    setNotice({ id: Date.now(), message })
  }, [])

  useEffect(() => {
    if (!notice) {
      return
    }
    const timer = window.setTimeout(() => setNotice(null), NOTICE_MS)
    return () => window.clearTimeout(timer)
  }, [notice])

  const showGame = useCallback((game: Game, drop: Position | null) => {
    setDroppedCell(drop)
    setView({ kind: 'game', game })
  }, [])

  /** Creates or fetches the game for a path. Only sets state once the request settles. */
  const loadGame = useCallback(
    async (target: 'new' | number) => {
      try {
        if (target === 'new') {
          const game = await createGame()
          window.history.replaceState(null, '', gamePath(game.id))
          showGame(game, null)
        } else {
          showGame(await getGame(target), null)
        }
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          setView({ kind: 'not-found' })
        } else {
          setView({
            kind: 'error',
            message: error instanceof ApiError ? error.message : 'Something went wrong.',
          })
        }
      }
    },
    [showGame],
  )

  // Initial load. The ref stops React StrictMode's second effect run in development from
  // creating a second game for "/".
  useEffect(() => {
    const target = parsePath(window.location.pathname)
    if (target !== null && !initialLoadStarted.current) {
      initialLoadStarted.current = true
      void loadGame(target)
    }
  }, [loadGame])

  // Back/forward between games.
  useEffect(() => {
    const onPopState = () => {
      const target = parsePath(window.location.pathname)
      setView(viewForPath(window.location.pathname))
      if (target !== null) {
        void loadGame(target)
      }
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [loadGame])

  const startNewGame = useCallback(async () => {
    if (busyRef.current) {
      return
    }
    busyRef.current = true
    setBusy(true)
    try {
      const game = await createGame()
      window.history.pushState(null, '', gamePath(game.id))
      showGame(game, null)
    } catch {
      flash('Could not start a new game. Please try again.')
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }, [showGame, flash])

  const game = view.kind === 'game' ? view.game : null

  const play = useCallback(
    async (column: number) => {
      if (!game || game.status !== 'IN_PROGRESS' || busyRef.current) {
        return
      }
      busyRef.current = true
      setBusy(true)
      try {
        const next = await playMove(game.id, column, game.nextMoveNumber)
        showGame(next, lastMovePosition(next))
      } catch (error) {
        if (error instanceof ApiError && error.status === 409) {
          // Our copy was stale (e.g. the game moved on in another tab): show the server's state.
          try {
            showGame(await getGame(game.id), null)
          } catch {
            flash('Could not refresh the game. Please reload the page.')
          }
        } else if (error instanceof ApiError && error.status === 422) {
          // Columns are always in range and input stops once the game is over, so a 422
          // from a column click means the column is full. (The server's message uses the
          // API's 0-based column numbers, which don't match the 1-7 shown to players.)
          flash('That column is full.')
        } else {
          flash('Something went wrong. Please try again.')
        }
      } finally {
        busyRef.current = false
        setBusy(false)
      }
    },
    [game, showGame, flash],
  )

  // Keys 1-7 drop a disc in that column.
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.repeat || event.altKey || event.ctrlKey || event.metaKey) {
        return
      }
      const column = Number(event.key) - 1
      if (Number.isInteger(column) && column >= 0 && column < 7) {
        void play(column)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [play])

  const isOver = game !== null && game.status !== 'IN_PROGRESS'

  return (
    <div className="app">
      <header className="topbar">
        <h1 className="brand">
          <span className="brand-mark" aria-hidden="true">
            <span className="player-red" />
            <span className="player-yellow" />
          </span>
          Connect Four
        </h1>
        <button type="button" className="btn" onClick={() => void startNewGame()} disabled={busy}>
          <span className="btn-icon" aria-hidden="true">
            +
          </span>
          New game
        </button>
      </header>

      {view.kind === 'not-found' || view.kind === 'error' ? (
        <main className="message-screen">
          <div className="empty-slot" aria-hidden="true" />
          <h2>{view.kind === 'not-found' ? 'Game not found' : "Couldn't load the game"}</h2>
          <p>
            {view.kind === 'not-found'
              ? "There's no game at this address. Start a fresh one instead."
              : view.message}
          </p>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => void startNewGame()}
            disabled={busy}
          >
            New game
          </button>
        </main>
      ) : (
        <main className="stage">
          <div className="status-slot">
            {game && isOver ? (
              <Banner
                key={game.id}
                status={game.status}
                moveCount={game.moves.length}
                onNewGame={() => void startNewGame()}
                busy={busy}
              />
            ) : (
              <StatusBar player={game?.currentPlayer ?? null} moveNumber={game?.nextMoveNumber ?? 1} />
            )}
          </div>

          <Board
            key={game?.id ?? 'loading'}
            board={game?.board ?? EMPTY_BOARD}
            currentPlayer={game?.currentPlayer ?? null}
            winningCells={game?.winningCells ?? []}
            droppedCell={droppedCell}
            interactive={game !== null && !isOver}
            onPlay={(column) => void play(column)}
          />

          {game && !isOver && (
            <p className="hint">
              Click a column or press <kbd>1</kbd>–<kbd>7</kbd>
            </p>
          )}
        </main>
      )}

      {notice && (
        <div key={notice.id} className="toast" role="alert">
          {notice.message}
        </div>
      )}
    </div>
  )
}
