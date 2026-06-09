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
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
    api.getLibraries().then(setLibraries);
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

      {/* Library Grid (shown when idle) */}
      {query.length < 2 && libraries.length > 0 && (
        <div style={{ maxWidth: '1000px', width: '100%', margin: '2rem auto 0', overflowY: 'auto', flex: 1, paddingBottom: '2rem' }}>
          {libraries.map(lib => (
            <div key={lib.id} style={{ marginBottom: '2.5rem' }}>
              <h3 style={{ fontSize: '1.3rem', marginBottom: '1rem', color: 'var(--text-secondary)' }}>{lib.name}</h3>
              <div style={{
                display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
                gap: '1rem'
              }}>
                {(libraryItems[lib.id] || []).filter(item => item.media_type !== 'episode').map(item => (
                  <Poster
                    key={item.id}
                    itemId={item.id}
                    title={item.title}
                    subtitle={item.year?.toString()}
                    onClick={() => { onSelect(item); onClose(); }}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Search Results */}
      {results.length > 0 && (
        <div style={{
          maxWidth: '1000px', width: '100%', margin: '2rem auto 0',
          display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
          gap: '1.5rem', overflowY: 'auto', flex: 1, paddingBottom: '2rem'
        }}>
          {results.filter(item => item.media_type !== 'episode').map(item => (
            <Poster
              key={item.id}
              itemId={item.id}
              title={item.title}
              subtitle={item.year?.toString()}
              onClick={() => { onSelect(item); onClose(); }}
            />
          ))}
        </div>
      )}

      {query.length >= 2 && results.length === 0 && (
        <div style={{ color: 'var(--text-secondary)', textAlign: 'center', marginTop: '3rem', fontSize: '1.2rem' }}>
          No results found for "{query}"
        </div>
      )}
    </div>
  );
};
