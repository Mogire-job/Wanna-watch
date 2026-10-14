import { useEffect, useState } from 'react'
import { fetchCountries, fetchGenres, fetchProviders, fetchTitles, type CountryOption, type ProviderOption, type Title, type TitlePage, type TitleType } from './api'
import TitleCard from './TitleCard'
import Spotlight from './Spotlight'
import AccountPanel from './AccountPanel'

const EMPTY_PAGE: TitlePage = { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 }

function preferredCountry(): string {
  const region = new Intl.Locale(navigator.language).region
  return region && /^[A-Z]{2}$/.test(region) ? region : 'US'
}

export default function App() {
  const [query, setQuery] = useState('')
  const [type, setType] = useState<TitleType | ''>('')
  const [genre, setGenre] = useState('')
  const [provider, setProvider] = useState('')
  const [country, setCountry] = useState(preferredCountry)
  const [page, setPage] = useState(0)
  const [catalogue, setCatalogue] = useState(EMPTY_PAGE)
  const [genres, setGenres] = useState<string[]>([])
  const [providers, setProviders] = useState<ProviderOption[]>([])
  const [countries, setCountries] = useState<CountryOption[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([fetchGenres(controller.signal), fetchCountries(controller.signal)])
      .then(([genreOptions, countryOptions]) => {
        setGenres(genreOptions)
        setCountries(countryOptions)
        if (!countryOptions.some((option) => option.code === country)) setCountry('US')
      })
      .catch((reason: Error) => {
        if (reason.name !== 'AbortError') setError(reason.message)
      })
    return () => controller.abort()
  }, [country])

  useEffect(() => {
    const controller = new AbortController()
    fetchProviders(country, controller.signal)
      .then(setProviders)
      .catch((reason: Error) => {
        if (reason.name !== 'AbortError') setError(reason.message)
      })
    setProvider('')
    return () => controller.abort()
  }, [country])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    const timer = window.setTimeout(() => {
      fetchTitles({ query, type, genre, provider, country, page }, controller.signal)
        .then((result) => {
          setCatalogue(result)
          setError(null)
        })
        .catch((reason: Error) => {
          if (reason.name !== 'AbortError') setError(reason.message)
        })
        .finally(() => {
          if (!controller.signal.aborted) setLoading(false)
        })
    }, query ? 250 : 0)
    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [query, type, genre, provider, country, page])

  function clearFilters() {
    setQuery('')
    setType('')
    setGenre('')
    setProvider('')
    setPage(0)
  }

  function changeType(value: TitleType | '') {
    setType(value)
    setPage(0)
  }

  function changeGenre(value: string) {
    setGenre(value)
    setPage(0)
  }

  function changeProvider(value: string) {
    setProvider(value)
    setPage(0)
  }

  const hasFilters = Boolean(query || type || genre || provider)
  const featuredTitle = !hasFilters && page === 0 ? catalogue.items[0] : undefined
  const visibleTitles = featuredTitle ? catalogue.items.slice(1) : catalogue.items
  const selectedCountry = countries.find((option) => option.code === country)

  return (
    <div className="app">
      <header>
        <div className="brand">
          <a className="brand-mark" href="/" aria-label="Wanna Watch home">
            <span className="brand-symbol" aria-hidden="true">W</span>
            <span>WANNA<span className="brand-mark-light">WATCH</span></span>
          </a>
          <p className="eyebrow">A WORLD OF STORIES, ONE PLACE</p>
          <h1>Find your <span>next world.</span></h1>
          <p className="tagline">Movies and series worth staying in for.</p>
        </div>
        <div className="header-actions">
          <AccountPanel />
          <label className="country-select">
            <span aria-hidden="true" className="globe-icon">◎</span>
            <span className="sr-only">Choose your country or region</span>
            <select
              aria-label="Choose your country or region"
              value={country}
              onChange={(event) => { setCountry(event.target.value); setPage(0) }}
            >
              {countries.length === 0
                ? <option value={country}>{country}</option>
                : countries.map((option) => <option key={option.code} value={option.code}>{option.name}</option>)}
            </select>
            <span className="region-code">{country}</span>
          </label>
          <label className="search-wrap">
            <span aria-hidden="true" className="search-icon">⌕</span>
            <input
              type="search"
              placeholder="Search movies and series"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value)
                setPage(0)
              }}
              aria-label="Search titles"
            />
            {query && <button type="button" className="clear-search" onClick={() => setQuery('')} aria-label="Clear search">×</button>}
          </label>
        </div>
      </header>

      <section className="discover-bar" aria-label="Catalogue filters">
        <div className="filter-group" aria-label="Title type">
          <button className={`filter-chip ${!type ? 'selected' : ''}`} onClick={() => changeType('')}>Everything</button>
          <button className={`filter-chip ${type === 'MOVIE' ? 'selected' : ''}`} onClick={() => changeType('MOVIE')}>Movies</button>
          <button className={`filter-chip ${type === 'SERIES' ? 'selected' : ''}`} onClick={() => changeType('SERIES')}>Series</button>
        </div>
        <div className="select-group">
          <label>
            <span className="sr-only">Filter by genre</span>
            <select aria-label="Filter by genre" value={genre} onChange={(event) => changeGenre(event.target.value)}>
              <option value="">All genres</option>
              {genres.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
          <label>
            <span className="sr-only">Filter by streaming service</span>
            <select value={provider} onChange={(event) => changeProvider(event.target.value)}>
              <option value="">All services</option>
              {providers.map((item) => <option key={item.name} value={item.name}>{item.name}</option>)}
            </select>
            </label>
        </div>
        <span className="market-indicator"><span className="live-dot" /> Showing options for {selectedCountry?.name ?? country}</span>
      </section>

      <main>
        <div className="results-heading" id="explore">
            <div>
              <p className="eyebrow">{hasFilters ? 'TUNED TO YOUR TASTE' : 'WHAT THE WORLD IS WATCHING'}</p>
              <h2>{hasFilters ? 'Your picks' : 'Trending everywhere'}</h2>
            </div>
            <span className="result-count">{catalogue.totalItems.toLocaleString()} titles</span>
        </div>

        {error && (
          <div className="notice error" role="alert">
            <strong>We couldn't reach the catalogue.</strong>
            <span>{error}. Make sure the API and PostgreSQL database are running.</span>
          </div>
        )}
        {!error && loading && (
          <div className="title-grid" aria-label="Loading catalogue" aria-busy="true">
            {Array.from({ length: 6 }, (_, index) => <div className="card skeleton" key={index} />)}
          </div>
        )}
        {!error && !loading && catalogue.items.length === 0 && (
          <div className="empty-state">
            <span className="empty-orbit" aria-hidden="true">✦</span>
            <h3>{hasFilters ? 'No matches this time' : 'Your next favorite is out there'}</h3>
            <p>{hasFilters
              ? 'Try another search or clear a filter to discover more.'
              : `The catalogue is ready for its first sync. Connect a TMDB access token and sync movies, series, trailers, and watch options for ${selectedCountry?.name ?? country}.`}</p>
            {hasFilters && <button className="text-button" onClick={clearFilters}>Clear all filters</button>}
          </div>
        )}
        {!error && !loading && featuredTitle && <Spotlight title={featuredTitle} country={selectedCountry?.name ?? country} />}
        {!error && !loading && catalogue.items.length > 0 && (
          <div className="title-grid">
            {visibleTitles.map((title: Title, index) => (
              <TitleCard key={title.id} title={title} index={index} country={selectedCountry?.name ?? country} />
            ))}
          </div>
        )}

        {!error && !loading && catalogue.totalPages > 1 && (
          <nav className="pagination" aria-label="Catalogue pages">
            <button disabled={page === 0} onClick={() => setPage((current) => current - 1)}>← Previous</button>
            <span>Page {page + 1} of {catalogue.totalPages}</span>
            <button disabled={page + 1 >= catalogue.totalPages} onClick={() => setPage((current) => current + 1)}>Next →</button>
          </nav>
        )}
      </main>

      <footer>
        <span>For wherever you call home <span className="footer-heart">♥</span></span>
        <span>This product uses the TMDB API but is not endorsed or certified by TMDB.</span>
        <a href="https://www.themoviedb.org/" target="_blank" rel="noreferrer">About TMDB</a>
      </footer>
    </div>
  )
}
