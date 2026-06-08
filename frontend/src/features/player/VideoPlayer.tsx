import { useRef, useState, useEffect, useCallback } from 'react';
import { api } from '../../api/client';
import type { MediaItem } from '../../types';
import { 
  PlayIcon, 
  PauseIcon, 
  VolumeHighIcon, 
  VolumeMutedIcon, 
  MaximizeIcon, 
  SkipBackIcon, 
  SkipForwardIcon,
  BackIcon
} from '../../components/common/Icons';
import { Button } from '../../components/common/Button';

interface VideoPlayerProps {
  item: MediaItem;
  onClose: () => void;
}

export const VideoPlayer: React.FC<VideoPlayerProps> = ({ item, onClose }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const seekerRef = useRef<HTMLDivElement>(null);

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [subtitles, setSubtitles] = useState<string[]>([]);
  
  const controlsTimeout = useRef<any>(null);

  // Resume logic
  useEffect(() => {
    const saved = localStorage.getItem(`sunset_playback_${item.id}`);
    if (saved && videoRef.current) {
      const state = JSON.parse(saved);
      videoRef.current.currentTime = state.timestamp;
    }
  }, [item.id]);

  // Persistence loop
  useEffect(() => {
    const interval = setInterval(() => {
      if (videoRef.current && isPlaying) {
        const state = {
          itemId: item.id,
          timestamp: videoRef.current.currentTime,
          duration: videoRef.current.duration,
          updatedAt: Date.now()
        };
        localStorage.setItem(`sunset_playback_${item.id}`, JSON.stringify(state));
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [item.id, isPlaying]);

  useEffect(() => {
    api.getSubtitles(item.id).then(setSubtitles);
  }, [item]);

  const togglePlay = useCallback(() => {
    if (videoRef.current) {
      if (isPlaying) videoRef.current.pause();
      else videoRef.current.play();
    }
  }, [isPlaying]);

  const skip = (seconds: number) => {
    if (videoRef.current) videoRef.current.currentTime += seconds;
  };

  const handleSeek = (e: React.MouseEvent | React.TouchEvent) => {
    if (!seekerRef.current || !videoRef.current) return;
    const rect = seekerRef.current.getBoundingClientRect();
    const x = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const pos = Math.max(0, Math.min(1, (x - rect.left) / rect.width));
    videoRef.current.currentTime = pos * videoRef.current.duration;
  };

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      containerRef.current?.requestFullscreen();
    } else {
      document.exitFullscreen();
    }
  };

  const formatTime = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    return `${h > 0 ? h + ':' : ''}${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const handleActivity = () => {
    setShowControls(true);
    clearTimeout(controlsTimeout.current);
    controlsTimeout.current = setTimeout(() => {
      if (isPlaying) setShowControls(false);
    }, 3000);
  };

  return (
    <div 
      ref={containerRef}
      style={{ 
        position: 'fixed', inset: 0, background: 'black', zIndex: 2000,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: showControls ? 'default' : 'none'
      }}
      onMouseMove={handleActivity}
      onClick={() => {
        if (showControls) setShowControls(false);
        else handleActivity();
      }}
      onTouchStart={handleActivity}
    >
      <video
        ref={videoRef}
        src={api.getStreamUrl(item.id)}
        style={{ width: '100%', height: '100%', objectFit: 'contain' }}
        autoPlay
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onTimeUpdate={(e) => setCurrentTime(e.currentTarget.currentTime)}
        onLoadedMetadata={(e) => setDuration(e.currentTarget.duration)}
        onClick={(e) => { e.stopPropagation(); togglePlay(); }}
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

      {/* Overlay Controls */}
      <div 
        style={{ 
          position: 'absolute', inset: 0, 
          display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
          background: showControls ? 'linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 20%, transparent 80%, rgba(0,0,0,0.8) 100%)' : 'transparent',
          opacity: showControls ? 1 : 0,
          transition: 'opacity 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
          pointerEvents: showControls ? 'all' : 'none',
          padding: 'var(--spacing-xl)'
        }}
      >
        {/* Top Header */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-lg)' }}>
          <Button variant="ghost" size="icon" onClick={onClose} style={{ color: 'white' }}>
            <BackIcon size={32} />
          </Button>
          <div>
            <h2 style={{ fontSize: '1.4rem', margin: 0, fontWeight: 700 }}>{item.title}</h2>
            {item.show_title && (
              <small style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                {item.show_title} • S{item.season} E{item.episode}
              </small>
            )}
          </div>
        </div>

        {/* Center Play/Pause Large */}
        <div 
          style={{ 
            position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
            display: 'flex', gap: 'var(--spacing-xxl)', alignItems: 'center'
          }}
        >
           <button onClick={() => skip(-10)} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
             <SkipBackIcon size={48} />
           </button>
           <button 
             onClick={togglePlay} 
             style={{ 
               background: 'transparent', border: 'none', color: 'white', cursor: 'pointer',
               display: 'flex', alignItems: 'center', justifyContent: 'center'
             }}
           >
             {isPlaying ? <PauseIcon size={84} /> : <PlayIcon size={84} />}
           </button>
           <button onClick={() => skip(10)} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
             <SkipForwardIcon size={48} />
           </button>
        </div>

        {/* Bottom Controls */}
        <div style={{ width: '100%' }}>
          {/* Seeker Bar */}
          <div 
            ref={seekerRef}
            onMouseDown={handleSeek}
            style={{ 
              height: '6px', width: '100%', background: 'rgba(255,255,255,0.3)', 
              borderRadius: '3px', marginBottom: 'var(--spacing-lg)', cursor: 'pointer',
              position: 'relative'
            }}
          >
             <div style={{ 
               height: '100%', width: `${(currentTime / duration) * 100}%`, 
               background: 'var(--primary-color)', borderRadius: '3px',
               position: 'relative'
             }}>
               <div style={{ 
                 position: 'absolute', right: '-8px', top: '-5px', 
                 width: '16px', height: '16px', borderRadius: '50%', 
                 background: 'var(--primary-color)', boxShadow: '0 0 10px rgba(0,0,0,0.5)'
               }} />
             </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-xl)' }}>
               <button onClick={togglePlay} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
                 {isPlaying ? <PauseIcon size={32} /> : <PlayIcon size={32} />}
               </button>
               
               <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-md)' }}>
                 <button onClick={() => setIsMuted(!isMuted)} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
                   {isMuted || volume === 0 ? <VolumeMutedIcon size={28} /> : <VolumeHighIcon size={28} />}
                 </button>
                 <input 
                   type="range" min="0" max="1" step="0.1" 
                   value={isMuted ? 0 : volume} 
                   onChange={(e) => { setVolume(parseFloat(e.target.value)); setIsMuted(false); if (videoRef.current) videoRef.current.volume = parseFloat(e.target.value); }}
                   style={{ width: '80px', accentColor: 'white' }}
                 />
               </div>

               <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'white', minWidth: '100px' }}>
                 {formatTime(currentTime)} / {formatTime(duration)}
               </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-lg)' }}>
               <button onClick={toggleFullscreen} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
                 <MaximizeIcon size={28} />
               </button>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        input[type=range] {
          cursor: pointer;
        }
        @keyframes fadeOut {
          from { opacity: 1; }
          to { opacity: 0; }
        }
      `}</style>
    </div>
  );
};
