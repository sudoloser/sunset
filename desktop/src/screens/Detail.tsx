import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api, assetUrl, type MediaItem } from '../api/client';
import type { Session } from '../session';
import { cacheItems, getCachedItem, sortEpisodes } from '../itemCache';

export function Detail({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const locationItem = (location as unknown as { state?: MediaItem | null }).state ?? null;
  const item: MediaItem | null =
    (locationItem && id && locationItem.id === id ? locationItem : null) ??
    (id ? getCachedItem(id) : null);
  const [episodes, setEpisodes] = useState<MediaItem[]>([]);
  const [season, setSeason] = useState<number | null>(null);

  useEffect(() => {
    setEpisodes([]);
    setSeason(null);
    if (item) cacheItems([item]);
    if (item?.media_type === 'episode' && item.show_title) {
      api
        .getShowEpisodes(serverUrl, item.show_title, session.user_id)
        .then(eps => {
          const ordered = sortEpisodes(eps);
          cacheItems(ordered);
          setEpisodes(ordered);
          const seasons = [...new Set(ordered.map(e => e.season ?? 1))].sort((a, b) => a - b);
          // Show pages always open at season 1; resume/continue flows go
          // straight to the player, so the dropdown never needs S4 first.
          setSeason(seasons[0] ?? 1);
        })
        .catch(() => {});
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [serverUrl, session.user_id, id]);

  if (!item) {
    return (
      <div className="sl-center">
        <p>Couldn't find that item. It may have fallen out of the cache.</p>
        <button className="sl-button" onClick={() => navigate('/')}>
          Back to browsing
        </button>
      </div>
    );
  }

  const isShow = item.media_type === 'episode' && item.show_title;
  const seasonNumbers = [...new Set(episodes.map(e => e.season ?? 1))].sort((a, b) => a - b);
  const shownSeason = season ?? seasonNumbers[0] ?? 1;
  const shownEpisodes = episodes
    .filter(e => (e.season ?? 1) === shownSeason)
    .sort((a, b) => (a.episode ?? 0) - (b.episode ?? 0));

  return (
    <div style={{ padding: 'var(--sl-space-lg)', maxWidth: 900, margin: '0 auto' }}>
      <button className="sl-button" onClick={() => navigate(-1)}>
        Back
      </button>
      <div
        style={{
          borderRadius: 'var(--sl-radius-md)',
          border: '2px solid var(--sl-text)',
          minHeight: 280,
          marginTop: 'var(--sl-space-md)',
          background: `var(--sl-bg-soft) url("${assetUrl(serverUrl, item.id, 'backdrop.jpg')}") center / cover`,
        }}
      />
      <div className="sl-card" style={{ marginTop: 'var(--sl-space-md)' }}>
        <h1 style={{ margin: 0, fontSize: 'var(--sl-text-2xl)' }}>
          {item.show_title ?? item.title}
        </h1>
        <p>
          {item.year && <span className="sl-tag">{item.year}</span>}{' '}
          {item.rating != null && <span className="sl-tag">★ {item.rating.toFixed(1)}</span>}{' '}
          {item.version_tag && <span className="sl-tag">{item.version_tag}</span>}
        </p>
        {item.genres && (
          <p style={{ color: 'var(--sl-text-link)' }}>{item.genres}</p>
        )}
        {item.description && <p>{item.description}</p>}
        {item.cast && (
          <p style={{ fontSize: 'var(--sl-text-sm)' }}>
            <strong>Cast:</strong> {item.cast}
          </p>
        )}
        {!isShow && (
          <button className="sl-button" onClick={() => navigate(`/play/${item.id}`)}>
            Play
          </button>
        )}
      </div>

      {isShow && seasonNumbers.length > 0 && (
        <div style={{ marginTop: 'var(--sl-space-lg)' }}>
          <label style={{ fontWeight: 700, marginRight: 'var(--sl-space-sm)' }}>Season</label>
          <select
            className="sl-input"
            style={{ width: 'auto' }}
            value={shownSeason}
            onChange={e => setSeason(Number(e.target.value))}
          >
            {seasonNumbers.map(s => (
              <option key={s} value={s}>
                Season {s}
              </option>
            ))}
          </select>
          <div style={{ display: 'grid', gap: 'var(--sl-space-sm)', marginTop: 'var(--sl-space-md)' }}>
            {shownEpisodes.map(ep => (
              <button
                key={ep.id}
                className="sl-button"
                style={{ textAlign: 'left', padding: 'var(--sl-space-md)' }}
                onClick={() => {
                  cacheItems([ep]);
                  navigate(`/play/${ep.id}`, { state: ep });
                }}
              >
                E{ep.episode}: {ep.title}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
