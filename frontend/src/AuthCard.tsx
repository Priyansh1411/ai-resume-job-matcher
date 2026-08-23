import { useState } from 'react'
import type { FormEvent } from 'react'
import type { AuthMode } from './useAuth'

type AuthCardProps = {
  isSubmitting: boolean
  errorMessage: string | null
  onSubmit: (mode: AuthMode, email: string, password: string) => void
}

function AuthCard({ isSubmitting, errorMessage, onSubmit }: AuthCardProps) {
  const [mode, setMode] = useState<AuthMode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSubmit(mode, email, password)
  }

  function handleModeChange(nextMode: AuthMode) {
    setMode(nextMode)
  }

  const isLogin = mode === 'login'

  return (
    <div className="card">
      <h2>{isLogin ? 'Log In' : 'Create Account'}</h2>
      <p className="card__subtitle">
        {isLogin ? 'Log in to upload and match your resume.' : 'Create an account to get started.'}
      </p>

      <form onSubmit={handleSubmit}>
        <label className="visually-hidden" htmlFor="auth-email">
          Email
        </label>
        <input
          id="auth-email"
          className="text-input"
          type="email"
          placeholder="Email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />

        <label className="visually-hidden" htmlFor="auth-password">
          Password
        </label>
        <input
          id="auth-password"
          className="text-input"
          type="password"
          placeholder="Password"
          autoComplete={isLogin ? 'current-password' : 'new-password'}
          minLength={isLogin ? undefined : 8}
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        {errorMessage && (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        )}

        <button type="submit" className="btn btn--primary btn--full" disabled={isSubmitting}>
          {isSubmitting ? 'Please wait...' : isLogin ? 'Log In' : 'Create Account'}
        </button>
      </form>

      <button type="button" className="auth-toggle" onClick={() => handleModeChange(isLogin ? 'register' : 'login')}>
        {isLogin ? "Don't have an account? Create one" : 'Already have an account? Log in'}
      </button>
    </div>
  )
}

export default AuthCard
