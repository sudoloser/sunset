import React, { useEffect, useState } from 'react';
import { Card } from '../../components/common/Card';
import { MediaRow } from '../../components/common/MediaRow';
import { api } from '../../api/client';
import type { Library, MediaItem } from '../../types';
import { MovieIcon, TVIcon } from '../../components/common/Icons';

interface LibrariesTabProps {
  onSelectLibrary: (lib: Library) => void;
  onSelectItem?: (item: MediaItem) => void;
  isAdmin: boolean;
  onGoToSettings: () => void;
}

export const LibrariesTab: React.FC<LibrariesTabProps> = ({ onSelectLibrary, onSelectItem, isAdmin, onGoToSettings }) => {
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [loading, setLoading] = useState(true);
  const [continueWatching, setContinueWatching] = useState<MediaItem[]>([]);
  const [myListItems, setMyListItems] = useState<MediaItem[]>([]);
  useEffect(() => {
    (async () => {
      const libs = await api.getLibraries();
      setLibraries(libs);
      setLoading(false);

      // Fetch all items for CW and My List
      const all: MediaItem[] = [];
      for (const lib of libs) {
        try {
          const items = await api.getLibraryItems(lib.id);
          all.push(...items);
        } catch {}
      }

      // Continue Watching
      const cw: MediaItem[] = [];
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith('sunset_playback_')) {
          const itemId = key.replace('sunset_playback_', '');
          const match = all.find(r => r.id === itemId);
          if (match) cw.push(match);
        }
      }
      setContinueWatching(cw);

      // My List
      try {
        const ids: string[] = JSON.parse(localStorage.getItem('sunset_mylist') || '[]');
        const found = all.filter(r => ids.includes(r.id));
        setMyListItems(found);
      } catch {}
    })();
  }, []);

  if (loading) return <div style={{ padding: '2rem' }}>Loading libraries...</div>;

  if (libraries.length === 0) {
    return (
      <div style={{ 
        display: 'flex', 
        flexDirection: 'column', 
        alignItems: 'center', 
        justifyContent: 'center', 
        height: '60vh',
        textAlign: 'center'
      }}>
        <h2 style={{ fontSize: '2rem', marginBottom: '1rem' }}>No Libraries Found</h2>
        <p style={{ color: 'var(--text-secondary)', maxWidth: '400px', marginBottom: '2rem' }}>
          You haven't added any media libraries yet. Add your first library in settings to start watching.
        </p>
        {isAdmin && (
          <button 
            onClick={onGoToSettings}
            style={{
              background: 'var(--primary-color)',
              color: 'white',
              border: 'none',
              padding: '1rem 2rem',
              borderRadius: 'var(--radius-md)',
              fontWeight: 700,
              cursor: 'pointer'
            }}
          >
            Go to Settings
          </button>
        )}
      </div>
    );
  }

  return (
    <div style={{ padding: '2rem 0' }}>
      {continueWatching.length > 0 && onSelectItem && (
        <div style={{ marginBottom: '2rem' }}>
          <MediaRow title="Continue Watching" items={continueWatching} onPlay={onSelectItem} />
        </div>
      )}

      {myListItems.length > 0 && onSelectItem && (
        <div style={{ marginBottom: '2rem' }}>
          <MediaRow title="My List" items={myListItems} onPlay={onSelectItem} />
        </div>
      )}
    </div>
  );
};
