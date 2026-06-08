import React from 'react';
import { Poster } from '../common/Poster';
import type { MediaItem } from '../../types';

interface MediaRowProps {
  title: string;
  items: MediaItem[];
  onPlay: (item: MediaItem) => void;
}

export const MediaRow: React.FC<MediaRowProps> = ({ title, items, onPlay }) => {
  if (items.length === 0) return null;

  return (
    <div style={{ marginBottom: 'var(--spacing-xl)', position: 'relative' }}>
      <h2 style={{ 
        fontSize: '1.4rem', 
        marginBottom: 'var(--spacing-md)', 
        paddingLeft: 'var(--spacing-xl)' 
      }}>
        {title}
      </h2>
      
      <div 
        className="no-scrollbar"
        style={{ 
          display: 'flex', 
          gap: 'var(--spacing-md)', 
          overflowX: 'auto', 
          padding: '0 var(--spacing-xl) 1rem var(--spacing-xl)',
          scrollBehavior: 'smooth',
          WebkitOverflowScrolling: 'touch'
        }}
      >
        {items.map((item) => (
          <Poster 
            key={item.id}
            itemId={item.id}
            title={item.title}
            subtitle={item.year?.toString() || (item.season ? `S${item.season} E${item.episode}` : undefined)}
            onClick={() => onPlay(item)}
          />
        ))}
      </div>
    </div>
  );
};
