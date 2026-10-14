import { useState } from 'react'
import type { CSSProperties } from 'react'
import type { Title } from './api'

export default function Spotlight({ title, country }: { title: Title; country: string }) {
  const [playing, setPlaying] = useState(false)
  const artwork: CSSProperties = title.posterUrl
    ? { backgroundImage: `url("${title.posterUrl}")` }
    : {}

  return (
    <section className="spotlight" aria-label={`Featured title: ${title.name}`}>
      <div className="spotlight-art" style={artwork} />
      {playing && title.trailerKey && (
        <iframe
          className="spotlight-video"
          src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(title.trailerKey)}?autoplay=1&rel=0`}
          title={`${title.name} official trailer`}
          allow="autoplay; encrypted-media; picture-in-picture"
          referrerPolicy="strict-origin-when-cross-origin"
          allowFullScreen
        />
      )}
      <div className="spotlight-copy">
        <p className="spotlight-kicker"><span className="sparkle-mark">✦</span> GLOBAL SPOTLIGHT <span className="spotlight-divider" /> WATCH IN {country.toUpperCase()}</p>
        <h3>{title.name}{title.releaseYear && <span> {title.releaseYear}</span>}</h3>
        <p className="spotlight-meta">
          {title.type === 'SERIES' ? 'Series' : 'Movie'}
          {title.rating !== null && <> <span>·</span> <span className="spotlight-star">★</span> {title.rating.toFixed(1)}</>}
          {title.genres.length > 0 && <> <span>·</span> {title.genres.slice(0, 3).join(' · ')}</>}
        </p>
        {title.synopsis && <p className="spotlight-synopsis">{title.synopsis}</p>}
        <div className="spotlight-actions">
          {title.trailerKey && !playing && (
            <button className="watch-button" type="button" onClick={() => setPlaying(true)}>
              <span aria-hidden="true">▶</span> Watch trailer
            </button>
          )}
          <a className="explore-button" href="#explore">Explore titles <span aria-hidden="true">↓</span></a>
        </div>
      </div>
      <span className="spotlight-index" aria-hidden="true">01</span>
    </section>
  )
}
