import type { ApiErrorBody, Game } from './types'

/** An error response from the API, or status 0 when the server couldn't be reached. */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      },
    })
  } catch {
    throw new ApiError(0, 'Could not reach the server')
  }

  if (!response.ok) {
    let message = response.statusText || 'Request failed'
    try {
      const body = (await response.json()) as Partial<ApiErrorBody>
      if (body.message) {
        message = body.message
      }
    } catch {
      // Not our JSON error format (e.g. a proxy error page); keep the status text.
    }
    throw new ApiError(response.status, message)
  }

  return (await response.json()) as T
}

export function createGame(): Promise<Game> {
  return request<Game>('/games', { method: 'POST' })
}

export function getGame(id: number): Promise<Game> {
  return request<Game>(`/games/${id}`)
}

export function playMove(id: number, column: number, moveNumber: number): Promise<Game> {
  return request<Game>(`/games/${id}/moves`, {
    method: 'POST',
    body: JSON.stringify({ column, moveNumber }),
  })
}
