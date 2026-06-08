import React from 'react';
import { Button } from '../common/Button';
import { PlayIcon, InfoIcon } from '../common/Icons';
import type { MediaItem } from '../../types';

interface HeroProps {
  item?: MediaItem;
  onPlay: (item: MediaItem) => void;
}

export const Hero: React.FC<HeroProps> = ({ item, onPlay }) => {
  if (!item) return (
    <div style={{ height: '70vh', backgroundColor: '#111', marginBottom: 'var(--spacing-xl)' }} />
  );

  const backdropUrl = `/api/media/${item.id}/asset/backdrop.jpg`;
  const logoUrl = `/api/media/${item.id}/asset/logo.png`;

  return (
    <div 
      style={{
        position: 'relative',
        height: '80vh',
        width: '100%',
        marginBottom: 'var(--spacing-xl)',
        display: 'flex',
        alignItems: 'center',
        padding: 'var(--spacing-xxl)',
        overflow: 'hidden'
      }}
    >
      {/* Background Image Placeholder / Video */}
      <div 
        style={{
          position: 'absolute',
          inset: 0,
          backgroundColor: '#1a1a1a',
          backgroundImage: `url(${backdropUrl})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          zIndex: 0
        }}
      />
      
      {/* Gradients */}
      <div className="hero-gradient" style={{ position: 'absolute', inset: 0, zIndex: 1 }} />
      <div style={{ 
        position: 'absolute', 
        inset: 0, 
        background: 'linear-gradient(to right, rgba(0,0,0,0.8) 0%, transparent 60%)',
        zIndex: 1 
      }} />

      <div style={{ position: 'relative', zIndex: 2, maxWidth: '600px' }}>
        <img 
          src={logoUrl} 
          alt={item.title}
          style={{ 
            maxHeight: '150px', 
            maxWidth: '100%', 
            marginBottom: 'var(--spacing-lg)',
            filter: 'drop-shadow(0 0 10px rgba(0,0,0,0.5))'
          }}
          onError={(e) => e.currentTarget.style.display = 'none'}
        />
        <h1 style={{ marginBottom: 'var(--spacing-md)', lineHeight: '1.1' }}>
          {item.title}
        </h1>
        <p style={{ 
          fontSize: '1.2rem', 
          color: 'var(--text-secondary)', 
          marginBottom: 'var(--spacing-xl)',
          display: '-webkit-box',
          WebkitLineClamp: 3,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden'
        }}>
          {item.year && `${item.year} • `}
          Experience the latest cinematic addition to your SunSet library. 
          High-quality streaming and immersive sound await.
        </p>
        
        <div style={{ display: 'flex', gap: 'var(--spacing-md)' }}>
          <Button size="lg" onClick={() => onPlay(item)}>
            <PlayIcon size={24} /> Play
          </Button>
          <Button variant="secondary" size="lg">
            <InfoIcon size={24} /> More Info
          </Button>
        </div>
      </div>
    </div>
  );
};
