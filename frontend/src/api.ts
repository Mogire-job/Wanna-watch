export interface Title {
  id: number
  type: 'MOVIE' | 'SERIES'
  name: string
  releaseYear: number | null
  synopsis: string | null
  posterUrl: string | null
  rating: number | null
  genres: string[]
  trailerKey: string | null
}

export async function fetchTitles(query: string, page = 0): Promise<Title[]> {
  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (query.trim()) params.set('q', query.trim())
  const res = await fetch(`/api/titles?${params}`)
  if (!res.ok) throw new Error(`Could not load titles (${res.status})`)
  return res.json()
}
