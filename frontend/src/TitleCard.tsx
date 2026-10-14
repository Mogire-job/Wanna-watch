import { useState, type CSSProperties } from 'react'
import type { Title } from './api'

function offerLabel(offerType: string): string {
  switch (offerType) {
    case 'STREAM': return 'Stream'
    case 'RENT': return 'Rent'
    case 'BUY': return 'Buy'
    default: return 'Free'
  }
}

export default function TitleCard({ title, index = 0, country }: { title: Title; index?: number; country: string }) {
  const [playing, setPlaying] = useState(false)
  const posterStyle = title.posterUrl
    ? { backgroundImage: `url("${title.posterUrl}")` }
    : undefined

  return (
    <article className="card" style={{ '--card-index': index } as CSSProperties}>
      <div className={`media ${!title.posterUrl ? 'media-fallback' : ''}`}>
        {playing && title.trailerKey ? (
          <iframe
            src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(title.trailerKey)}?autoplay=1&rel=0`}
            title={`${title.name} official trailer`}
            allow="autoplay; encrypted-media; picture-in-picture"
            referrerPolicy="strict-origin-when-cross-origin"
            allowFullScreen
          />
        ) : (
          <button
            className="play"
            type="button"
            disabled={!title.trailerKey}
            onClick={() => setPlaying(true)}
            style={posterStyle}
            aria-label={title.trailerKey ? `Play ${title.name} trailer` : `${title.name} has no trailer yet`}
          >
            <span className="play-label">{title.trailerKey ? 'Watch trailer' : 'Trailer coming soon'}</span>
          </button>
        )}
        <span className="type-badge">{title.type === 'SERIES' ? 'Series' : 'Movie'}</span>
        {title.rating !== null && <span className="rating-badge"><span aria-hidden="true">★</span> {title.rating.toFixed(1)}</span>}
      </div>
      <div className="info">
        <h3>
          {title.name} {title.releaseYear && <span>({title.releaseYear})</span>}
        </h3>
        {title.genres.length > 0 && <p className="meta">{title.genres.join(' · ')}</p>}
        {title.synopsis && <p className="synopsis">{title.synopsis}</p>}
        {title.availability.length > 0 ? (
          <div className="availability">
            <span className="availability-label">WATCH OPTIONS IN {country.toUpperCase()}</span>
            <div className="provider-list">
              {title.availability.map((offer, offerIndex) => {
                const content = (
                  <>
                    {offer.logoUrl
                      ? <img src={offer.logoUrl} alt="" loading="lazy" onError={(event) => { event.currentTarget.hidden = true }} />
                      : <span className="provider-initial">{offer.provider.charAt(0)}</span>}
                    <span>{offer.provider}<small>{offerLabel(offer.offerType)}</small></span>
                  </>
                )
                return offer.url ? (
                  <a
                    className="provider-link"
                    href={offer.url}
                    key={`${offer.provider}-${offer.offerType}-${offerIndex}`}
                    target="_blank"
                    rel="noreferrer"
                    aria-label={`${offerLabel(offer.offerType)} ${title.name} on ${offer.provider}`}
                  >
                    {content}
                  </a>
                ) : (
                  <span className="provider-link provider-static" key={`${offer.provider}-${offer.offerType}-${offerIndex}`}>
                    {content}
                  </span>
                )
              })}
            </div>
          </div>
        ) : (
          <p className="availability-missing">No watch options listed for this country yet</p>
        )}
      </div>
    </article>
  )
}
