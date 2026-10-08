import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, type MediaItem } from '../api/client';
import type { Session } from '../session';
import { cacheItems, getCachedItem, sortEpisodes } from '../itemCache';
import { getCoverUrl } from '../coverArt';
import { loadPref, savePref, type PlaybackPrefs, type SubtitlePrefs } from '../prefs';
import {
  gstreamerMissing,
  mpvAvailable,
  mpvCmd,
  mpvStartSession,
  mpvStatusPoll,
  mpvStopSession,
  setFullscreen,
  updatePresence,
  type MpvStatus,
} from '../discord';

// Containers browsers (and WebKitGTK in particular) demux natively.
// Everything else goes through the server's remux route even when the
// codecs themselves are playable — direct MKV stalls where mpv flies.
const NATIVE_CONTAINERS = ['mp4', 'm4v', 'mov'];

function fileExt(item: MediaItem): string {
  const base = item.file_path.split('?')[0];
  const dot = base.lastIndexOf('.');
  return dot >= 0 ? base.slice(dot + 1).toLowerCase() : '';
}

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
    `${dir}/Anime4K_Upscale_CNN_x2_S.glsl`,
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
    `${dir}/Anime4K_Upscale_CNN_x2_S.glsl`,
  ];
}

const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2];

function fmtTime(total: number): string {
  if (!Number.isFinite(total) || total < 0) return '0:00';
  return `${Math.floor(total / 60)}:${String(Math.floor(total % 60)).padStart(2, '0')}`;
}

export function Player({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const videoRef = useRef<HTMLVideoElement>(null);

  // Look the item up fresh every render: navigating play -> play must not
  // keep showing (and probing) the previous episode.
  const locationItem = (location as unknown as { state?: MediaItem | null }).state ?? null;
  const item: MediaItem | null =
    (locationItem && id && locationItem.id === id ? locationItem : null) ??
    (id ? getCachedItem(id) : null);
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
  const [muted, setMuted] = useState(false);
  const [volume, setVolume] = useState(1);
  const [showEpisodes, setShowEpisodes] = useState(false);
  const [autoNext, setAutoNext] = useState(true);
  const [missingGst, setMissingGst] = useState<string[]>([]);
  const [preparing, setPreparing] = useState(true);
  const [notice, setNotice] = useState('');
  const [isTranscode, setIsTranscode] = useState(false);
  const [fsVideo, setFsVideo] = useState(false);
  const [mpvId, setMpvId] = useState<string | null>(null);
  const [mpvState, setMpvState] = useState<MpvStatus | null>(null);
  const mpvIdRef = useRef<string | null>(null);
  mpvIdRef.current = mpvId;
  const episodesRef = useRef<MediaItem[]>([]);
  episodesRef.current = episodes;
  const autoNextRef = useRef(autoNext);
  autoNextRef.current = autoNext;
  const resumeRef = useRef(0);
  const coverRef = useRef<string | null>(null);
  // Transcode pipes restart at 0 from the ?start= offset, so absolute
  // position = currentTime + offset. Direct/remux streams have offset 0.
  const offsetRef = useRef(0);
  const transcodeRef = useRef(false);
  const itemRef = useRef(item);
  itemRef.current = item;

  // Load prefs + source + subtitles + episode list.
  useEffect(() => {
    if (!item || !id) {
      setError('Item fell out of the cache. Go back and pick it again.');
      return;
    }
    // Fresh episode: drop everything from the previous one.
    setSrc('');
    setSubs([]);
    setActiveSub('off');
    setEpisodes([]);
    setError('');
    setPreparing(true);
    setTime(0);
    setDuration(0);
    let cancelled = false;
    (async () => {
      const [playPrefs, subP, discordP] = await Promise.all([
        loadPref('sunset_prefs_playback'),
        loadPref('sunset_prefs_subtitles'),
        loadPref('sunset_prefs_discord'),
      ]);
      if (cancelled) return;
      setPrefs(playPrefs);
      setSubPrefs(subP);
      coverRef.current = null;
      // Hosted cover for Discord art; uploads once, then cached locally.
      void getCoverUrl(serverUrl, item.id, discordP.imgurClientId ?? '').then(url => {
        if (!cancelled) coverRef.current = url;
      });
      setMuted(playPrefs.muted);
      setVolume(playPrefs.volume);
      setMpvOk(await mpvAvailable(playPrefs.mpvPath));
      setMissingGst(await gstreamerMissing());

      // Resume where playback state left off. Transcode pipes restart at 0
      // from the ?start= offset, so resume goes into the URL, not currentTime.
      const pb = await api.getPlayback(serverUrl, item.id, session.user_id).catch(() => null);
      const resumeAt =
        pb && pb.timestamp > 10 && (!pb.duration || pb.timestamp < pb.duration - 15)
          ? Math.floor(pb.timestamp)
          : 0;

      try {
        const codec = await api.getCodec(serverUrl, item.id);
        const containerOk = NATIVE_CONTAINERS.includes(fileExt(item));
        const codecsOk = !needsTranscode(codec.video_codec, codec.audio_codec);
        let next: string;
        let offset = 0;
        let transcode = false;
        if (playPrefs.stream === 'transcode' || (!codecsOk && playPrefs.stream !== 'direct')) {
          transcode = true;
          offset = resumeAt;
          next = api.transcodeUrl(serverUrl, item.id, resumeAt);
        } else if (containerOk) {
          next = api.streamUrl(serverUrl, item.id);
          if (resumeAt > 0) resumeRef.current = resumeAt;
        } else {
          // Playable codecs, foreign container (MKV, AVI, ...): remuxed
          // fragmented MP4 starts in seconds and caches for fast seeks.
          next = api.remuxUrl(serverUrl, item.id);
          if (resumeAt > 0) resumeRef.current = resumeAt;
        }
        if (cancelled) return;
        transcodeRef.current = transcode;
        offsetRef.current = offset;
        setIsTranscode(transcode);
        setSrc(next);
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
        if (!cancelled) {
          cacheItems(eps);
          setEpisodes(sortEpisodes(eps));
        }
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  // Desktop presence is native-only: always report is_playing=false so the
  // server pipeline never fires for desktop plays (no double presence).
  const save = useCallback(
    (timestamp: number, dur: number | undefined, _playing: boolean) => {
      const it = itemRef.current;
      if (!it || !Number.isFinite(timestamp)) return;
      const absolute = timestamp + (transcodeRef.current ? offsetRef.current : 0);
      void api
        .savePlayback(serverUrl, {
          item_id: it.id,
          timestamp: absolute,
          duration: dur && dur > 0 ? dur : undefined,
          user_id: session.user_id,
          is_playing: false,
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
        large_image: coverRef.current ?? undefined,
        large_text: item.show_title ?? item.title,
        start_timestamp: Math.floor(Date.now() / 1000),
      });
    };
    const onPause = () => {
      setIsPlaying(false);
      save(v.currentTime, v.duration, false);
      void updatePresence({
        state: 'Paused',
        details: item.show_title ?? item.title,
        large_image: coverRef.current ?? undefined,
        large_text: item.show_title ?? item.title,
      });
    };
    // Transcode pipes can't byte-seek: restart ffmpeg at the wanted offset.
    const onSeeking = () => {
      if (!transcodeRef.current || !itemRef.current) return;
      const absolute = Math.floor(v.currentTime + offsetRef.current);
      if (Math.abs(absolute - offsetRef.current) < 3) return;
      save(v.currentTime, v.duration, !v.paused);
      offsetRef.current = absolute;
      setPreparing(true);
      setSrc(api.transcodeUrl(serverUrl, itemRef.current.id, absolute));
    };
    v.addEventListener('timeupdate', onTime);
    v.addEventListener('play', onPlay);
    v.addEventListener('pause', onPause);
    v.addEventListener('seeking', onSeeking);
    return () => {
      v.removeEventListener('timeupdate', onTime);
      v.removeEventListener('play', onPlay);
      v.removeEventListener('pause', onPause);
      v.removeEventListener('seeking', onSeeking);
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

  const persistPrefs = useCallback(
    (patch: Partial<PlaybackPrefs>) => {
      setPrefs(prev => {
        if (!prev) return prev;
        const next = { ...prev, ...patch };
        void savePref('sunset_prefs_playback', next);
        return next;
      });
    },
    [],
  );

  const setVolumeBoth = useCallback(
    (vol: number) => {
      const v = videoRef.current;
      const clamped = Math.min(1, Math.max(0, vol));
      setVolume(clamped);
      if (v) v.volume = clamped;
      persistPrefs({ volume: clamped });
    },
    [persistPrefs],
  );

  const setMutedBoth = useCallback(
    (m: boolean) => {
      const v = videoRef.current;
      setMuted(m);
      if (v) v.muted = m;
      persistPrefs({ muted: m });
    },
    [persistPrefs],
  );

  const toggleFullscreen = useCallback(async () => {
    // Fullscreen the VIDEO, not just the window: take the native window
    // fullscreen and stretch the video over the whole viewport. The DOM
    // fullscreen API is unreliable inside webviews, so CSS does the work.
    if (fsVideo) {
      setFsVideo(false);
      await setFullscreen(false);
      return;
    }
    setFsVideo(true);
    if (!(await setFullscreen(true))) {
      try {
        await document.documentElement.requestFullscreen();
      } catch {
        setNotice('Fullscreen is not available in this window.');
        setTimeout(() => setNotice(''), 3000);
        setFsVideo(false);
      }
    }
  }, [fsVideo]);

  const togglePip = useCallback(async () => {
    const v = videoRef.current;
    if (!v || !document.pictureInPictureEnabled) {
      setNotice('Picture-in-picture is not supported by this window.');
      setTimeout(() => setNotice(''), 3000);
      return;
    }
    try {
      if (document.pictureInPictureElement) await document.exitPictureInPicture();
      else await v.requestPictureInPicture();
    } catch {
      setNotice('Picture-in-picture failed to start.');
      setTimeout(() => setNotice(''), 3000);
    }
  }, []);

  const toggle = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) void v.play().catch(() => {});
    else v.pause();
  }, []);

  // Keyboard shortcuts (matches web: space/k play, j/l seek, f fullscreen, m mute, esc back).
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.target as HTMLElement)?.tagName === 'INPUT') return;
      const v = videoRef.current;
      if (!v) return;
      if (e.key === ' ' || e.key === 'k') {
        e.preventDefault();
        toggle();
      } else if (e.key === 'ArrowRight' || e.key === 'l') v.currentTime += 10;
      else if (e.key === 'ArrowLeft' || e.key === 'j') v.currentTime -= 10;
      else if (e.key === 'f') {
        e.preventDefault();
        toggleFullscreen();
      } else if (e.key === 'm') setMutedBoth(!muted);
      else if (e.key === 'Escape') {
        if (fsVideo) void toggleFullscreen();
        else navigate(-1);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [toggle, toggleFullscreen, muted, setMutedBoth, navigate, fsVideo]);

  const openExternalSub = (file: File | null) => {
    if (extSubUrl) URL.revokeObjectURL(extSubUrl);
    if (!file) {
      setExtSubUrl(null);
      return;
    }
    setExtSubUrl(URL.createObjectURL(file));
    setActiveSub('external');
  };

  const mpvTitle = item
    ? item.show_title
      ? `${item.show_title} S${item.season}E${item.episode}`
      : item.title
    : '';
  const mpvShaders = (itus: MediaItem | null, p: PlaybackPrefs | null): string[] => {
    if (!itus || !p) return [];
    return p.upscaleAnime || isAnime(itus) ? anime4kShaders(p.shaderDir, p.anime4kLevel) : [];
  };

  const playInMpv = async () => {
    if (!item || !prefs || mpvId) return;
    setMpvMsg('');
    try {
      const shaders = mpvShaders(item, prefs);
      const id = await mpvStartSession({
        mpvPath: prefs.mpvPath || 'mpv',
        url: api.streamUrl(serverUrl, item.id),
        title: mpvTitle,
        shaders,
      });
      setMpvId(id);
      setMpvMsg(shaders.length > 0 ? 'Playing in mpv, upscaled.' : 'Playing in mpv.');
    } catch (e) {
      setMpvMsg(e instanceof Error ? e.message : 'Could not launch mpv. Set its path in Settings, Playback.');
    }
  };

  const stopMpv = useCallback(
    async (saveFirst: boolean) => {
      if (!mpvId) return;
      const sid = mpvId;
      setMpvId(null);
      setMpvState(null);
      try {
        if (saveFirst) {
          const st = await mpvStatusPoll(sid).catch(() => null);
          if (st?.time_pos != null) save(st.time_pos, st.duration ?? undefined, false);
        }
        await mpvStopSession(sid);
      } catch {
        // mpv already gone; progress (if any) was saved above.
      }
    },
    [mpvId, save],
  );

  // While mpv plays: poll progress, sync it home, handle end-of-file.
  useEffect(() => {
    if (!mpvId || !item) return;
    let cancelled = false;
    let misses = 0;
    const tick = async () => {
      try {
        const st = await mpvStatusPoll(mpvId);
        misses = 0;
        if (cancelled) return;
        setMpvState(st);
        if (st.time_pos != null) save(st.time_pos, st.duration ?? undefined, !st.paused);
        void updatePresence({
          state: st.paused ? 'Paused (mpv)' : item.show_title ? `S${item.season} E${item.episode} (mpv)` : 'Watching (mpv)',
          details: item.show_title ?? item.title,
          large_image: coverRef.current ?? undefined,
          large_text: item.show_title ?? item.title,
          start_timestamp: Math.floor(Date.now() / 1000),
        });
        if (st.eof) {
          const sid = mpvId;
          setMpvId(null);
          setMpvState(null);
          await mpvStopSession(sid).catch(() => {});
          if (cancelled) return;
          const eps = episodesRef.current;
          const i = eps.findIndex(e => e.id === item.id);
          const nxt = i >= 0 && i < eps.length - 1 ? eps[i + 1] : null;
          if (nxt && autoNextRef.current) {
            cacheItems([nxt]);
            navigate(`/play/${nxt.id}`, { state: nxt });
          }
        }
      } catch {
        // mpv quit on its own (or crashed): wrap up the session quietly.
        misses += 1;
        if (misses >= 2 && !cancelled) {
          setMpvId(null);
          setMpvState(null);
        }
      }
    };
    const timer = setInterval(() => void tick(), 5000);
    void tick();
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mpvId]);

  // Leaving the screen stops the external player (after saving progress).
  useEffect(() => {
    return () => {
      const sid = mpvIdRef.current;
      if (sid) {
        void (async () => {
          try {
            const st = await mpvStatusPoll(sid).catch(() => null);
            if (st?.time_pos != null && itemRef.current) {
              const it = itemRef.current;
              void api
                .savePlayback(serverUrl, {
                  item_id: it.id,
                  timestamp: st.time_pos,
                  duration: st.duration ?? undefined,
                  user_id: session.user_id,
                  is_playing: false,
                })
                .catch(() => {});
            }
            await mpvStopSession(sid);
          } catch {
            // Already gone.
          }
        })();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const displayTime = time + (transcodeRef.current ? offsetRef.current : 0);
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
      {isTranscode && !error && (
        <div className="sl-card" style={{ marginBottom: 'var(--sl-space-md)' }}>
          This file needs live transcoding, which is slow on weak servers. For instant
          playback with seeking, use <strong>Open in mpv</strong> below (zero server load).
        </div>
      )}
      {missingGst.length > 0 && (
        <div className="sl-card" style={{ borderColor: '#b3261e', marginBottom: 'var(--sl-space-md)' }}>
          <strong>Video can't play yet — system codecs are missing:</strong>
          <ul style={{ margin: 'var(--sl-space-sm) 0' }}>
            {missingGst.map(m => (
              <li key={m}>{m}</li>
            ))}
          </ul>
          <p style={{ fontSize: 'var(--sl-text-sm)' }}>
            Arch/CachyOS: <code>sudo pacman -S gst-plugins-good gst-plugins-base gst-libav</code>
            <br />
            Ubuntu/Debian: <code>sudo apt install gstreamer1.0-plugins-good gstreamer1.0-plugins-base gstreamer1.0-libav</code>
            <br />
            Then restart the app.
          </p>
        </div>
      )}
      {notice && <p className="sl-tag">{notice}</p>}
      {src && (
        <div
          style={
            fsVideo
              ? {
                  position: 'fixed', inset: 0, zIndex: 60, background: '#000',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }
              : { position: 'relative' }
          }
        >
        {preparing && (
          <div
            className="sl-skeleton"
            style={{
              position: 'absolute', inset: 0, display: 'flex', alignItems: 'center',
              justifyContent: 'center', zIndex: 1, minHeight: 240,
            }}
          >
            <span className="sl-tag">Preparing video…</span>
          </div>
        )}
        <video
          ref={videoRef}
          src={src}
          controls
          autoPlay
          style={
            fsVideo
              ? { width: '100vw', height: '100vh', objectFit: 'contain', background: '#000' }
              : { width: '100%', borderRadius: 'var(--sl-radius-md)', border: '2px solid var(--sl-text)', background: '#000' }
          }
          onClick={toggle}
          onDoubleClick={() => void toggleFullscreen()}
          title={fsVideo ? 'Double-click or Esc to exit fullscreen' : undefined}
          onLoadedMetadata={e => {
            setDuration(e.currentTarget.duration);
            if (resumeRef.current > 0) {
              e.currentTarget.currentTime = resumeRef.current;
              resumeRef.current = 0;
            }
          }}
          onCanPlay={() => setPreparing(false)}
          onError={() => {
            setPreparing(false);
            setError('This file would not play here. Try "Open in mpv" below.');
          }}
          onEnded={() => {
            if (autoNext && next) goEp(next);
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
        </div>
      )}

      <div className="sl-card" style={{ marginTop: 'var(--sl-space-md)' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-sm)', alignItems: 'center' }}>
          <button className="sl-button" onClick={toggle}>
            {isPlaying ? 'Pause' : 'Play'}
          </button>
          <span className="sl-tag">
            {fmtTime(displayTime)} / {Number.isFinite(duration) && duration > 0 ? fmtTime(duration) : 'live'}
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
          <button className="sl-button" onClick={() => void togglePip()}>
            PiP
          </button>
          <button className="sl-button" onClick={() => void toggleFullscreen()}>
            Fullscreen
          </button>
          <button className="sl-button" onClick={() => setMutedBoth(!muted)}>
            {muted ? 'Unmute' : 'Mute'}
          </button>
          <input
            type="range"
            min={0}
            max={1}
            step={0.05}
            value={volume}
            onChange={e => setVolumeBoth(Number(e.target.value))}
            style={{ width: 100 }}
            title="Volume"
          />
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
          {episodes.length > 0 && (
            <button className="sl-button" onClick={() => setShowEpisodes(v => !v)}>
              {showEpisodes ? 'Hide episodes' : `Episodes (${episodes.length})`}
            </button>
          )}
          {next && (
            <label style={{ display: 'flex', gap: 'var(--sl-space-xs)', alignItems: 'center', fontSize: 'var(--sl-text-sm)' }}>
              <input type="checkbox" checked={autoNext} onChange={e => setAutoNext(e.target.checked)} />
              Autoplay next
            </label>
          )}
          {mpvId ? (
            <div
              className="sl-card"
              style={{ padding: 'var(--sl-space-sm) var(--sl-space-md)', flexBasis: '100%' }}
            >
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-sm)', alignItems: 'center' }}>
                <span className="sl-tag">mpv</span>
                <button
                  className="sl-button"
                  onClick={() => mpvId && void mpvCmd(mpvId, ['cycle', 'pause']).catch(() => {})}
                >
                  {mpvState?.paused ? 'Resume' : 'Pause'}
                </button>
                <button
                  className="sl-button"
                  onClick={() => mpvId && void mpvCmd(mpvId, ['seek', -10]).catch(() => {})}
                >
                  -10s
                </button>
                <button
                  className="sl-button"
                  onClick={() => mpvId && void mpvCmd(mpvId, ['seek', 10]).catch(() => {})}
                >
                  +10s
                </button>
                <button
                  className="sl-button"
                  onClick={() => mpvId && void mpvCmd(mpvId, ['cycle', 'sub']).catch(() => {})}
                >
                  Sub track
                </button>
                <button
                  className="sl-button"
                  onClick={() => mpvId && void mpvCmd(mpvId, ['cycle', 'audio']).catch(() => {})}
                >
                  Audio track
                </button>
                <button className="sl-button" onClick={() => void stopMpv(true)}>
                  Stop
                </button>
                <span style={{ fontSize: 'var(--sl-text-sm)', color: 'var(--sl-text-link)' }}>
                  {mpvState?.time_pos != null
                    ? `${fmtTime(mpvState.time_pos)} / ${mpvState.duration ? fmtTime(mpvState.duration) : 'live'}`
                    : 'connecting…'}
                </span>
              </div>
            </div>
          ) : mpvOk ? (
            <button className="sl-button" onClick={() => void playInMpv()}>
              Open in mpv{isAnime(item) && prefs?.anime4kLevel ? ` (Anime4K ${prefs.anime4kLevel})` : ''}
            </button>
          ) : (
            <span style={{ fontSize: 'var(--sl-text-sm)', color: 'var(--sl-text-link)' }}>
              mpv not found — set it up in Settings, Playback for instant heavy files.
            </span>
          )}
        </div>
        {mpvMsg && <p style={{ fontSize: 'var(--sl-text-sm)' }}>{mpvMsg}</p>}
      </div>

      {showEpisodes && episodes.length > 0 && (
        <div className="sl-card" style={{ marginTop: 'var(--sl-space-md)' }}>
          <h3 style={{ marginTop: 0 }}>Episodes</h3>
          <div style={{ display: 'grid', gap: 'var(--sl-space-sm)', maxHeight: 320, overflowY: 'auto' }}>
            {episodes.map(ep => (
              <button
                key={ep.id}
                className="sl-button"
                style={{
                  textAlign: 'left',
                  opacity: ep.id === item.id ? 1 : 0.75,
                }}
                onClick={() => goEp(ep)}
              >
                E{ep.episode}: {ep.title}
                {ep.id === item.id ? ' (playing)' : ''}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
