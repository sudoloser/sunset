import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type Library, type MediaItem } from '../api/client';
import type { Session } from '../session';
import { cacheItems, sortEpisodes } from '../itemCache';
import { Poster } from '../components/Poster';

export function Libraries({ serverUrl }: { serverUrl: string }) {
  const navigate = useNavigate();
  const [libraries, setLibraries] = useState<Library[]>([]);

  useEffect(() => {
    api.getLibraries(serverUrl).then(setLibraries).catch(() => {});
  }, [serverUrl]);

  return (
    <div style={{ padding: 'var(--sl-space-lg)', maxWidth: 1100, margin: '0 auto' }}>
      <h1 className="sl-section-title">Libraries</h1>
      <hr className="sl-divider" />
      <div style={{ display: 'grid', gap: 'var(--sl-space-md)', maxWidth: 640, margin: '0 auto' }}>
        {libraries.map(lib => (
          <button
            key={lib.id}
            className="sl-button"
            style={{ padding: 'var(--sl-space-md)', fontSize: 'var(--sl-text-lg)', textAlign: 'left' }}
            onClick={() => navigate(`/libraries/${lib.id}`)}
          >
            {lib.name} <span className="sl-tag">{lib.lib_type === 'movies' ? 'Movies' : 'TV Shows'}</span>
          </button>
        ))}
        {libraries.length === 0 && <p style={{ textAlign: 'center' }}>No libraries yet.</p>}
      </div>
    </div>
  );
}

export function LibraryDetail({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [library, setLibrary] = useState<Library | null>(null);
  const [items, setItems] = useState<MediaItem[]>([]);

  useEffect(() => {
    if (!id) return;
    api
      .getLibraries(serverUrl)
      .then(libs => setLibrary(libs.find(l => l.id === id) ?? null))
      .catch(() => {});
    api
      .getLibraryItems(serverUrl, id, session.user_id)
      .then(data => {
        cacheItems(data);
        setItems(data);
      })
      .catch(() => {});
  }, [serverUrl, session.user_id, id]);

  const open = (item: MediaItem) => {
    cacheItems([item]);
    navigate(`/item/${item.id}`, { state: item });
  };

  const grouped = new Map<string, MediaItem[]>();
  if (library?.lib_type === 'shows') {
    for (const item of items) {
      const key = item.show_title ?? item.title;
      const list = grouped.get(key) ?? [];
      list.push(item);
      grouped.set(key, list);
    }
    // Order each show so the representative is always S1E1, never a
    // later season that happened to sort first from the database.
    for (const [key, eps] of grouped) grouped.set(key, sortEpisodes(eps));
  }

  return (
    <div style={{ padding: 'var(--sl-space-lg)', maxWidth: 1100, margin: '0 auto' }}>
      <button className="sl-button" onClick={() => navigate(-1)}>
        Back
      </button>
      <h1 className="sl-section-title" style={{ marginTop: 'var(--sl-space-md)' }}>
        {library?.name ?? 'Library'}
      </h1>
      <hr className="sl-divider" />
      {library?.lib_type === 'movies' ? (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-lg)' }}>
          {items.map(item => (
            <Poster
              key={item.id}
              serverUrl={serverUrl}
              item={item}
              subtitle={item.year?.toString()}
              onClick={() => open(item)}
            />
          ))}
        </div>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-lg)' }}>
          {[...grouped.entries()].map(([show, eps]) => (
            <Poster
              key={show}
              serverUrl={serverUrl}
              item={eps[0]}
              title={show}
              subtitle={`${eps.length} episode${eps.length === 1 ? '' : 's'}`}
              onClick={() => open(eps[0])}
            />
          ))}
        </div>
      )}
    </div>
  );
}
