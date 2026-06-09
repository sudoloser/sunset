import { useRef, useState, useEffect, useCallback } from 'react';
import { api } from '../../api/client';
import type { MediaItem } from '../../types';
import { 
  PlayIcon, PauseIcon, VolumeHighIcon, VolumeMutedIcon, 
  MaximizeIcon, SkipBackIcon, SkipForwardIcon, BackIcon,
  SubtitlesIcon, EpisodesIcon,
} from '../../components/common/Icons';
import { Button } from '../../components/common/Button';
import { loadSubtitleSettings, type SubtitleSettingsType } from '../settings/SubtitleSettings';

interface VideoPlayerProps {
  item: MediaItem;
  onClose: () => void;
}

const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2];

export const VideoPlayer: React.FC<VideoPlayerProps> = ({ item, onClose }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const seekerRef = useRef<HTMLDivElement>(null);
  const controlsTimeout = useRef<any>(null);

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [subtitleFiles, setSubtitleFiles] = useState<string[]>([]);
  const [activeSubtitle, setActiveSubtitle] = useState<string>('');
  const [showSubtitlePicker, setShowSubtitlePicker] = useState(false);
  const [showEpisodes, setShowEpisodes] = useState(false);
  const [episodes, setEpisodes] = useState<MediaItem[]>([]);
  const [subSettings, setSubSettings] = useState<SubtitleSettingsType>(loadSubtitleSettings);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [showSpeedPicker, setShowSpeedPicker] = useState(false);
  const [showPip, setShowPip] = useState(false);

  const isShow = item.media_type === 'episode' || !!item.show_title;
  const showTitle = item.show_title || item.title;

  // Cloud sync: load remote playback state
  useEffect(() => {
    api.getPlayback(item.id).then(state => {
      if (state && videoRef.current) {
        videoRef.current.currentTime = state.timestamp;
      }
    }).catch(() => {});
  }, [item.id]);

  // Persistence loop: local + cloud sync
  useEffect(() => {
    const interval = setInterval(async () => {
      if (videoRef.current && isPlaying) {
        const ts = videoRef.current.currentTime;
        const dur = videoRef.current.duration;
        const state = { itemId: item.id, timestamp: ts, duration: dur, updatedAt: Date.now() };
        localStorage.setItem(`sunset_playback_${item.id}`, JSON.stringify(state));
        api.savePlayback({ item_id: item.id, timestamp: ts, duration: dur }).catch(() => {});
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [item.id, isPlaying]);

  // Load subtitle files
  useEffect(() => {
    api.getSubtitles(item.id).then(setSubtitleFiles);
  }, [item]);

  // Load episodes for shows
  useEffect(() => {
    if (isShow) {
      api.getShowEpisodes(showTitle).then(setEpisodes);
    }
  }, [isShow, showTitle]);

  // Sync subtitle settings
  useEffect(() => {
    const onStorage = () => setSubSettings(loadSubtitleSettings());
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  // Toggle subtitle track
  useEffect(() => {
    if (!videoRef.current) return;
    const tracks = videoRef.current.textTracks;
    for (let i = 0; i < tracks.length; i++) {
      const track = tracks[i];
      if (track.label === activeSubtitle || (activeSubtitle === '' && track.label === subtitleFiles[0]?.replace('.srt', '').replace('.vtt', ''))) {
        track.mode = 'showing';
      } else {
        track.mode = 'hidden';
      }
    }
  }, [activeSubtitle, subtitleFiles]);

  // Autoplay next episode
  useEffect(() => {
    if (!videoRef.current || !isShow) return;
    const onEnded = () => {
      if (!episodes.length) return;
      const idx = episodes.findIndex(e => e.id === item.id);
      if (idx >= 0 && idx < episodes.length - 1) {
        setShowControls(true);
      }
    };
    videoRef.current.addEventListener('ended', onEnded);
    return () => videoRef.current?.removeEventListener('ended', onEnded);
  }, [episodes, item.id, isShow]);

  // Keyboard shortcuts
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement) return;
      switch (e.key) {
        case ' ':
        case 'k': e.preventDefault(); togglePlay(); break;
        case 'f': e.preventDefault(); toggleFullscreen(); break;
        case 'm': e.preventDefault(); setIsMuted(v => !v); break;
        case 'j': e.preventDefault(); skip(-10); break;
        case 'l': e.preventDefault(); skip(10); break;
        case 'Escape': e.preventDefault(); onClose(); break;
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  });

  const togglePlay = useCallback(() => {
    if (videoRef.current) {
      if (isPlaying) videoRef.current.pause();
      else videoRef.current.play();
    }
  }, [isPlaying]);

  const skip = (seconds: number) => {
    if (videoRef.current) videoRef.current.currentTime += seconds;
  };

  const [seeking, setSeeking] = useState(false);

  const calcSeek = (clientX: number) => {
    if (!seekerRef.current || !videoRef.current) return;
    const rect = seekerRef.current.getBoundingClientRect();
    const x = clientX;
    const pos = Math.max(0, Math.min(1, (x - rect.left) / rect.width));
    videoRef.current.currentTime = pos * videoRef.current.duration;
  };

  const handleSeek = (e: React.MouseEvent | React.TouchEvent) => {
    e.stopPropagation();
    const x = 'touches' in e ? e.touches[0].clientX : e.clientX;
    calcSeek(x);
    setSeeking(true);
  };

  const handleSeekMove = (e: React.MouseEvent | React.TouchEvent) => {
    if (!seeking) return;
    e.preventDefault();
    const x = 'touches' in e ? e.touches[0].clientX : e.clientX;
    calcSeek(x);
  };

  const handleSeekEnd = () => setSeeking(false);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      containerRef.current?.requestFullscreen();
    } else {
      document.exitFullscreen();
    }
  };

  const togglePiP = async () => {
    if (document.pictureInPictureElement) {
      await document.exitPictureInPicture();
      setShowPip(false);
    } else if (videoRef.current) {
      await videoRef.current.requestPictureInPicture();
      setShowPip(true);
    }
  };

  const changeSpeed = (speed: number) => {
    if (videoRef.current) videoRef.current.playbackRate = speed;
    setPlaybackRate(speed);
    setShowSpeedPicker(false);
  };

  const introSkip = () => {
    skip(85);
  };

  const formatTime = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    return `${h > 0 ? h + ':' : ''}${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const showControlsTemporarily = () => {
    setShowControls(true);
    clearTimeout(controlsTimeout.current);
    controlsTimeout.current = setTimeout(() => {
      if (isPlaying) {
        setShowControls(false);
        setShowSubtitlePicker(false);
        setShowEpisodes(false);
        setShowSpeedPicker(false);
      }
    }, 3000);
  };

  const handleActivity = () => showControlsTemporarily();

  const subLabel = (f: string) => f.replace('.srt', '').replace('.vtt', '');

  return (
    <div 
      ref={containerRef}
      style={{ 
        position: 'fixed', inset: 0, background: 'black', zIndex: 2000,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: showControls ? 'default' : 'none',
        userSelect: 'none', WebkitUserSelect: 'none',
        WebkitTouchCallout: 'none'
      }}
      onMouseMove={handleActivity}
      onTouchStart={handleActivity}
      onTouchEnd={handleActivity}
      onContextMenu={(e) => e.preventDefault()}
      onClick={() => {
        if (showControls) { setShowControls(false); setShowSubtitlePicker(false); setShowEpisodes(false); setShowSpeedPicker(false); }
        else showControlsTemporarily();
      }}
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
        {subtitleFiles.map(sub => (
          <track key={sub} kind="subtitles" src={api.getSubtitleUrl(item.id, sub)} label={subLabel(sub)} />
        ))}
      </video>

      <style>{`
        video::cue {
          color: ${subSettings.color} !important;
          background: ${subSettings.backgroundOpacity > 0 ? `rgba(0,0,0,${subSettings.backgroundOpacity})` : 'transparent'} !important;
          font-size: ${subSettings.size}% !important;
          font-family: ${subSettings.font} !important;
          font-weight: ${subSettings.bold ? 700 : 400} !important;
        }
      `}</style>

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
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-lg)' }}>
            <Button variant="ghost" size="icon" onClick={(e) => { e.stopPropagation(); onClose(); }} style={{ color: 'white' }}>
              <BackIcon size={32} />
            </Button>
            <div>
              <h2 style={{ fontSize: '1.4rem', margin: 0, fontWeight: 700 }}>{item.title}</h2>
              {item.show_title && (
                <small style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  {item.show_title} • {item.season === 0 ? 'Specials' : `S${String(item.season).padStart(2, '0')}`} E{String(item.episode).padStart(2, '0')}
                </small>
              )}
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {isShow && episodes.length > 0 && (
              <Button variant="ghost" size="icon" onClick={(e) => { e.stopPropagation(); setShowEpisodes(v => !v); setShowSubtitlePicker(false); setShowSpeedPicker(false); clearTimeout(controlsTimeout.current); }} style={{ color: showEpisodes ? 'var(--primary-color)' : 'white' }}>
                <EpisodesIcon size={26} />
              </Button>
            )}
            {subtitleFiles.length > 0 && (
              <Button variant="ghost" size="icon" onClick={(e) => { e.stopPropagation(); setShowSubtitlePicker(v => !v); setShowEpisodes(false); setShowSpeedPicker(false); clearTimeout(controlsTimeout.current); }} style={{ color: showSubtitlePicker ? 'var(--primary-color)' : 'white' }}>
                <SubtitlesIcon size={26} />
              </Button>
            )}
            <Button variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); setShowSpeedPicker(v => !v); setShowSubtitlePicker(false); setShowEpisodes(false); clearTimeout(controlsTimeout.current); }} style={{ color: showSpeedPicker ? 'var(--primary-color)' : 'white', fontWeight: 700, fontSize: '0.9rem' }}>
              {playbackRate}x
            </Button>
          </div>
        </div>

        {/* Speed Picker */}
        {showSpeedPicker && (
          <div 
            style={{
              position: 'absolute', top: '80px', right: 'var(--spacing-xl)',
              background: 'rgba(20,20,20,0.95)', borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)', padding: '0.5rem',
              minWidth: '100px', zIndex: 10, backdropFilter: 'blur(10px)'
            }}
            onClick={e => e.stopPropagation()}
          >
            {SPEEDS.map(s => (
              <div key={s} onClick={() => changeSpeed(s)}
                style={{
                  padding: '0.6rem 1rem', borderRadius: 'var(--radius-md)', cursor: 'pointer', textAlign: 'center',
                  color: playbackRate === s ? 'var(--primary-color)' : 'white',
                  fontWeight: playbackRate === s ? 700 : 400,
                  transition: 'var(--transition-standard)'
                }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              >
                {s}x
              </div>
            ))}
          </div>
        )}

        {/* Center Play/Pause Large */}
        <div 
          style={{ 
            position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
            display: 'flex', gap: 'var(--spacing-xxl)', alignItems: 'center'
          }}
        >
           <button onClick={(e) => { e.stopPropagation(); skip(-10); }} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
             <SkipBackIcon size={48} />
           </button>
           <button onClick={(e) => { e.stopPropagation(); togglePlay(); }} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
             {isPlaying ? <PauseIcon size={84} /> : <PlayIcon size={84} />}
           </button>
           <button onClick={(e) => { e.stopPropagation(); skip(10); }} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
             <SkipForwardIcon size={48} />
           </button>
        </div>

        {/* Bottom Controls */}
        <div style={{ width: '100%' }}>
          {/* Intro Skip */}
          {currentTime < 120 && (
            <button onClick={(e) => { e.stopPropagation(); introSkip(); }}
              style={{
                position: 'absolute', bottom: '80px', right: 'var(--spacing-xl)',
                background: 'rgba(255,255,255,0.15)', border: 'none', color: 'white',
                padding: '0.5rem 1.2rem', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                fontWeight: 700, fontSize: '0.85rem', backdropFilter: 'blur(10px)',
                transition: 'var(--transition-standard)'
              }}
              onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.25)'}
              onMouseLeave={e => e.currentTarget.style.background = 'rgba(255,255,255,0.15)'}
            >
              Skip Intro
            </button>
          )}

          {/* Seeker Bar */}
          <div 
            ref={seekerRef}
            onMouseDown={handleSeek}
            onTouchStart={handleSeek}
            onMouseMove={handleSeekMove}
            onTouchMove={handleSeekMove}
            onMouseUp={handleSeekEnd}
            onTouchEnd={handleSeekEnd}
            onMouseLeave={handleSeekEnd}
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
               <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-md)' }}>
                 <button onClick={(e) => { e.stopPropagation(); setIsMuted(!isMuted); }} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
                   {isMuted || volume === 0 ? <VolumeMutedIcon size={28} /> : <VolumeHighIcon size={28} />}
                 </button>
                 <input 
                   type="range" min="0" max="1" step="0.1" 
                   value={isMuted ? 0 : volume} 
                   onChange={(e) => { e.stopPropagation(); const v = parseFloat(e.target.value); setVolume(v); setIsMuted(false); if (videoRef.current) videoRef.current.volume = v; }}
                   style={{ width: '80px', accentColor: 'white' }}
                 />
               </div>
               <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'white', minWidth: '100px' }}>
                 {formatTime(currentTime)} / {formatTime(duration)}
               </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-lg)' }}>
               <button onClick={(e) => { e.stopPropagation(); togglePiP(); }} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer', fontSize: '0.8rem', fontWeight: 600 }}>
                 {showPip ? 'Exit PiP' : 'PiP'}
               </button>
               <button onClick={(e) => { e.stopPropagation(); toggleFullscreen(); }} style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer' }}>
                 <MaximizeIcon size={28} />
               </button>
            </div>
          </div>
        </div>
      </div>

      {/* Subtitle Picker */}
      {showSubtitlePicker && subtitleFiles.length > 0 && (
        <div 
          style={{
            position: 'absolute', top: '80px', right: 'var(--spacing-xl)',
            background: 'rgba(20,20,20,0.95)', borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)', padding: '0.5rem', minWidth: '180px', zIndex: 10,
            backdropFilter: 'blur(10px)'
          }}
          onClick={e => e.stopPropagation()}
        >
          <div onClick={() => { setActiveSubtitle(''); setShowSubtitlePicker(false); }}
            style={{
              padding: '0.6rem 1rem', borderRadius: 'var(--radius-md)', cursor: 'pointer',
              color: activeSubtitle === '' ? 'var(--primary-color)' : 'white',
              fontWeight: activeSubtitle === '' ? 700 : 400, transition: 'var(--transition-standard)'
            }}
            onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
          >
            Off
          </div>
          {subtitleFiles.map(sub => {
            const label = subLabel(sub);
            return (
              <div key={sub} onClick={() => { setActiveSubtitle(label); setShowSubtitlePicker(false); }}
                style={{
                  padding: '0.6rem 1rem', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                  color: activeSubtitle === label ? 'var(--primary-color)' : 'white',
                  fontWeight: activeSubtitle === label ? 700 : 400, transition: 'var(--transition-standard)'
                }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              >
                {label}
              </div>
            );
          })}
        </div>
      )}

      {/* Episodes Panel */}
      {showEpisodes && episodes.length > 0 && (
        <div 
          style={{
            position: 'absolute', top: '80px', right: 'var(--spacing-xl)',
            background: 'rgba(20,20,20,0.95)', borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)', padding: '0.5rem',
            minWidth: '280px', maxHeight: '60vh', overflowY: 'auto', zIndex: 10,
            backdropFilter: 'blur(10px)'
          }}
          onClick={e => e.stopPropagation()}
        >
          <div style={{ padding: '0.6rem 1rem', fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-secondary)', borderBottom: '1px solid var(--border-color)', marginBottom: '0.25rem' }}>
            Episodes
          </div>
          {episodes.map(ep => (
            <div key={ep.id}
              onClick={() => { /* navigate to episode */ }}
              style={{
                padding: '0.6rem 1rem', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                display: 'flex', gap: '0.75rem', alignItems: 'center',
                fontWeight: ep.id === item.id ? 700 : 400, transition: 'var(--transition-standard)'
              }}
              onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
            >
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', minWidth: '2rem', textAlign: 'right' }}>
                {ep.episode}
              </span>
              <span style={{ fontSize: '0.9rem' }}>{ep.title}</span>
            </div>
          ))}
        </div>
      )}

      <style>{`
        input[type=range] { cursor: pointer; }
        @keyframes fadeOut { from { opacity: 1; } to { opacity: 0; } }
      `}</style>
    </div>
  );
};
