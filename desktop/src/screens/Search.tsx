import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type MediaItem } from '../api/client';
import type { Session } from '../session';
import { cacheItems } from '../itemCache';
import { Poster } from '../components/Poster';

export function Search({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<MediaItem[]>([]);
  const [busy, setBusy] = useState(false);

  const run = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    setBusy(true);
    try {
      const data = await api.search(serverUrl, query.trim(), session.user_id);
      cacheItems(data);
      const seen = new Set<string>();
      setResults(
        data.filter(item => {
          if (item.media_type === 'episode' && item.show_title) {
            if (seen.has(item.show_title)) return false;
            seen.add(item.show_title);
          }
          return true;
        }),
      );
    } catch {
      setResults([]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ padding: 'var(--sl-space-lg)', maxWidth: 1100, margin: '0 auto' }}>
      <h1 className="sl-section-title">Search</h1>
      <hr className="sl-divider" />
      <form onSubmit={e => void run(e)} style={{ display: 'flex', gap: 'var(--sl-space-sm)', maxWidth: 560, margin: '0 auto' }}>
        <input
          className="sl-input"
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Movies, shows..."
          autoFocus
        />
        <button className="sl-button" type="submit" disabled={busy}>
          {busy ? '...' : 'Go'}
        </button>
      </form>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-lg)', marginTop: 'var(--sl-space-lg)' }}>
        {results.map(item => (
          <Poster
            key={item.id}
            serverUrl={serverUrl}
            item={item}
            subtitle={item.show_title ? 'TV show' : item.year?.toString()}
            onClick={() => {
              cacheItems([item]);
              navigate(`/item/${item.id}`, { state: item });
            }}
          />
        ))}
      </div>
    </div>
  );
}
