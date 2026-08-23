export type TokenStorage = {
  getToken(): string | null
  setToken(token: string): void
  clearToken(): void
}

const TOKEN_KEY = 'resumeMatch.authToken'

export const localStorageTokenStorage: TokenStorage = {
  getToken() {
    return window.localStorage.getItem(TOKEN_KEY)
  },
  setToken(token: string) {
    window.localStorage.setItem(TOKEN_KEY, token)
  },
  clearToken() {
    window.localStorage.removeItem(TOKEN_KEY)
  },
}
