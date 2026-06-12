import React, { useEffect, useState, useCallback, useRef } from 'react';
import { api } from '../../api/client';
import { Hero } from '../../components/common/Hero';
import { MediaRow } from '../../components/common/MediaRow';
import type { MediaItem, Library } from '../../types';

interface DashboardProps {
  onSelectItem: (item: MediaItem) => void;
  onPlayItem: (item: MediaItem) => void;
  onSearch: () => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onSelectItem, onPlayItem, onSearch }) => {
  const [recent, setRecent] = useState<MediaItem[]>([]);
  const [continueWatching, setContinueWatching] = useState<MediaItem[]>([]);
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [featured, setFeatured] = useState<MediaItem | undefined>();
  const [genres, setGenres] = useState<string[]>([]);
  const [genreItems, setGenreItems] = useState<Record<string, MediaItem[]>>({});
  const [collections, setCollections] = useState<any[]>([]);
  const [pulling, setPulling] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const pullStartY = useRef(0);

  const loadData = useCallback(async () => {
    const userId = localStorage.getItem('sunset_user_id') || undefined;
    const [recentData, libsData, continueData] = await Promise.all([
      api.getRecentlyAdded(userId),
      api.getLibraries(),
      userId ? api.getContinueWatching(userId) : Promise.resolve([])
    ]);
    
    // Deduplicate by show_title for episodes
    const dedupe = (items: MediaItem[]) => {
      const seen = new Set();
      return items.filter(r => {
        if (r.media_type === 'episode' && r.show_title) {
          if (seen.has(r.show_title)) return false;
          seen.add(r.show_title);
          return true;
        }
        return true;
      }).map(r => ({
        ...r,
        title: (r.media_type === 'episode' && r.show_title) ? r.show_title : r.title
      }));
    };

    setRecent(dedupe(recentData));
    setContinueWatching(dedupe(continueData));
    setLibraries(libsData);
    if (recentData.length > 0) setFeatured(recentData[0]);

    // Handle Collections
    const allItems: MediaItem[] = [];
    for (const lib of libsData) {
      const items = await api.getLibraryItems(lib.id, userId);
      allItems.push(...items);
    }

    const collectionGroups = allItems.reduce((acc, item) => {
      if (item.collection_name) {
        if (!acc[item.collection_name]) acc[item.collection_name] = [];
        acc[item.collection_name].push(item);
      }
      return acc;
    }, {} as Record<string, MediaItem[]>);

    const collectionsData = Object.entries(collectionGroups)
      .filter(([_, items]) => items.length > 1)
      .map(([name, items]) => ({
        ...items[0],
        id: `collection_${items[0].id}`,
        title: name,
        media_type: 'movie' as const,
        is_collection: true,
        items: items.sort((a, b) => (a.year || 0) - (b.year || 0))
      }));

    setCollections(collectionsData);

    try {
      const genreList = await api.getGenres();
      setGenres(genreList.slice(0, 5));
      const itemsMap: Record<string, MediaItem[]> = {};
      await Promise.all(genreList.slice(0, 5).map(async (g) => {
        try {
          const genreData = await api.getGenreItems(g, userId);
          const gSeen = new Set();
          itemsMap[g] = genreData.filter((r: MediaItem) => {
            if (r.media_type === 'episode' && r.show_title) {
              if (gSeen.has(r.show_title)) return false;
              gSeen.add(r.show_title);
              r.title = r.show_title;
              return true;
            }
            return true;
          });
        } catch {}
      }));
      setGenreItems(itemsMap);
    } catch {}
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  return (
    <div
      ref={containerRef}
      style={{ paddingBottom: 'var(--spacing-xxl)', marginTop: 'calc(-1 * var(--header-height) - var(--safe-area-top))' }}
      onTouchStart={(e) => { pullStartY.current = e.touches[0].clientY; }}
      onTouchMove={(e) => {
        if (containerRef.current && containerRef.current.scrollTop <= 0) {
          const diff = e.touches[0].clientY - pullStartY.current;
          if (diff > 80) { setPulling(true); }
        }
      }}
      onTouchEnd={() => {
        if (pulling) { setPulling(false); loadData(); }
      }}
    >
      {pulling && <div className="pull-indicator">Release to refresh</div>}
      <Hero item={featured} onPlay={onPlayItem} onMoreInfo={onSelectItem} />

      {/* Search Button */}
      <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '0 var(--spacing-xl)', marginBottom: 'var(--spacing-md)', marginTop: '-60px', position: 'relative', zIndex: 20 }}>
        <button
          onClick={onSearch}
          style={{
            background: 'rgba(255,255,255,0.1)', border: 'none', color: 'white',
            padding: '0.6rem 1.5rem', borderRadius: 'var(--radius-xl)',
            cursor: 'pointer', fontSize: '0.9rem', fontWeight: 600,
            backdropFilter: 'blur(10px)', display: 'flex', alignItems: 'center', gap: '0.5rem',
            transition: 'var(--transition-standard)'
          }}
          onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.2)'}
          onMouseLeave={e => e.currentTarget.style.background = 'rgba(255,255,255,0.1)'}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          Search
        </button>
      </div>

      <div style={{ position: 'relative', zIndex: 10, marginTop: '-20px' }}>
        {continueWatching.length > 0 && (
          <MediaRow title="Continue Watching" items={continueWatching} onPlay={onSelectItem} />
        )}
        {collections.length > 0 && (
          <MediaRow title="Collections" items={collections} onPlay={(c: any) => onSelectItem(c.items[0])} />
        )}
        <MediaRow title="Recently Added" items={recent} onPlay={onSelectItem} />

        {genres.map(genre => genreItems[genre]?.length > 0 && (
          <MediaRow key={genre} title={genre} items={genreItems[genre]} onPlay={onSelectItem} />
        ))}

        {libraries.map(lib => (
          <LibraryRow 
            key={lib.id} 
            lib={lib} 
            onPlay={onSelectItem} 
          />
        ))}
      </div>
    </div>
  );
};

const LibraryRow: React.FC<{ lib: Library, onPlay: (item: MediaItem) => void }> = ({ lib, onPlay }) => {
  const [items, setItems] = useState<MediaItem[]>([]);

  useEffect(() => {
    const userId = localStorage.getItem('sunset_user_id') || undefined;
    api.getLibraryItems(lib.id, userId).then(data => {
      if (lib.lib_type === 'shows') {
        const seen = new Set<string>();
        const grouped = data.filter(item => {
          if (item.show_title) {
            if (seen.has(item.show_title)) return false;
            seen.add(item.show_title);
            item.title = item.show_title;
            return true;
          }
          return true;
        });
        setItems(grouped.slice(0, 15));
      } else {
        setItems(data.slice(0, 15));
      }
    });
  }, [lib.id, lib.lib_type]);

  return <MediaRow title={lib.name} items={items} onPlay={onPlay} />;
};
