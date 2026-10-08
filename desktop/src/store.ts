const KEY = 'sunset_server_url';

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

export async function getServerUrl(): Promise<string | null> {
  const store = await tauriStore();
  if (store) {
    const v = await store.get(KEY);
    return typeof v === 'string' ? v : null;
  }
  return localStorage.getItem(KEY);
}

export async function setServerUrl(url: string): Promise<void> {
  const store = await tauriStore();
  if (store) {
    await store.set(KEY, url);
    await store.save();
    return;
  }
  localStorage.setItem(KEY, url);
}
