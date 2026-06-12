import React from 'react';
import { Button } from '../../components/common/Button';
import { BackIcon } from '../../components/common/Icons';
import { Poster } from '../../components/common/Poster';
import type { MediaItem } from '../../types';

interface CollectionViewProps {
  name: string;
  items: MediaItem[];
  onSelectItem: (item: MediaItem) => void;
  onBack: () => void;
}

export const CollectionView: React.FC<CollectionViewProps> = ({ name, items, onSelectItem, onBack }) => {
  return (
    <div style={{ paddingBottom: 'var(--spacing-xxl)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '3rem' }}>
        <Button variant="ghost" onClick={onBack}>
          <BackIcon size={20} /> Back
        </Button>
        <h1 style={{ fontSize: '2.5rem' }}>{name}</h1>
      </div>

      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', 
        gap: '2rem' 
      }}>
        {items.map(item => (
          <Poster 
            key={item.id} 
            itemId={item.id}
            title={item.title}
            subtitle={item.year?.toString()}
            onClick={() => onSelectItem(item)}
          />
        ))}
      </div>
    </div>
  );
};
