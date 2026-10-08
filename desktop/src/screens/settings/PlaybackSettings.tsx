import { useEffect, useState } from 'react';
import { loadPref, savePref, type PlaybackPrefs } from '../../prefs';
import { mpvAvailable } from '../../discord';

export function PlaybackSettings() {
  const [prefs, setPrefs] = useState<PlaybackPrefs | null>(null);
  const [mpvOk, setMpvOk] = useState<boolean | null>(null);

  useEffect(() => {
    void loadPref('sunset_prefs_playback').then(setPrefs);
  }, []);

  useEffect(() => {
    if (!prefs) return;
    setMpvOk(null);
    const t = setTimeout(() => {
      void mpvAvailable(prefs.mpvPath).then(setMpvOk);
    }, 400);
    return () => clearTimeout(t);
  }, [prefs?.mpvPath]);

  if (!prefs) return <p>loading...</p>;

  const update = (patch: Partial<PlaybackPrefs>) => {
    const next = { ...prefs, ...patch };
    setPrefs(next);
    void savePref('sunset_prefs_playback', next);
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Playback</h3>
      <label style={{ fontWeight: 700 }}>Stream mode</label>
      <select
        className="sl-input"
        value={prefs.stream}
        onChange={e => update({ stream: e.target.value as PlaybackPrefs['stream'] })}
      >
        <option value="auto">Auto (direct when playable, transcode otherwise)</option>
        <option value="direct">Always direct</option>
        <option value="transcode">Always transcode</option>
      </select>

      <h3 style={{ fontSize: 'var(--sl-text-lg)', marginTop: 'var(--sl-space-lg)' }}>
        Anime upscaling <span className="sl-tag">mpv</span>
      </h3>
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
        Plays through mpv with Anime4K shaders. Needs mpv installed on this machine.
        {mpvOk == null ? '' : mpvOk ? ' mpv was found.' : ' mpv was NOT found.'}
      </p>
      <label style={{ fontWeight: 700 }}>mpv binary</label>
      <input
        className="sl-input"
        value={prefs.mpvPath}
        onChange={e => update({ mpvPath: e.target.value })}
        placeholder="mpv"
      />
      <label style={{ display: 'flex', gap: 'var(--sl-space-sm)', alignItems: 'center', marginTop: 'var(--sl-space-sm)' }}>
        <input
          type="checkbox"
          checked={prefs.upscaleAnime}
          onChange={e => update({ upscaleAnime: e.target.checked })}
        />
        Prefer upscaled mpv playback for anime
      </label>
      <label style={{ fontWeight: 700, display: 'block', marginTop: 'var(--sl-space-sm)' }}>
        Anime4K level
      </label>
      <select
        className="sl-input"
        value={prefs.anime4kLevel}
        onChange={e => update({ anime4kLevel: e.target.value as PlaybackPrefs['anime4kLevel'] })}
      >
        <option value="">Off (no shaders)</option>
        <option value="A">A — fast</option>
        <option value="B">B — balanced</option>
        <option value="C">C — quality</option>
      </select>
      <label style={{ fontWeight: 700, display: 'block', marginTop: 'var(--sl-space-sm)' }}>
        Shader folder
      </label>
      <input
        className="sl-input"
        value={prefs.shaderDir}
        onChange={e => update({ shaderDir: e.target.value })}
        placeholder="/path/to/Anime4K shaders"
      />
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-xs)' }}>
        Expected files: Anime4K_Clamp_Highlights.glsl, Restore and Upscale CNN pairs for the chosen level.
      </p>
    </div>
  );
}
