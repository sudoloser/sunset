import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type MediaItem } from '../api/client';
import type { Session } from '../session';
import { getCachedItem } from '../itemCache';
import { loadPref, savePref, type PlaybackPrefs, type SubtitlePrefs } from '../prefs';
import { mpvAvailable, mpvPlay, updatePresence } from '../discord';

const H265_MP4_CODECS = ['hev1.1.6.L93.B0', 'hvc1.1.6.L93.B0', 'hev1.1.6.L120.90'];
const TRANSCODE_AUDIO_CODECS = ['ac3', 'eac3', 'dts', 'dts-hd', 'dtshd', 'truehd', 'mlp'];

function supportsH265(): boolean {
  if (typeof document === 'undefined') return false;
  const v = document.createElement('video');
  return H265_MP4_CODECS.some(c => v.canPlayType(`video/mp4; codecs="${c}"`) !== '');
}

function canPlayAudio(codec: string): boolean {
  if (typeof document === 'undefined') return true;
  const v = document.createElement('video');
  return (
    v.canPlayType(`audio/mp4; codecs="${codec}"`) !== '' ||
    v.canPlayType(`audio/webm; codecs="${codec}"`) !== ''
  );
}

function needsTranscode(videoCodec: string | null, audioCodec: string | null): boolean {
  const vc = (videoCodec || '').toLowerCase();
  const ac = (audioCodec || '').toLowerCase();
  if (vc === 'hevc' || vc === 'h265') return !supportsH265();
  if (TRANSCODE_AUDIO_CODECS.includes(ac) && !canPlayAudio(ac)) return true;
  return false;
}

function isAnime(item: MediaItem): boolean {
  if (item.media_type !== 'episode') return false;
  return /anim/i.test(item.genres ?? '');
}

// Standard Anime4K v4 shader chains per quality level.
function anime4kShaders(dir: string, level: '' | 'A' | 'B' | 'C'): string[] {
  if (!level || !dir) return [];
  const base = [
    `${dir}/Anime4K_Clamp_Highlights.glsl`,
    `${dir}/Anime4K_Restore_CNN_M.glsl`,
    `${dir}/Anime4K_Upscale_CNN_x2_M.glsl`,
    `${dir}/Anime4K_AutoDownscalePre_x2.glsl`,
    `${dir}/Anime4K_AutoDownscalePre_x4.glsl`,
    `${dir}/Anime4K_Upscale_CNN_x2_XS.glsl`,
  ];
  if (level === 'A') return base;
  if (level === 'B')
    return base.map(f =>
      f.replace('_M.glsl', '_L.glsl').replace('_XS.glsl', '_S.glsl'),
    );
  return [
    `${dir}/Anime4K_Clamp_Highlights.glsl`,
    `${dir}/Anime4K_Restore_CNN_VL.glsl`,
    `${dir}/Anime4K_Upscale_CNN_x2_VL.glsl`,
    `${dir}/Anime4K_Restore_CNN_M.glsl`,
    `${dir}/Anime4K_AutoDownscalePre_x2.glsl`,
    `${dir}/Anime4K_AutoDownscalePre_x4.glsl`,
    `${dir}/Anime4K_Upscale_CNN_x2_XS.glsl`,
  ];
}

const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2];

export function Player({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const videoRef = useRef<HTMLVideoElement>(null);

  const [item] = useState<MediaItem | null>(id ? getCachedItem(id) : null);
  const [src, setSrc] = useState('');
  const [subs, setSubs] = useState<string[]>([]);
  const [activeSub, setActiveSub] = useState<string>('off');
  const [extSubUrl, setExtSubUrl] = useState<string | null>(null);
  const [episodes, setEpisodes] = useState<MediaItem[]>([]);
  const [prefs, setPrefs] = useState<PlaybackPrefs | null>(null);
  const [subPrefs, setSubPrefs] = useState<SubtitlePrefs | null>(null);
  const [mpvOk, setMpvOk] = useState(false);
  const [mpvMsg, setMpvMsg] = useState('');
  const [error, setError] = useState('');
  const [isPlaying, setIsPlaying] = useState(false);
  const [time, setTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const resumeRef = useRef(0);
  const itemRef = useRef(item);
  itemRef.current = item;

  // Load prefs + source + subtitles + episode list.
  useEffect(() => {
    if (!item || !id) {
      setError('Item fell out of the cache. Go back and pick it again.');
      return;
    }
    let cancelled = false;
    (async () => {
      const [playPrefs, subP] = await Promise.all([
        loadPref('sunset_prefs_playback'),
        loadPref('sunset_prefs_subtitles'),
      ]);
      if (cancelled) return;
      setPrefs(playPrefs);
      setSubPrefs(subP);
      setMpvOk(await mpvAvailable(playPrefs.mpvPath));

      // Resume where playback state left off.
      const pb = await api.getPlayback(serverUrl, item.id, session.user_id).catch(() => null);
      if (!cancelled && pb && pb.timestamp > 10 && (!pb.duration || pb.timestamp < pb.duration - 15)) {
        resumeRef.current = pb.timestamp;
      }

      try {
        const codec = await api.getCodec(serverUrl, item.id);
        const forceTranscode = playPrefs.stream === 'transcode';
        const forceDirect = playPrefs.stream === 'direct';
        const useTranscode =
          forceTranscode || (!forceDirect && needsTranscode(codec.video_codec, codec.audio_codec));
        setSrc(useTranscode ? api.transcodeUrl(serverUrl, item.id) : api.streamUrl(serverUrl, item.id));
      } catch {
        if (!cancelled) setError('Could not probe this file. The server may be unreachable.');
        return;
      }

      const subList = await api.getSubtitles(serverUrl, item.id).catch(() => []);
      if (!cancelled && subList.length > 0) {
        setSubs(subList);
        setActiveSub(subList[0]);
      }

      if (item.media_type === 'episode' && item.show_title) {
        const eps = await api.getShowEpisodes(serverUrl, item.show_title, session.user_id).catch(() => []);
        if (!cancelled) setEpisodes(eps.sort((a, b) => (a.episode ?? 0) - (b.episode ?? 0)));
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const save = useCallback(
    (timestamp: number, dur: number | undefined, playing: boolean) => {
      const it = itemRef.current;
      if (!it || !Number.isFinite(timestamp)) return;
      void api
        .savePlayback(serverUrl, {
          item_id: it.id,
          timestamp,
          duration: dur && dur > 0 ? dur : undefined,
          user_id: session.user_id,
          is_playing: playing,
        })
        .catch(() => {});
    },
    [serverUrl, session.user_id],
  );

  // Periodic progress sync + pause/resume presence.
  useEffect(() => {
    const v = videoRef.current;
    if (!v || !item) return;
    const onTime = () => {
      setTime(v.currentTime);
      if (Math.floor(v.currentTime) % 10 === 0) save(v.currentTime, v.duration, !v.paused);
    };
    const onPlay = () => {
      setIsPlaying(true);
      void updatePresence({
        state: item.show_title ? `S${item.season} E${item.episode}` : 'Watching',
        details: item.show_title ?? item.title,
        start_timestamp: Math.floor(Date.now() / 1000),
      });
    };
    const onPause = () => {
      setIsPlaying(false);
      save(v.currentTime, v.duration, false);
      void updatePresence({ state: 'Paused', details: item.show_title ?? item.title });
    };
    v.addEventListener('timeupdate', onTime);
    v.addEventListener('play', onPlay);
    v.addEventListener('pause', onPause);
    return () => {
      v.removeEventListener('timeupdate', onTime);
      v.removeEventListener('play', onPlay);
      v.removeEventListener('pause', onPause);
      save(v.currentTime, v.duration, false);
      void updatePresence({ state: 'Idle', details: 'SunSet' });
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [src]);

  // Apply prefs to the element.
  useEffect(() => {
    const v = videoRef.current;
    if (!v || !prefs) return;
    v.playbackRate = prefs.speed;
    v.volume = prefs.volume;
    v.muted = prefs.muted;
  }, [prefs, src]);

  const toggle = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) void v.play().catch(() => {});
    else v.pause();
  }, []);

  // Keyboard shortcuts.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const v = videoRef.current;
      if (!v) return;
      if (e.key === ' ') {
        e.preventDefault();
        toggle();
      } else if (e.key === 'ArrowRight') v.currentTime += 10;
      else if (e.key === 'ArrowLeft') v.currentTime -= 10;
      else if (e.key === 'f') {
        if (document.fullscreenElement) void document.exitFullscreen();
        else void document.documentElement.requestFullscreen().catch(() => {});
      } else if (e.key === 'm') {
        v.muted = !v.muted;
        if (prefs) {
          const next = { ...prefs, muted: v.muted };
          setPrefs(next);
          void savePref('sunset_prefs_playback', next);
        }
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [toggle, prefs]);

  const openExternalSub = (file: File | null) => {
    if (extSubUrl) URL.revokeObjectURL(extSubUrl);
    if (!file) {
      setExtSubUrl(null);
      return;
    }
    setExtSubUrl(URL.createObjectURL(file));
    setActiveSub('external');
  };

  const playInMpv = async () => {
    if (!item || !prefs) return;
    setMpvMsg('');
    try {
      const shaders = prefs.upscaleAnime || isAnime(item) ? anime4kShaders(prefs.shaderDir, prefs.anime4kLevel) : [];
      await mpvPlay({
        mpvPath: prefs.mpvPath || 'mpv',
        url: api.streamUrl(serverUrl, item.id),
        title: item.show_title ? `${item.show_title} S${item.season}E${item.episode}` : item.title,
        shaders,
      });
      setMpvMsg(shaders.length > 0 ? 'Opened in mpv, upscaled.' : 'Opened in mpv.');
    } catch (e) {
      setMpvMsg(e instanceof Error ? e.message : 'Could not launch mpv. Set its path in Settings, Playback.');
    }
  };

  const idx = episodes.findIndex(e => e.id === item?.id);
  const prev = idx > 0 ? episodes[idx - 1] : null;
  const next = idx >= 0 && idx < episodes.length - 1 ? episodes[idx + 1] : null;
  const goEp = (ep: MediaItem) => {
    const v = videoRef.current;
    if (v) save(v.currentTime, v.duration, false);
    navigate(`/play/${ep.id}`);
  };

  if (!item) {
    return (
      <div className="sl-center">
        <p>{error || 'No item.'}</p>
        <button className="sl-button" onClick={() => navigate(-1)}>
          Back
        </button>
      </div>
    );
  }

  const cueStyle = subPrefs
    ? `video::cue { color: ${subPrefs.color}; font-size: ${subPrefs.size}%; font-family: ${subPrefs.font}; font-weight: ${subPrefs.bold ? 700 : 400}; ${subPrefs.backgroundOpacity > 0 ? `background: rgba(0,0,0,${subPrefs.backgroundOpacity});` : ''} }`
    : '';

  return (
    <div style={{ padding: 'var(--sl-space-md)', maxWidth: 1100, margin: '0 auto' }}>
      <style>{cueStyle}</style>
      <button className="sl-button" onClick={() => navigate(-1)}>
        Back
      </button>
      <h2 style={{ fontSize: 'var(--sl-text-xl)' }}>
        {item.show_title ? `${item.show_title} — ${item.title}` : item.title}
        {isAnime(item) && prefs?.upscaleAnime !== false && <span className="sl-tag"> anime upscale ready</span>}
      </h2>
      {error && <p className="sl-error">{error}</p>}
      {src && (
        <video
          ref={videoRef}
          src={src}
          controls
          autoPlay
          style={{ width: '100%', borderRadius: 'var(--sl-radius-md)', border: '2px solid var(--sl-text)', background: '#000' }}
          onClick={toggle}
          onLoadedMetadata={e => {
            setDuration(e.currentTarget.duration);
            if (resumeRef.current > 0) {
              e.currentTarget.currentTime = resumeRef.current;
              resumeRef.current = 0;
            }
          }}
        >
          {subs.map(name => (
            <track
              key={name}
              kind="subtitles"
              label={name}
              src={api.subtitleUrl(serverUrl, item.id, name)}
              default={name === activeSub}
            />
          ))}
          {extSubUrl && <track kind="subtitles" label="External file" src={extSubUrl} default={activeSub === 'external'} />}
        </video>
      )}

      <div className="sl-card" style={{ marginTop: 'var(--sl-space-md)' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-sm)', alignItems: 'center' }}>
          <button className="sl-button" onClick={toggle}>
            {isPlaying ? 'Pause' : 'Play'}
          </button>
          <span className="sl-tag">
            {Math.floor(time / 60)}:{String(Math.floor(time % 60)).padStart(2, '0')} /{' '}
            {Math.floor(duration / 60)}:{String(Math.floor(duration % 60)).padStart(2, '0')}
          </span>
          <select
            className="sl-input"
            style={{ width: 'auto' }}
            value={prefs?.speed ?? 1}
            onChange={e => {
              if (!prefs) return;
              const speed = Number(e.target.value);
              const next = { ...prefs, speed };
              setPrefs(next);
              void savePref('sunset_prefs_playback', next);
              if (videoRef.current) videoRef.current.playbackRate = speed;
            }}
          >
            {SPEEDS.map(s => (
              <option key={s} value={s}>
                {s}x
              </option>
            ))}
          </select>
          <button
            className="sl-button"
            onClick={() => {
              const v = videoRef.current;
              if (!v || !document.pictureInPictureEnabled) return;
              if (document.pictureInPictureElement) void document.exitPictureInPicture().catch(() => {});
              else void v.requestPictureInPicture().catch(() => {});
            }}
          >
            PiP
          </button>
          <select
            className="sl-input"
            style={{ width: 'auto' }}
            value={activeSub}
            onChange={e => {
              const v = videoRef.current;
              const val = e.target.value;
              setActiveSub(val);
              if (!v) return;
              for (let i = 0; i < v.textTracks.length; i++) {
                v.textTracks[i].mode = v.textTracks[i].label === val ? 'showing' : 'disabled';
              }
            }}
          >
            <option value="off">Subs off</option>
            {subs.map(name => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
            {extSubUrl && <option value="external">External file</option>}
          </select>
          <label className="sl-button" style={{ cursor: 'pointer' }}>
            Load sub file
            <input
              type="file"
              accept=".srt,.vtt"
              style={{ display: 'none' }}
              onChange={e => openExternalSub(e.target.files?.[0] ?? null)}
            />
          </label>
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-sm)', marginTop: 'var(--sl-space-md)' }}>
          {prev && (
            <button className="sl-button" onClick={() => goEp(prev)}>
              Prev: E{prev.episode}
            </button>
          )}
          {next && (
            <button className="sl-button" onClick={() => goEp(next)}>
              Next: E{next.episode}
            </button>
          )}
          {mpvOk ? (
            <button className="sl-button" onClick={() => void playInMpv()}>
              Open in mpv{isAnime(item) && prefs?.anime4kLevel ? ` (Anime4K ${prefs.anime4kLevel})` : ''}
            </button>
          ) : (
            <span style={{ fontSize: 'var(--sl-text-sm)', color: 'var(--sl-text-link)' }}>
              mpv not found — set it up in Settings, Playback for upscaling.
            </span>
          )}
        </div>
        {mpvMsg && <p style={{ fontSize: 'var(--sl-text-sm)' }}>{mpvMsg}</p>}
      </div>
    </div>
  );
}
