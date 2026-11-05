export type Player = 'RED' | 'YELLOW'

export type GameStatus = 'IN_PROGRESS' | 'RED_WON' | 'YELLOW_WON' | 'DRAW'

export interface Position {
  row: number
  column: number
}

/** Game state as returned by the API. The server owns all rules; the UI only renders this. */
export interface Game {
  id: number
  /** Rows top to bottom; each cell is a player or null when empty. */
  board: (Player | null)[][]
  /** Null unless the game is in progress. */
  currentPlayer: Player | null
  status: GameStatus
  /** The column of each move, in order. */
  moves: number[]
  nextMoveNumber: number
  winningCells: Position[]
}

/** The API's error body for every error status. */
export interface ApiErrorBody {
  status: number
  error: string
  message: string
}
