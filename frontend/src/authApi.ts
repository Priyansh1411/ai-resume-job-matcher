import { extractErrorMessage } from './apiErrors'

export type AuthResult = {
  token: string
}

export class AuthError extends Error {}

function isAuthResult(value: unknown): value is AuthResult {
  if (!value || typeof value !== 'object') {
    return false
  }
  return typeof (value as Record<string, unknown>).token === 'string'
}

async function requestToken(
  path: string,
  email: string,
  password: string,
  defaultErrorMessage: string,
): Promise<AuthResult> {
  let response: Response
  try {
    response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
  } catch {
    throw new AuthError('Could not reach the server. Please check your connection and try again.')
  }

  let data: unknown = null
  try {
    data = await response.json()
  } catch {
    data = null
  }

  if (!response.ok) {
    throw new AuthError(extractErrorMessage(data) ?? defaultErrorMessage)
  }

  if (!isAuthResult(data)) {
    throw new AuthError('Received an unexpected response from the server.')
  }

  return data
}

export function registerUser(email: string, password: string): Promise<AuthResult> {
  return requestToken('/api/auth/register', email, password, 'Could not create an account. Please try again.')
}

export function loginUser(email: string, password: string): Promise<AuthResult> {
  return requestToken('/api/auth/login', email, password, 'Could not log in. Please try again.')
}
