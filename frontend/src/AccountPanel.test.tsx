import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AccountPanel from './AccountPanel'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('account panel', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    window.history.replaceState({}, '', '/')
    fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(input.toString(), window.location.origin)
      if (url.pathname === '/api/auth/me') return new Response(null, { status: 401 })
      if (url.pathname === '/api/auth/google') return jsonResponse({ enabled: false })
      if (url.pathname === '/api/auth/csrf') return jsonResponse({ token: 'csrf-test-token' })
      if (url.pathname === '/api/auth/register') return jsonResponse({ message: 'Check your email for a verification link.' }, 202)
      return jsonResponse({})
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  it('registers with a CSRF-protected request and confirms email verification is required', async () => {
    render(<AccountPanel />)
    fireEvent.click(screen.getByRole('button', { name: 'Sign in / Create account' }))
    fireEvent.click(screen.getByRole('button', { name: 'New here? Create an account' }))

    fireEvent.change(screen.getByLabelText('Your name'), { target: { value: 'Avery' } })
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'avery@example.com' } })
    fireEvent.change(screen.getByLabelText(/Password/), { target: { value: 'a-strong-password-123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Check your email')
    const registration = fetchMock.mock.calls.find(([input]) =>
      new URL(input.toString(), window.location.origin).pathname === '/api/auth/register')
    expect(registration?.[1]).toMatchObject({
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'csrf-test-token' },
      body: JSON.stringify({ email: 'avery@example.com', displayName: 'Avery', password: 'a-strong-password-123' }),
    })
  })
})
