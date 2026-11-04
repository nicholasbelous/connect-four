# Design Decisions

## Scope

- Two players, local hotseat, on one screen
- 7 columns x 6 rows
- Red moves first
- Win: four in a row horizontally, vertically, or diagonally
- Draw: the board is full with no winner
- Game state persists, so a page refresh resumes the game

## Architecture

- Server-authoritative: the backend owns all rules and state; the frontend only renders server state and sends move requests
- Game rules live in a pure Java package with no Spring dependencies, so they're unit-testable
- Moves are stored as an ordered log; game state is derived from the move log

## API

- `POST /api/games` -> `201`, returns new game state
- `GET /api/games/{id}` -> `200` with game state; `404` if unknown
  - Game state: board, current player, status, move list, next move number, winning cells (if any)
- `POST /api/games/{id}/moves`, body `{column, moveNumber}`
  - `200` with updated state
  - `400` if the request body is malformed (e.g. missing or non-integer fields)
  - `409` if `moveNumber` is stale or out of order
  - `422` if the move is illegal (column full, column out of range, game already over)
  - `404` if unknown game
- One consistent JSON error format for all errors

## Data model

- `games`: `id`, `status` (`IN_PROGRESS`, `RED_WON`, `YELLOW_WON`, `DRAW`), `created_at`, `updated_at`
  - `status` is a cached copy of state derived from the move log, updated in the same transaction as each move insert, so reads don't replay the log
- `moves`: `game_id`, `move_number`, `player`, `column_index`, `created_at`
  - Primary key `(game_id, move_number)` guarantees one move per move number in a game
  - Ordering is enforced by the server's `moveNumber` check; the key is a backstop against concurrent requests

## Out of scope (next steps)

- Online multiplayer (WebSockets on top of the move log)
- Computer opponent
- Undo / replay
- Frontend tests
