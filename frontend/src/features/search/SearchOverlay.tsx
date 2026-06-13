import React, { useState, useEffect, useRef } from 'react';
import { api } from '../../api/client';
import { Poster } from '../../components/common/Poster';
import type { MediaItem, Library } from '../../types';

interface SearchOverlayProps {
  onClose: () => void;
  onSelect: (item: MediaItem) => void;
}

export const SearchOverlay: React.FC<SearchOverlayProps> = ({ onClose, onSelect }) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<MediaItem[]>([]);
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [libraryItems, setLibraryItems] = useState<Record<string, MediaItem[]>>({});
  const [typeFilter, setTypeFilter] = useState<'all' | 'MOVIE' | 'EPISODE'>('all');
  const [genreFilter, setGenreFilter] = useState('');
  const [genres, setGenres] = useState<string[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
    api.getLibraries().then(setLibraries);
    api.getGenres().then(setGenres).catch(() => {});
  }, []);

  useEffect(() => {
    if (libraries.length === 0) return;
    libraries.forEach(async (lib) => {
      try {
        const items = await api.getLibraryItems(lib.id);
        setLibraryItems(prev => ({ ...prev, [lib.id]: items.slice(0, 8) }));
      } catch {}
    });
  }, [libraries]);

  useEffect(() => {
    if (query.length < 2) { setResults([]); return; }
    const timer = setTimeout(async () => {
      try {
        const data = await api.search(query);
        setResults(data);
      } catch {}
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  const filterItem = (item: MediaItem): boolean => {
    if (typeFilter === 'MOVIE' && item.media_type !== 'movie') return false;
    if (typeFilter === 'EPISODE' && item.media_type !== 'episode') return false;
    if (genreFilter && item.genres) {
      const itemGenres = item.genres.split(',').map(g => g.trim());
      if (!itemGenres.includes(genreFilter)) return false;
    }
    return true;
  };

  const pillStyle = (active: boolean): React.CSSProperties => ({
    padding: '0.35rem 0.85rem',
    borderRadius: '999px',
    border: '1px solid var(--border-color)',
    background: active ? 'var(--primary-color)' : 'rgba(255,255,255,0.08)',
    color: active ? 'white' : 'var(--text-secondary)',
    fontSize: '0.85rem',
    fontWeight: 600,
    cursor: 'pointer',
    whiteSpace: 'nowrap' as const,
    flexShrink: 0,
  });

  const filterRowStyle: React.CSSProperties = {
    display: 'flex',
    gap: '0.5rem',
    overflowX: 'auto',
    whiteSpace: 'nowrap',
    paddingBottom: '0.25rem',
  };

  interface EpisodeGroup {
    showTitle: string;
    episodes: MediaItem[];
  }

  const groupEpisodes = (items: MediaItem[]): (MediaItem | EpisodeGroup)[] => {
    const groups: Record<string, MediaItem[]> = {};
    const nonEpisodes: MediaItem[] = [];
    for (const item of items) {
      if (item.media_type === 'episode' && item.show_title) {
        if (!groups[item.show_title]) groups[item.show_title] = [];
        groups[item.show_title].push(item);
      } else {
        nonEpisodes.push(item);
      }
    }
    const grouped: (MediaItem | EpisodeGroup)[] = [...nonEpisodes];
    for (const [showTitle, episodes] of Object.entries(groups)) {
      grouped.push({ showTitle, episodes });
    }
    return grouped;
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.95)', zIndex: 3000,
      display: 'flex', flexDirection: 'column', padding: '2rem'
    }} onClick={onClose}>
      <div style={{ maxWidth: '1000px', width: '100%', margin: '0 auto' }} onClick={e => e.stopPropagation()}>
        <input
          ref={inputRef}
          type="text"
          placeholder="Search movies & shows..."
          value={query}
          onChange={e => setQuery(e.target.value)}
          style={{
            width: '100%', padding: '1.2rem 1.5rem', fontSize: '1.5rem',
            background: 'rgba(255,255,255,0.1)', border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)', color: 'white', outline: 'none',
            fontWeight: 600
          }}
        />
        <button
          onClick={onClose}
          style={{
            position: 'absolute', top: '1rem', right: '1rem',
            background: 'rgba(255,255,255,0.1)', border: 'none', color: 'white',
            width: '40px', height: '40px', borderRadius: '50%', cursor: 'pointer',
            fontSize: '1.2rem', display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}
        >
          ✕
        </button>
      </div>

      {/* Type Filter Chips */}
      <div style={{ maxWidth: '1000px', width: '100%', margin: '1rem auto 0' }}>
        <div style={filterRowStyle}>
          {(['all', 'MOVIE', 'EPISODE'] as const).map(t => (
            <button key={t} style={pillStyle(typeFilter === t)} onClick={() => setTypeFilter(t)}>
              {t === 'all' ? 'All' : t === 'MOVIE' ? 'Movies' : 'Shows'}
            </button>
          ))}
        </div>
      </div>

      {/* Genre Filter Chips */}
      {genres.length > 0 && (
        <div style={{ maxWidth: '1000px', width: '100%', margin: '0.75rem auto 0' }}>
          <div style={filterRowStyle}>
            {genres.map(g => (
              <button key={g} style={pillStyle(genreFilter === g)} onClick={() => setGenreFilter(genreFilter === g ? '' : g)}>
                {g}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Library Grid (shown when idle) */}
      {query.length < 2 && libraries.length > 0 && (
        <div style={{ maxWidth: '1000px', width: '100%', margin: '2rem auto 0', overflowY: 'auto', flex: 1, paddingBottom: '2rem' }}>
          {libraries.map(lib => {
            const filtered = (libraryItems[lib.id] || []).filter(filterItem);
            const grouped = groupEpisodes(filtered);
            if (grouped.length === 0) return null;
            return (
              <div key={lib.id} style={{ marginBottom: '2.5rem' }}>
                <h3 style={{ fontSize: '1.3rem', marginBottom: '1rem', color: 'var(--text-secondary)' }}>{lib.name}</h3>
                <div style={{
                  display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
                  gap: '1rem'
                }}>
                  {grouped.map((entry, i) => {
                    if ('showTitle' in entry) {
                      const group = entry as EpisodeGroup;
                      return (
                        <div key={group.showTitle} style={{
                          gridColumn: '1 / -1',
                          padding: '0.75rem 1rem',
                          background: 'rgba(255,255,255,0.06)',
                          borderRadius: 'var(--radius-md)',
                        }}>
                          <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'white' }}>{group.showTitle}</div>
                          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                            {group.episodes.length} episode{group.episodes.length !== 1 ? 's' : ''}
                          </div>
                          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem', overflowX: 'auto' }}>
                            {group.episodes.map(ep => (
                              <Poster
                                key={ep.id}
                                itemId={ep.id}
                                title={ep.title}
                                subtitle={ep.year?.toString()}
                                onClick={() => { onSelect(ep); onClose(); }}
                              />
                            ))}
                          </div>
                        </div>
                      );
                    }
                    const item = entry as MediaItem;
                    return (
                      <Poster
                        key={item.id}
                        itemId={item.id}
                        title={item.title}
                        subtitle={item.year?.toString()}
                        onClick={() => { onSelect(item); onClose(); }}
                      />
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Search Results */}
      {results.length > 0 && (() => {
        const filtered = results.filter(filterItem);
        const grouped = groupEpisodes(filtered);
        if (grouped.length === 0) return null;
        return (
          <div style={{
            maxWidth: '1000px', width: '100%', margin: '2rem auto 0',
            overflowY: 'auto', flex: 1, paddingBottom: '2rem'
          }}>
            <div style={{
              display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
              gap: '1.5rem'
            }}>
              {grouped.map((entry, i) => {
                if ('showTitle' in entry) {
                  const group = entry as EpisodeGroup;
                  return (
                    <div key={group.showTitle} style={{
                      gridColumn: '1 / -1',
                      padding: '0.75rem 1rem',
                      background: 'rgba(255,255,255,0.06)',
                      borderRadius: 'var(--radius-md)',
                    }}>
                      <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'white' }}>{group.showTitle}</div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                        {group.episodes.length} episode{group.episodes.length !== 1 ? 's' : ''}
                      </div>
                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem', overflowX: 'auto' }}>
                        {group.episodes.map(ep => (
                          <Poster
                            key={ep.id}
                            itemId={ep.id}
                            title={ep.title}
                            subtitle={ep.year?.toString()}
                            onClick={() => { onSelect(ep); onClose(); }}
                          />
                        ))}
                      </div>
                    </div>
                  );
                }
                const item = entry as MediaItem;
                return (
                  <Poster
                    key={item.id}
                    itemId={item.id}
                    title={item.title}
                    subtitle={item.year?.toString()}
                    onClick={() => { onSelect(item); onClose(); }}
                  />
                );
              })}
            </div>
          </div>
        );
      })()}

      {query.length >= 2 && results.filter(filterItem).length === 0 && (
        <div style={{ color: 'var(--text-secondary)', textAlign: 'center', marginTop: '3rem', fontSize: '1.2rem' }}>
          No results found for "{query}"
        </div>
      )}
    </div>
  );
};
