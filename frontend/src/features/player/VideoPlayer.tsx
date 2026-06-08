import React, { useRef, useState, useEffect } from 'react';
import { api } from '../../api/client';
import type { MediaItem } from '../../types';
import { Button } from '../../components/common/Button';

interface VideoPlayerProps {
  item: MediaItem;
  onClose: () => void;
}

export const VideoPlayer: React.FC<VideoPlayerProps> = ({ item, onClose }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [episodes, setEpisodes] = useState<MediaItem[]>([]);
  const [isEpisodeListOpen, setIsEpisodeListOpen] = useState(false);
  const [subtitles, setSubtitles] = useState<string[]>([]);
  const controlsTimeout = useRef<any>(null);

  useEffect(() => {
    if (item.media_type === 'episode' && item.show_title) {
      api.getShowEpisodes(item.show_title).then(setEpisodes);
    }
    api.getSubtitles(item.id).then(setSubtitles);
  }, [item]);

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) videoRef.current.pause();
      else videoRef.current.play();
      setIsPlaying(!isPlaying);
    }
  };

  const skip = (seconds: number) => {
    if (videoRef.current) videoRef.current.currentTime += seconds;
  };

  const handleActivity = () => {
    setShowControls(true);
    clearTimeout(controlsTimeout.current);
    controlsTimeout.current = setTimeout(() => setShowControls(false), 3000);
  };


  return (
    <div 
      style={{ 
        position: 'fixed', inset: 0, background: 'black', zIndex: 1000,
        display: 'flex', alignItems: 'center', justifyContent: 'center'
      }}
      onMouseMove={handleActivity}
      onTouchStart={handleActivity}
    >
      <video
        ref={videoRef}
        src={api.getStreamUrl(item.id)}
        style={{ width: '100%', maxHeight: '100%' }}
        autoPlay
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
      >
        {subtitles.map(sub => (
          <track 
            key={sub}
            kind="subtitles"
            src={api.getSubtitleUrl(item.id, sub)}
            label={sub.replace('.srt', '').replace('.vtt', '')}
            default
          />
        ))}
      </video>

      {showControls && (
        <div 
          className="glass"
          style={{ 
            position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.3)',
            display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
            padding: '2rem', transition: 'opacity 0.3s'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <h2 style={{ fontSize: '1.2rem', marginBottom: '0.2rem' }}>{item.title}</h2>
              {item.show_title && (
                <small style={{ color: 'white', opacity: 0.8 }}>
                  {item.show_title} • S{item.season} E{item.episode}
                </small>
              )}
            </div>
            <Button variant="secondary" onClick={onClose} style={{ background: 'rgba(255,255,255,0.1)' }}>Exit</Button>
          </div>

          <div style={{ display: 'flex', gap: '3rem', alignItems: 'center', justifyContent: 'center' }}>
            <button onClick={() => skip(-10)} style={{ background: 'transparent', border: 'none', color: 'white', fontSize: '2rem', cursor: 'pointer' }}>⏪</button>
            <button 
              onClick={togglePlay} 
              style={{ 
                width: '80px', height: '80px', borderRadius: '50%', background: 'var(--primary-color)', 
                border: 'none', color: 'white', fontSize: '2.5rem', cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center'
              }}
            >
              {isPlaying ? '⏸' : '▶'}
            </button>
            <button onClick={() => skip(10)} style={{ background: 'transparent', border: 'none', color: 'white', fontSize: '2rem', cursor: 'pointer' }}>⏩</button>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', gap: '1rem' }}>
              {episodes.length > 0 && (
                <Button variant="secondary" onClick={() => setIsEpisodeListOpen(true)} style={{ background: 'rgba(255,255,255,0.1)' }}>Episodes</Button>
              )}
            </div>
            {/* Next button logic would go here */}
          </div>
        </div>
      )}

      {isEpisodeListOpen && (
        <div 
          className="glass" 
          style={{ 
            position: 'absolute', top: 0, right: 0, width: '320px', height: '100%', 
            padding: '2rem', overflowY: 'auto', zIndex: 1100, borderLeft: '1px solid var(--border-color)'
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem' }}>
            <h3>Episodes</h3>
            <button onClick={() => setIsEpisodeListOpen(false)} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>✕</button>
          </div>
          {episodes.map(ep => (
            <div 
              key={ep.id} 
              className="glass"
              style={{ 
                padding: '1rem', marginBottom: '0.75rem', cursor: 'pointer',
                border: ep.id === item.id ? '1px solid var(--primary-color)' : '1px solid var(--border-color)'
              }}
              onClick={() => {
                // In a real app, we'd navigate here
                setIsEpisodeListOpen(false);
              }}
            >
              <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>S{ep.season} E{ep.episode}</div>
              <div style={{ fontWeight: 500 }}>{ep.title}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
