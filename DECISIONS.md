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
  - `status` is stored so a game's outcome can be queried directly in SQL without replaying moves; it's written in the same transaction as each move insert. The API still derives game state by replaying the move log
- `moves`: `game_id`, `move_number`, `player`, `column_index`, `created_at`
  - Primary key `(game_id, move_number)` guarantees one move per move number in a game
  - Ordering is enforced by the server's `moveNumber` check. Moves on a game are serialized with a row lock (`SELECT ... FOR UPDATE` on the game), which prevents a deadlock found when testing simultaneous moves against MySQL; the primary key remains a backstop

## Trade-offs

- Connect Four over chess: a small ruleset left time for a polished core (rules, persistence, API, UI, deployment) rather than a wide but thin one
- Spring Boot 4.1 over 3.5: 3.5's open-source support ended on 2026-06-30
- React over Vue: faster to build with inside the time box
- oxlint over ESLint: the current Vite React template ships oxlint; kept it rather than add dependencies
- Local hotseat over live multiplayer: the move log is the foundation for adding it, since each new move could be broadcast to other clients
- The frontend shows its own wording for a full column ("That column is full.") because the API's messages use 0-based column numbers, while players see columns 1–7

## Verification

- Game rule tests were checked by deliberately introducing three bugs (one-way win detection, draw checked before win, full column checked before game over); each made tests fail
- The draw test proves its board is a draw with an independent brute-force scan for four in a row, not the game's own win detection
- Simultaneous moves were load-tested against real MySQL (10 identical requests at once). This found a deadlock that returned 500s; after adding the row lock, every round gave one 200 and nine 409s
- The UI was checked end to end in headless Chrome: drop animation, ghost preview, win highlight and banner, refresh resume, 409 and 422 handling, not-found screen, reduced motion, and a 375px phone layout
- The Kubernetes manifests were tested on kind: all pods became ready, and a game was created and played through the frontend

## Out of scope (next steps)

- Online multiplayer (WebSockets on top of the move log)
- Computer opponent
- Undo / replay
- Frontend tests
