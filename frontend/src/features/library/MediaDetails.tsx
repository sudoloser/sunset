import React, { useEffect, useState } from 'react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { api } from '../../api/client';
import { PlayIcon } from '../../components/common/Icons';
import type { MediaItem } from '../../types';

interface MediaDetailsProps {
  item: MediaItem;
  onClose: () => void;
  onPlay: (item: MediaItem) => void;
}

export const MediaDetails: React.FC<MediaDetailsProps> = ({ item, onClose, onPlay }) => {
  const [episodes, setEpisodes] = useState<MediaItem[]>([]);
  const [selectedSeason, setSelectedSeason] = useState<number>(1);

  const isShow = item.media_type === 'episode' || !!item.show_title;
  const title = item.show_title || item.title;

  useEffect(() => {
    if (isShow) {
      api.getShowEpisodes(title).then(data => {
        setEpisodes(data);
        if (data.length > 0) {
          setSelectedSeason(data[0].season || 1);
        }
      });
    }
  }, [title, isShow]);

  const backdropUrl = `/api/media/${item.id}/asset/backdrop.jpg`;
  const logoUrl = `/api/media/${item.id}/asset/logo.png`;

  const seasons = Array.from(new Set(episodes.map(e => e.season || 1))).sort((a, b) => a - b);
  const filteredEpisodes = episodes.filter(e => e.season === selectedSeason).sort((a, b) => (a.episode || 0) - (b.episode || 0));

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      backgroundColor: 'rgba(0,0,0,0.9)',
      zIndex: 1500,
      overflowY: 'auto',
      display: 'flex',
      justifyContent: 'center',
      padding: '2rem 0'
    }} onClick={onClose}>
      <Card 
        style={{ 
          width: '90%', 
          maxWidth: '1000px', 
          backgroundColor: 'var(--bg-color)', 
          padding: 0, 
          overflow: 'hidden',
          position: 'relative'
        }}
        onClick={(e: React.MouseEvent) => e.stopPropagation()}
      >
        {/* Hero Area */}
        <div style={{ position: 'relative', height: '50vh', width: '100%' }}>
          <div style={{
            position: 'absolute',
            inset: 0,
            backgroundImage: `url(${backdropUrl})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
          }} />
          <div className="hero-gradient" style={{ position: 'absolute', inset: 0 }} />
          
          <button 
            onClick={onClose}
            style={{ 
              position: 'absolute', top: '1.5rem', right: '1.5rem', 
              background: 'rgba(0,0,0,0.5)', border: 'none', color: 'white', 
              width: '40px', height: '40px', borderRadius: '50%', cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 10
            }}
          >
            ✕
          </button>

          <div style={{ position: 'absolute', bottom: '2rem', left: '3rem', right: '3rem', zIndex: 2 }}>
            <img 
              src={logoUrl} 
              alt={title}
              style={{ maxHeight: '120px', maxWidth: '80%', marginBottom: '1.5rem', filter: 'drop-shadow(0 0 10px rgba(0,0,0,0.8))' }}
              onError={e => e.currentTarget.style.display = 'none'}
            />
            {!logoUrl && <h1 style={{ fontSize: '2.5rem', marginBottom: '1rem' }}>{title}</h1>}
            
            <div style={{ display: 'flex', gap: '1rem' }}>
              <Button size="lg" onClick={() => onPlay(item)}>
                <PlayIcon size={24} /> Play
              </Button>
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div style={{ padding: '3rem', display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '4rem' }}>
          <div>
            <div style={{ display: 'flex', gap: '1rem', color: 'var(--text-secondary)', fontWeight: 600, marginBottom: '1.5rem' }}>
              <span>{item.year}</span>
              {isShow && <span>{seasons.length} Seasons</span>}
            </div>
            <p style={{ fontSize: '1.1rem', lineHeight: '1.6', marginBottom: '2rem' }}>
              {item.description || 'No description available for this title.'}
            </p>

            {isShow && (
              <div style={{ marginTop: '3rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                  <h2 style={{ margin: 0 }}>Episodes</h2>
                  <select 
                    value={selectedSeason} 
                    onChange={e => setSelectedSeason(parseInt(e.target.value))}
                    style={{ 
                      background: 'var(--surface-variant)', color: 'white', border: '1px solid var(--border-color)', 
                      padding: '0.5rem 1rem', borderRadius: 'var(--radius-sm)', outline: 'none'
                    }}
                  >
                    {seasons.map(s => <option key={s} value={s}>Season {s}</option>)}
                  </select>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {filteredEpisodes.map(ep => (
                    <div 
                      key={ep.id}
                      style={{ 
                        display: 'grid', gridTemplateColumns: 'auto 1fr auto', gap: '1.5rem', alignItems: 'center',
                        padding: '1rem', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                        transition: 'background 0.2s'
                      }}
                      onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
                      onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                      onClick={() => onPlay(ep)}
                    >
                      <span style={{ fontSize: '1.2rem', color: 'var(--text-secondary)', width: '30px', textAlign: 'center' }}>
                        {ep.episode}
                      </span>
                      <div>
                        <div style={{ fontWeight: 700 }}>{ep.title}</div>
                        {/* Placeholder for episode description/thumbnail if we add it later */}
                      </div>
                      <PlayIcon size={20} style={{ opacity: 0.5 }} />
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div>
            <div style={{ marginBottom: '2rem' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'block', marginBottom: '0.5rem' }}>Cast:</span>
              <div style={{ fontSize: '0.9rem', lineHeight: '1.5' }}>
                {item.cast || 'Cast information not available.'}
              </div>
            </div>
            <div>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'block', marginBottom: '0.5rem' }}>Genres:</span>
              <div style={{ fontSize: '0.9rem' }}>Cinematic, Immersive, SunSet Original</div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};
