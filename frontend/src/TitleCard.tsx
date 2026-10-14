import { useState } from 'react'
import type { Title } from './api'

// The trailer loads only after a tap, which keeps the first screen light on mobile data.
export default function TitleCard({ title }: { title: Title }) {
  const [playing, setPlaying] = useState(false)

  return (
    <article className="card">
      <div className="media">
        {playing && title.trailerKey ? (
          <iframe
            src={`https://www.youtube-nocookie.com/embed/${title.trailerKey}?autoplay=1&rel=0`}
            title={`${title.name} trailer`}
            allow="autoplay; encrypted-media; picture-in-picture"
            allowFullScreen
          />
        ) : (
          <button
            className="play"
            disabled={!title.trailerKey}
            onClick={() => setPlaying(true)}
            style={title.posterUrl ? { backgroundImage: `url(${title.posterUrl})` } : undefined}
          >
            {title.trailerKey ? 'Play trailer' : 'No trailer yet'}
          </button>
        )}
      </div>
      <div className="info">
        <h2>
          {title.name} {title.releaseYear && <span>({title.releaseYear})</span>}
        </h2>
        <p className="meta">
          {title.type === 'SERIES' ? 'Series' : 'Movie'}
          {title.genres.length > 0 && ` · ${title.genres.join(', ')}`}
        </p>
        {title.synopsis && <p>{title.synopsis}</p>}
      </div>
    </article>
  )
}
