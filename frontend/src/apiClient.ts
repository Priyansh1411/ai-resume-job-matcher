import { localStorageTokenStorage } from './tokenStorage'
import type { TokenStorage } from './tokenStorage'

/**
 * Thrown when a protected call comes back 401/403 rejected by Spring
 * Security's filter chain before it reached a controller - i.e. the token is
 * missing, malformed, or expired - rather than a controller-level 403 like
 * "not authorized to access this resume", which should surface as a normal
 * error, not a forced logout.
 *
 * Both cases return the same status code and a JSON body with an `error`
 * string, so status alone can't tell them apart. What does: every
 * domain-level rejection in this app's controllers returns exactly
 * `{ error: string }` (see e.g. ResumeMatchController's exception handlers),
 * while a request Spring Security itself rejects never reaches a controller
 * and instead gets Spring Boot's default error page - which also carries
 * `status` and `path` fields. That combination is what's checked for below.
 */
export class UnauthorizedError extends Error {}

export type ProtectedFetchOptions = {
  method?: string
  headers?: Record<string, string>
  body?: BodyInit
}

export type ProtectedFetchResult = {
  response: Response
  data: unknown
}

function isFrameworkErrorPage(value: unknown): boolean {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Record<string, unknown>
  return typeof candidate.status === 'number' && typeof candidate.path === 'string'
}

export function createProtectedFetch(tokenStorage: TokenStorage) {
  return async function protectedFetch(
    path: string,
    options: ProtectedFetchOptions = {},
  ): Promise<ProtectedFetchResult> {
    const token = tokenStorage.getToken()
    const headers: Record<string, string> = { ...options.headers }
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body,
    })

    let data: unknown = null
    try {
      data = await response.json()
    } catch {
      data = null
    }

    if (
      !response.ok &&
      (response.status === 401 || response.status === 403) &&
      (data === null || isFrameworkErrorPage(data))
    ) {
      throw new UnauthorizedError('Your session has expired. Please log in again.')
    }

    return { response, data }
  }
}

export const protectedFetch = createProtectedFetch(localStorageTokenStorage)
