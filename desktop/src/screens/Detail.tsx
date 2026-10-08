import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api, assetUrl, type MediaItem } from '../api/client';
import type { Session } from '../session';
import { cacheItems, getCachedItem } from '../itemCache';

export function Detail({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [item] = useState<MediaItem | null>(
    (location.state as MediaItem | null) ?? (id ? getCachedItem(id) : null),
  );
  const [episodes, setEpisodes] = useState<MediaItem[]>([]);

  useEffect(() => {
    if (item) cacheItems([item]);
    if (item?.media_type === 'episode' && item.show_title) {
      api
        .getShowEpisodes(serverUrl, item.show_title, session.user_id)
        .then(eps => {
          cacheItems(eps);
          setEpisodes(eps);
        })
        .catch(() => {});
    }
  }, [serverUrl, session.user_id, item]);

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
  const seasons = new Map<number, MediaItem[]>();
  for (const ep of episodes) {
    const list = seasons.get(ep.season ?? 1) ?? [];
    list.push(ep);
    seasons.set(ep.season ?? 1, list);
  }

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

      {isShow &&
        [...seasons.entries()]
          .sort(([a], [b]) => a - b)
          .map(([season, eps]) => (
            <div key={season} style={{ marginTop: 'var(--sl-space-lg)' }}>
              <h2>Season {season}</h2>
              <div style={{ display: 'grid', gap: 'var(--sl-space-sm)' }}>
                {eps
                  .sort((a, b) => (a.episode ?? 0) - (b.episode ?? 0))
                  .map(ep => (
                    <button
                      key={ep.id}
                      className="sl-button"
                      style={{ textAlign: 'left', padding: 'var(--sl-space-md)' }}
                      onClick={() => navigate(`/play/${ep.id}`)}
                    >
                      E{ep.episode}: {ep.title}
                    </button>
                  ))}
              </div>
            </div>
          ))}
    </div>
  );
}
