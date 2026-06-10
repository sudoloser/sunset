import React, { useEffect, useState } from 'react';
import { MediaRow } from '../../components/common/MediaRow';
import { LibraryView } from './LibraryView';
import { api } from '../../api/client';
import type { Library, MediaItem } from '../../types';

interface LibrariesTabProps {
  onSelectItem?: (item: MediaItem) => void;
  isAdmin: boolean;
  onGoToSettings: () => void;
  userId?: string;
}

export const LibrariesTab: React.FC<LibrariesTabProps> = ({ onSelectItem, isAdmin, onGoToSettings, userId }) => {
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [loading, setLoading] = useState(true);
  const [continueWatching, setContinueWatching] = useState<MediaItem[]>([]);
  const [myListItems, setMyListItems] = useState<MediaItem[]>([]);
  const [selectedLibrary, setSelectedLibrary] = useState<Library | null>(null);

  useEffect(() => {
    (async () => {
      const libs = await api.getLibraries();
      setLibraries(libs);
      setLoading(false);

      if (userId) {
        try {
          const [cw, myList] = await Promise.all([
            api.getContinueWatching(userId),
            api.getUserItems(userId),
          ]);
          setContinueWatching(cw);
          setMyListItems(myList);
        } catch {}
      }
    })();
  }, [userId]);

  if (loading) return <div style={{ padding: '2rem' }}>Loading libraries...</div>;

  if (selectedLibrary && onSelectItem) {
    return (
      <LibraryView
        library={selectedLibrary}
        onSelectItem={onSelectItem}
        onBack={() => setSelectedLibrary(null)}
      />
    );
  }

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
