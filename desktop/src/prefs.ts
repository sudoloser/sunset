export interface SubtitlePrefs {
  color: string;
  size: number;
  font: string;
  bold: boolean;
  backgroundOpacity: number;
}

export type StreamPreference = 'auto' | 'direct' | 'transcode';

export interface PlaybackPrefs {
  stream: StreamPreference;
  speed: number;
  volume: number;
  muted: boolean;
  upscaleAnime: boolean;
  anime4kLevel: '' | 'A' | 'B' | 'C';
  shaderDir: string;
  mpvPath: string;
}

export interface AppearancePrefs {
  density: 'comfortable' | 'compact';
}

const DEFAULTS = {
  sunset_prefs_subtitles: {
    color: '#ffffff',
    size: 100,
    font: 'sans-serif',
    bold: false,
    backgroundOpacity: 0,
  } as SubtitlePrefs,
  sunset_prefs_playback: {
    stream: 'auto',
    speed: 1,
    volume: 1,
    muted: false,
    upscaleAnime: false,
    anime4kLevel: '',
    shaderDir: '',
    mpvPath: 'mpv',
  } as PlaybackPrefs,
  sunset_prefs_appearance: { density: 'comfortable' } as AppearancePrefs,
  sunset_prefs_discord: { clientId: '', autoConnect: false, imgurClientId: '' } as DiscordPrefs,
};

export interface DiscordPrefs {
  clientId: string;
  autoConnect: boolean;
  imgurClientId: string;
}

type PrefKey = keyof typeof DEFAULTS;

function isTauri(): boolean {
  return (
    typeof window !== 'undefined' &&
    (typeof (window as unknown as { __TAURI_INTERNALS__?: unknown }).__TAURI_INTERNALS__ !== 'undefined' ||
      typeof (window as unknown as { __TAURI__?: unknown }).__TAURI__ !== 'undefined')
  );
}

async function tauriStore(): Promise<{
  get: (key: string) => Promise<unknown>;
  set: (key: string, value: unknown) => Promise<void>;
  save: () => Promise<void>;
} | null> {
  if (!isTauri()) return null;
  try {
    const { load } = await import('@tauri-apps/plugin-store');
    return await load('sunset.json', { autoSave: false });
  } catch {
    return null;
  }
}

export async function loadPref<K extends PrefKey>(key: K): Promise<(typeof DEFAULTS)[K]> {
  const fallback = DEFAULTS[key];
  const store = await tauriStore();
  if (store) {
    const v = await store.get(key);
    if (v && typeof v === 'object') return { ...fallback, ...(v as object) };
    return fallback;
  }
  try {
    const raw = localStorage.getItem(key);
    if (raw) return { ...fallback, ...JSON.parse(raw) };
  } catch {
    // Corrupt prefs fall back to defaults.
  }
  return fallback;
}

export async function savePref<K extends PrefKey>(key: K, value: (typeof DEFAULTS)[K]): Promise<void> {
  const store = await tauriStore();
  if (store) {
    await store.set(key, value);
    await store.save();
    return;
  }
  localStorage.setItem(key, JSON.stringify(value));
}
