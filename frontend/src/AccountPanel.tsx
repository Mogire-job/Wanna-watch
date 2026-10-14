import { useEffect, useRef, useState, type FormEvent } from 'react'
import { registerAccount, signIn, signOut, verifyAccount, type Account } from './api'

type Mode = 'login' | 'register'

export default function AccountPanel() {
  const [account, setAccount] = useState<Account | null>(null)
  const [mode, setMode] = useState<Mode>('login')
  const [open, setOpen] = useState(false)
  const [googleEnabled, setGoogleEnabled] = useState(false)
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const verificationStarted = useRef(false)

  useEffect(() => {
    fetch('/api/auth/me')
      .then(async (response) => {
        if (response.status === 401) return null
        if (!response.ok) throw new Error('Could not check your account status.')
        return response.json() as Promise<Account>
      })
      .then(setAccount)
      .catch((reason: Error) => setError(reason.message))

    fetch('/api/auth/google')
      .then((response) => response.ok ? response.json() as Promise<{ enabled: boolean }> : null)
      .then((settings) => setGoogleEnabled(settings?.enabled ?? false))
      .catch(() => setGoogleEnabled(false))

    const params = new URLSearchParams(window.location.search)
    const verificationToken = params.get('verify')
    if (verificationToken && !verificationStarted.current) {
      verificationStarted.current = true
      setBusy(true)
      verifyAccount(verificationToken)
        .then((result) => {
          setMode('login')
          setOpen(true)
          setMessage(result.message)
          setError('')
        })
        .catch((reason: Error) => {
          setOpen(true)
          setError(reason.message)
        })
        .finally(() => setBusy(false))
      params.delete('verify')
      const search = params.toString()
      window.history.replaceState({}, '', `${window.location.pathname}${search ? `?${search}` : ''}${window.location.hash}`)
    }
    if (params.get('auth') === 'google-error') {
      setOpen(true)
      setError('Google sign-in could not be completed. Check your Google OAuth setup and verify the account email.')
      params.delete('auth')
      const search = params.toString()
      window.history.replaceState({}, '', `${window.location.pathname}${search ? `?${search}` : ''}${window.location.hash}`)
    }
  }, [])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setMessage('')
    try {
      if (mode === 'register') {
        const result = await registerAccount({ email, displayName, password })
        setMessage(result.message)
      } else {
        setAccount(await signIn({ email, password }))
        setOpen(false)
      }
      setPassword('')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Account request failed.')
    } finally {
      setBusy(false)
    }
  }

  async function logout() {
    setBusy(true)
    setError('')
    try {
      await signOut()
      setAccount(null)
      setOpen(false)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not sign out.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="account-shell">
      {account ? (
        <div className="account-session">
          <span>Signed in as <strong>{account.displayName || account.email}</strong></span>
          <button className="account-toggle" type="button" onClick={logout} disabled={busy}>Sign out</button>
        </div>
      ) : (
        <button className="account-toggle" type="button" onClick={() => { setOpen((value) => !value); setError(''); setMessage('') }}>
          {open ? 'Close account' : 'Sign in / Create account'}
        </button>
      )}

      {open && !account && (
        <section className="account-panel" aria-label="Your Wanna Watch account">
          <div className="account-heading">
            <div>
              <p className="eyebrow">YOUR ACCOUNT</p>
              <h2>{mode === 'register' ? 'Join Wanna Watch' : 'Welcome back'}</h2>
            </div>
            <button className="account-close" type="button" onClick={() => setOpen(false)} aria-label="Close account form">×</button>
          </div>
          <form onSubmit={submit}>
            {mode === 'register' && (
              <label>
                Your name
                <input autoComplete="name" required maxLength={120} value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
              </label>
            )}
            <label>
              Email address
              <input type="email" autoComplete="email" required maxLength={254} value={email} onChange={(event) => setEmail(event.target.value)} />
            </label>
            <label>
              Password
              <input
                type="password"
                autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                required
                minLength={mode === 'register' ? 12 : 1}
                maxLength={72}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              {mode === 'register' && <small>Use at least 12 characters. We’ll email you a verification link.</small>}
            </label>
            {error && <p className="account-message account-error" role="alert">{error}</p>}
            {message && <p className="account-message" role="status">{message}</p>}
            <button className="account-submit" type="submit" disabled={busy}>
              {busy ? 'Please wait…' : mode === 'register' ? 'Create account' : 'Sign in'}
            </button>
          </form>
          <button
            className="account-switch"
            type="button"
            onClick={() => {
              setMode((value) => value === 'login' ? 'register' : 'login')
              setError('')
              setMessage('')
            }}
          >
            {mode === 'register' ? 'Already have an account? Sign in' : 'New here? Create an account'}
          </button>
          {googleEnabled && (
            <a className="google-signin" href="/oauth2/authorization/google">Continue with Google</a>
          )}
        </section>
      )}
    </div>
  )
}
