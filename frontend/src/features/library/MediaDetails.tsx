import React, { useEffect, useState, useRef } from 'react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { api } from '../../api/client';
import { PlayIcon } from '../../components/common/Icons';
import type { MediaItem } from '../../types';

const ContextMenu: React.FC<{ item: MediaItem; onClose: () => void }> = ({ item, onClose }) => {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) onClose();
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [onClose]);

  const handleAction = async (action: 'stream' | 'download') => {
    try {
      const token = await api.generateMediaToken(item.id);
      const base = action === 'stream' ? `/api/stream/${item.id}` : `/api/media/${item.id}/download`;
      window.open(`${base}?token=${encodeURIComponent(token)}`, '_blank');
    } catch {}
    onClose();
  };

  return (
    <div
      ref={menuRef}
      style={{
        position: 'absolute', top: '100%', right: 0, zIndex: 100,
        background: 'var(--surface-color)', border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-md)', boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
        minWidth: '180px', overflow: 'hidden'
      }}
    >
      {[{
        label: 'Direct Stream', action: 'stream' as const, icon: (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
        )
      }, {
        label: 'Download', action: 'download' as const, icon: (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
        )
      }].map(({ label, action, icon }) => (
        <div
          key={action}
          onClick={() => handleAction(action)}
          style={{
            display: 'flex', alignItems: 'center', gap: '0.75rem',
            padding: '0.75rem 1rem', cursor: 'pointer', fontSize: '0.9rem',
            transition: 'background 0.15s'
          }}
          onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
          onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
        >
          {icon}
          <span>{label}</span>
        </div>
      ))}
    </div>
  );
};

interface MediaDetailsProps {
  item: MediaItem;
  onClose: () => void;
  onPlay: (item: MediaItem) => void;
}

const CastAvatar: React.FC<{ name: string }> = ({ name }) => {
  const initials = name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
      <div style={{
        width: '36px', height: '36px', borderRadius: '50%',
        background: 'var(--surface-variant)', color: 'var(--text-secondary)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: '0.75rem', fontWeight: 700, flexShrink: 0
      }}>
        {initials}
      </div>
      <span style={{ fontSize: '0.9rem' }}>{name}</span>
    </div>
  );
};

export const MediaDetails: React.FC<MediaDetailsProps> = ({ item, onClose, onPlay }) => {
  const [episodes, setEpisodes] = useState<MediaItem[]>([]);
  const [selectedSeason, setSelectedSeason] = useState<number | null>(null);
  const [userRating, setUserRating] = useState<number>(() => {
    try { return parseInt(localStorage.getItem(`sunset_rating_${item.id}`) || '0'); } catch { return 0; }
  });
  const [inMyList, setInMyList] = useState<boolean>(() => {
    try {
      const list = JSON.parse(localStorage.getItem('sunset_mylist') || '[]');
      return list.includes(item.id);
    } catch { return false; }
  });
  const [menuOpen, setMenuOpen] = useState<string | null>(null);

  const toggleMyList = () => {
    try {
      const list: string[] = JSON.parse(localStorage.getItem('sunset_mylist') || '[]');
      if (inMyList) {
        const idx = list.indexOf(item.id);
        if (idx >= 0) list.splice(idx, 1);
      } else {
        list.push(item.id);
      }
      localStorage.setItem('sunset_mylist', JSON.stringify(list));
      setInMyList(!inMyList);
    } catch {}
  };

  const isShow = item.media_type === 'episode' || !!item.show_title;
  const title = item.show_title || item.title;
  const castList = item.cast ? item.cast.split(',').map(s => s.trim()).filter(Boolean) : [];

  const setRating = (r: number) => {
    setUserRating(r);
    if (r > 0) localStorage.setItem(`sunset_rating_${item.id}`, r.toString());
    else localStorage.removeItem(`sunset_rating_${item.id}`);
  };

  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = ''; };
  }, []);

  useEffect(() => {
    if (isShow) {
      api.getShowEpisodes(title).then(data => {
        setEpisodes(data);
        if (data.length > 0) {
          setSelectedSeason(data[0].season ?? 1);
        }
      });
    }
  }, [title, isShow]);

  const backdropUrl = `/api/media/${item.id}/asset/backdrop.jpg`;
  const logoUrl = `/api/media/${item.id}/asset/logo.png`;

  const seasons = Array.from(new Set(episodes.map(e => e.season ?? 1))).sort((a, b) => a - b);
  const filteredEpisodes = episodes.filter(e => (e.season ?? 1) === selectedSeason).sort((a, b) => (a.episode || 0) - (b.episode || 0));

  return (
    <div className="media-details-outer" style={{
      position: 'fixed',
      inset: 0,
      backgroundColor: 'rgba(0,0,0,0.7)',
      zIndex: 1500,
      overflowY: 'auto',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'flex-start',
      backdropFilter: 'blur(30px) saturate(1.5)',
      WebkitBackdropFilter: 'blur(30px) saturate(1.5)'
    }} onClick={onClose}>
      {/* Background Image Layer */}
      <div style={{
        position: 'fixed',
        inset: 0,
        backgroundImage: `url(${backdropUrl})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        opacity: 0.3,
        zIndex: -1
      }} />

      <Card
        className="media-details-card"
        style={{ 
          width: '90%', 
          maxWidth: '1000px', 
          backgroundColor: 'rgba(20, 20, 20, 0.8)', 
          padding: 0, 
          margin: '2rem auto',
          position: 'relative',
          border: '1px solid rgba(255,255,255,0.1)',
          backdropFilter: 'blur(10px)',
          boxShadow: '0 20px 50px rgba(0,0,0,0.5)'
        }}
        onClick={(e: React.MouseEvent) => e.stopPropagation()}
      >
        <style>{`
          @media (max-width: 768px) {
            .media-details-outer {
              padding: 0 !important;
            }
            .media-details-card {
              width: 100% !important;
              max-width: 100% !important;
              margin: 0 !important;
              border-radius: 0 !important;
              border: none !important;
              min-height: 100vh !important;
            }
            .media-details-grid {
              grid-template-columns: 1fr !important;
              gap: 2rem !important;
            }
          }
        `}</style>

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
              <Button size="lg" variant="secondary" onClick={toggleMyList}>
                {inMyList ? '✓ In My List' : '+ My List'}
              </Button>
              <div style={{ position: 'relative' }}>
                <button
                  onClick={e => { e.stopPropagation(); setMenuOpen(menuOpen === item.id ? null : item.id); }}
                  style={{
                    background: 'rgba(255,255,255,0.1)', border: 'none', color: 'white',
                    width: '44px', height: '44px', borderRadius: 'var(--radius-md)',
                    cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '1.2rem', transition: 'var(--transition-standard)'
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.2)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'rgba(255,255,255,0.1)'}
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                    <circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/>
                  </svg>
                </button>
                {menuOpen === item.id && <ContextMenu item={item} onClose={() => setMenuOpen(null)} />}
              </div>
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="media-details-grid" style={{ padding: '3rem', display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '4rem' }}>
          <div>
            <div style={{ display: 'flex', gap: '1rem', color: 'var(--text-secondary)', fontWeight: 600, marginBottom: '1.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <span>{item.year}</span>
              {isShow && <span>{seasons.length} Seasons</span>}
              {item.rating ? <span>TMDB {item.rating.toFixed(1)}/10</span> : null}
              <span style={{ display: 'flex', gap: '0.25rem', alignItems: 'center' }}>
                {[1,2,3,4,5].map(star => (
                  <button key={star} onClick={() => setRating(userRating === star ? 0 : star)}
                    style={{
                      background: 'transparent', border: 'none', cursor: 'pointer',
                      color: star <= userRating ? 'var(--primary-color)' : 'var(--border-color)',
                      fontSize: '1.4rem', padding: 0, lineHeight: 1
                    }}
                  >
                    ★
                  </button>
                ))}
              </span>
            </div>
            <p style={{ fontSize: '1.1rem', lineHeight: '1.6', marginBottom: '2rem' }}>
              {item.description || 'No description available for this title.'}
            </p>

            {isShow && (
              <div style={{ marginTop: '3rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                  <h2 style={{ margin: 0 }}>Episodes</h2>
                  <select 
                    value={selectedSeason ?? undefined} 
                    onChange={e => setSelectedSeason(parseInt(e.target.value))}
                    style={{ 
                      background: 'var(--surface-variant)', color: 'white', border: '1px solid var(--border-color)', 
                      padding: '0.5rem 1rem', borderRadius: 'var(--radius-sm)', outline: 'none'
                    }}
                  >
                    {seasons.map(s => <option key={s} value={s}>{s === 0 ? 'Specials' : `Season ${s}`}</option>)}
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
                      </div>
                      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }} onClick={e => e.stopPropagation()}>
                        <button
                          onClick={e => { e.stopPropagation(); setMenuOpen(menuOpen === ep.id ? null : ep.id); }}
                          style={{
                            background: 'transparent', border: 'none', color: 'var(--text-secondary)',
                            cursor: 'pointer', padding: '4px', display: 'flex', borderRadius: 'var(--radius-sm)',
                            transition: 'var(--transition-standard)'
                          }}
                          onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
                          onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                        >
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                            <circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/>
                          </svg>
                        </button>
                        {menuOpen === ep.id && <ContextMenu item={ep} onClose={() => setMenuOpen(null)} />}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div>
            <div style={{ marginBottom: '2rem' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'block', marginBottom: '0.75rem' }}>Cast:</span>
              {castList.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {castList.map((name, i) => (
                    <CastAvatar key={i} name={name} />
                  ))}
                </div>
              ) : (
                <div style={{ fontSize: '0.9rem', lineHeight: '1.5' }}>
                  Cast information not available.
                </div>
              )}
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
