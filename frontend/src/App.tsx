import { useEffect, useState } from 'react'
import { fetchTitles, type Title } from './api'
import TitleCard from './TitleCard'

export default function App() {
  const [query, setQuery] = useState('')
  const [titles, setTitles] = useState<Title[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  // Debounced search: reload the feed 300 ms after the user stops typing.
  useEffect(() => {
    setLoading(true)
    const handle = setTimeout(() => {
      fetchTitles(query)
        .then((t) => {
          setTitles(t)
          setError(null)
        })
        .catch((e: Error) => setError(e.message))
        .finally(() => setLoading(false))
    }, 300)
    return () => clearTimeout(handle)
  }, [query])

  return (
    <div className="app">
      <header>
        <h1>Wanna Watch</h1>
        <input
          type="search"
          placeholder="Search movies and series"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search titles"
        />
      </header>

      <main>
        {error && <p className="notice error">{error}</p>}
        {!error && !loading && titles.length === 0 && (
          <p className="notice">No titles yet. Titles appear once the data sync is set up.</p>
        )}
        {titles.map((t) => (
          <TitleCard key={t.id} title={t} />
        ))}
      </main>
    </div>
  )
}
