import React, { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { Hero } from '../../components/common/Hero';
import { MediaRow } from '../../components/common/MediaRow';
import type { MediaItem, Library } from '../../types';

interface DashboardProps {
  onSelectItem: (item: MediaItem) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onSelectItem }) => {
  const [recent, setRecent] = useState<MediaItem[]>([]);
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [featured, setFeatured] = useState<MediaItem | undefined>();

  useEffect(() => {
    const load = async () => {
      const [recentData, libsData] = await Promise.all([
        api.getRecentlyAdded(),
        api.getLibraries()
      ]);
      setRecent(recentData);
      setLibraries(libsData);
      if (recentData.length > 0) {
        setFeatured(recentData[0]);
      }
    };
    load();
  }, []);

  return (
    <div style={{ paddingBottom: 'var(--spacing-xxl)', marginTop: 'calc(-1 * var(--header-height))' }}>
      <Hero item={featured} onPlay={onSelectItem} />
      
      <div style={{ position: 'relative', zIndex: 10, marginTop: '-100px' }}>
        <MediaRow title="Recently Added" items={recent} onPlay={onSelectItem} />
        
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
    api.getLibraryItems(lib.id).then(data => setItems(data.slice(0, 15)));
  }, [lib.id]);

  return <MediaRow title={lib.name} items={items} onPlay={onPlay} />;
};
