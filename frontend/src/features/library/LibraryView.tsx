import React, { useEffect, useState } from 'react';
import { Button } from '../../components/common/Button';
import { api } from '../../api/client';
import { BackIcon, PlayIcon } from '../../components/common/Icons';
import { Poster } from '../../components/common/Poster';
import type { MediaItem, Library } from '../../types';

interface LibraryViewProps {
  library: Library;
  onSelectItem: (item: MediaItem) => void;
  onBack: () => void;
}

export const LibraryView: React.FC<LibraryViewProps> = ({ library, onSelectItem, onBack }) => {
  const [items, setItems] = useState<MediaItem[]>([]);
  const [selectedShow, setSelectedShow] = useState<string | null>(null);

  useEffect(() => {
    api.getLibraryItems(library.id).then(setItems);
  }, [library.id]);

  const groupedShows = items.reduce((acc, item) => {
    // If it's a shows library, we group everything.
    // Use show_title if available, otherwise use title as the show name.
    const title = item.show_title || item.title;
    if (!acc[title]) acc[title] = [];
    acc[title].push(item);
    return acc;
  }, {} as Record<string, MediaItem[]>);

  if (library.lib_type === 'shows' && selectedShow) {
    const showEpisodes = groupedShows[selectedShow];
    const seasons = Object.entries(showEpisodes.reduce((acc, ep) => {
      if (!acc[ep.season!]) acc[ep.season!] = [];
      acc[ep.season!].push(ep);
      return acc;
    }, {} as Record<number, MediaItem[]>)).sort(([a], [b]) => Number(a) - Number(b));

    return (
      <div style={{ paddingBottom: 'var(--spacing-xxl)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
          <Button variant="ghost" onClick={() => setSelectedShow(null)}>
            <BackIcon size={20} /> Back to Shows
          </Button>
          <h2 style={{ fontSize: '2rem' }}>{selectedShow}</h2>
        </div>
        
        {seasons.map(([season, episodes]) => (
          <div key={season} style={{ marginBottom: '2.5rem' }}>
            <h3 style={{ color: 'var(--primary-color)', marginBottom: '1.2rem', fontSize: '1.4rem' }}>Season {season}</h3>
            <div style={{ display: 'grid', gap: '0.75rem', maxWidth: '1000px' }}>
              {episodes.sort((a, b) => a.episode! - b.episode!).map(ep => (
                <div 
                  key={ep.id} 
                  style={{ 
                    padding: '1.2rem', 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center',
                    cursor: 'pointer',
                    backgroundColor: 'var(--surface-color)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)',
                    transition: 'var(--transition-standard)'
                  }}
                  onClick={() => onSelectItem(ep)}
                  onMouseEnter={e => e.currentTarget.style.backgroundColor = 'var(--surface-variant)'}
                  onMouseLeave={e => e.currentTarget.style.backgroundColor = 'var(--surface-color)'}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>EPISODE {ep.episode}</span>
                    <span style={{ fontSize: '1.1rem', fontWeight: 600 }}>{ep.title}</span>
                  </div>
                  <PlayIcon size={24} style={{ color: 'var(--primary-color)' }} />
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div style={{ paddingBottom: 'var(--spacing-xxl)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '3rem' }}>
        <Button variant="ghost" onClick={onBack}>
          <BackIcon size={20} /> Back
        </Button>
        <h1 style={{ fontSize: '2.5rem' }}>{library.name}</h1>
      </div>

      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', 
        gap: '2rem' 
      }}>
        {library.lib_type === 'movies' ? (
          items.map(item => (
            <Poster 
              key={item.id} 
              itemId={item.id}
              title={item.title}
              subtitle={item.year?.toString()}
              onClick={() => onSelectItem(item)}
            />
          ))
        ) : (
          Object.keys(groupedShows).map(showTitle => (
            <Poster 
              key={showTitle} 
              itemId={groupedShows[showTitle][0]?.id} // Use first episode for poster
              title={showTitle}
              subtitle={`${groupedShows[showTitle].length} Episodes`}
              onClick={() => onSelectItem(groupedShows[showTitle][0])}
            />
          ))
        )}
      </div>
    </div>
  );
};
