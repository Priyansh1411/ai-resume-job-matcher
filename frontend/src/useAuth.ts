import { useCallback, useState } from 'react'
import { localStorageTokenStorage } from './tokenStorage'
import type { TokenStorage } from './tokenStorage'
import { AuthError, loginUser, logoutUser, registerUser } from './authApi'

export type AuthMode = 'login' | 'register'

export function useAuth(tokenStorage: TokenStorage = localStorageTokenStorage) {
  const [token, setToken] = useState<string | null>(() => tokenStorage.getToken())
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const submit = useCallback(
    async (mode: AuthMode, email: string, password: string): Promise<boolean> => {
      setIsSubmitting(true)
      setErrorMessage(null)

      try {
        const result = mode === 'login' ? await loginUser(email, password) : await registerUser(email, password)
        tokenStorage.setToken(result.token)
        setToken(result.token)
        return true
      } catch (err) {
        setErrorMessage(err instanceof AuthError ? err.message : 'Something went wrong. Please try again.')
        return false
      } finally {
        setIsSubmitting(false)
      }
    },
    [tokenStorage],
  )

  const logout = useCallback(() => {
    // Fire before clearing the token: logoutUser() reads it from storage
    // synchronously (before its first await), so calling it first guarantees
    // the server-side revocation request actually carries the token, even
    // though we don't wait for it - a network failure here must never block
    // the user from logging out locally.
    void logoutUser()
    tokenStorage.clearToken()
    setToken(null)
    setErrorMessage(null)
  }, [tokenStorage])

  const handleUnauthorized = useCallback(() => {
    tokenStorage.clearToken()
    setToken(null)
    setErrorMessage('Your session has expired. Please log in again.')
  }, [tokenStorage])

  return {
    isAuthenticated: token !== null,
    isSubmitting,
    errorMessage,
    submit,
    logout,
    handleUnauthorized,
  }
}
