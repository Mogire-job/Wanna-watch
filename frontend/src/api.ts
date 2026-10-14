export type TitleType = 'MOVIE' | 'SERIES'

export interface TitleAvailability {
  provider: string
  logoUrl: string | null
  offerType: 'STREAM' | 'RENT' | 'BUY' | 'FREE'
  url: string | null
}

export interface Title {
  id: number
  type: TitleType
  name: string
  releaseYear: number | null
  synopsis: string | null
  posterUrl: string | null
  rating: number | null
  genres: string[]
  trailerKey: string | null
  availability: TitleAvailability[]
}

export interface TitlePage {
  items: Title[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface TitleFilters {
  query: string
  type: TitleType | ''
  genre: string
  provider: string
  country: string
  page: number
}

async function readJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw new Error(`Could not load catalogue (${response.status})`)
  }
  return response.json() as Promise<T>
}

export async function fetchTitles(filters: TitleFilters, signal?: AbortSignal): Promise<TitlePage> {
  const params = new URLSearchParams({ page: String(filters.page), size: '20' })
  if (filters.query.trim()) params.set('q', filters.query.trim())
  if (filters.type) params.set('type', filters.type)
  if (filters.genre) params.set('genre', filters.genre)
  if (filters.provider) params.set('provider', filters.provider)
  if (filters.country) params.set('country', filters.country)
  return readJson<TitlePage>(`/api/titles?${params}`, signal)
}

export function fetchGenres(signal?: AbortSignal): Promise<string[]> {
  return readJson<string[]>('/api/genres', signal)
}

export interface CountryOption {
  code: string
  name: string
}

export function fetchCountries(signal?: AbortSignal): Promise<CountryOption[]> {
  return readJson<CountryOption[]>('/api/countries', signal)
}

export interface ProviderOption {
  name: string
  logoUrl: string | null
}

export function fetchProviders(country: string, signal?: AbortSignal): Promise<ProviderOption[]> {
  return readJson<ProviderOption[]>(`/api/providers?country=${encodeURIComponent(country)}`, signal)
}

export interface Account {
  email: string
  displayName: string | null
  emailVerified: boolean
}

async function postAuth<T>(path: string, body?: object): Promise<T> {
  const csrfResponse = await fetch('/api/auth/csrf')
  if (!csrfResponse.ok) throw new Error(`Could not start secure sign-in (${csrfResponse.status})`)
  const { token } = await csrfResponse.json() as { token: string }
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(error?.message ?? `Account request failed (${response.status})`)
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}

export function registerAccount(input: { email: string; displayName: string; password: string }): Promise<{ message: string }> {
  return postAuth('/api/auth/register', input)
}

export function verifyAccount(token: string): Promise<{ message: string }> {
  return postAuth('/api/auth/verify', { token })
}

export function signIn(input: { email: string; password: string }): Promise<Account> {
  return postAuth('/api/auth/login', input)
}

export function signOut(): Promise<void> {
  return postAuth('/api/auth/logout')
}
