import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function createResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('discovery page', () => {
  const title = {
    id: 1,
    type: 'MOVIE',
    name: 'Dune',
    releaseYear: 2021,
    synopsis: 'A young heir journeys to a distant world.',
    posterUrl: null,
    rating: 8.1,
    genres: ['Drama', 'Science Fiction'],
    trailerKey: 'trailer-key',
    availability: [],
  }

  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(input.toString(), window.location.origin)
      if (url.pathname === '/api/auth/me') return new Response(null, { status: 401 })
      if (url.pathname === '/api/auth/google') return createResponse({ enabled: false })
      if (url.pathname === '/api/genres') return createResponse(['Drama', 'Science Fiction'])
      if (url.pathname === '/api/countries') {
        return createResponse([{ code: 'US', name: 'United States' }, { code: 'GB', name: 'United Kingdom' }])
      }
      if (url.pathname === '/api/providers') return createResponse([{ name: 'Netflix', logoUrl: null }])
      if (url.pathname === '/api/titles') {
        return createResponse({ items: [title], page: 0, size: 20, totalItems: 1, totalPages: 1 })
      }
      return createResponse([])
    })
    vi.stubGlobal('fetch', fetchMock)
  })

  it('shows titles with the visitor’s selected market', async () => {
    render(<App />)

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input]) => {
        const url = new URL(input.toString(), window.location.origin)
        return url.pathname === '/api/titles' && url.searchParams.get('country') === 'US'
      })).toBe(true)
    })
    expect(await screen.findByRole('heading', { name: /Dune/ })).toBeInTheDocument()
    expect(screen.getByLabelText('Choose your country or region')).toHaveValue('US')
    expect(screen.getByText(/Showing options for United States/)).toBeInTheDocument()
    expect(screen.getByText(/This product uses the TMDB API/)).toBeInTheDocument()
  })

  it('reloads the feed with the selected genre and country', async () => {
    render(<App />)
    await screen.findByRole('heading', { name: /Dune/ })

    fireEvent.change(screen.getByLabelText('Filter by genre'), { target: { value: 'Drama' } })
    fireEvent.change(screen.getByLabelText('Choose your country or region'), { target: { value: 'GB' } })

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input]) => {
        const url = new URL(input.toString(), window.location.origin)
        return url.pathname === '/api/titles'
          && url.searchParams.get('genre') === 'Drama'
          && url.searchParams.get('country') === 'GB'
      })).toBe(true)
    })
    expect(await screen.findByText(/Showing options for United Kingdom/)).toBeInTheDocument()
  })
})
