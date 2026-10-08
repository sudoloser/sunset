export interface Session {
  user_id: string;
  username: string;
  is_admin: boolean;
}

const PREFIX = 'sunset_session_';

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
  delete: (key: string) => Promise<boolean>;
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

export async function getSession(): Promise<Session | null> {
  const store = await tauriStore();
  if (store) {
    const user_id = await store.get(`${PREFIX}user_id`);
    if (typeof user_id !== 'string') return null;
    return {
      user_id,
      username: (await store.get(`${PREFIX}username`)) as string,
      is_admin: (await store.get(`${PREFIX}is_admin`)) === true,
    };
  }
  const user_id = localStorage.getItem(`${PREFIX}user_id`);
  if (!user_id) return null;
  return {
    user_id,
    username: localStorage.getItem(`${PREFIX}username`) || 'User',
    is_admin: localStorage.getItem(`${PREFIX}is_admin`) === 'true',
  };
}

export async function setSession(s: Session): Promise<void> {
  const store = await tauriStore();
  if (store) {
    await store.set(`${PREFIX}user_id`, s.user_id);
    await store.set(`${PREFIX}username`, s.username);
    await store.set(`${PREFIX}is_admin`, s.is_admin);
    await store.save();
    return;
  }
  localStorage.setItem(`${PREFIX}user_id`, s.user_id);
  localStorage.setItem(`${PREFIX}username`, s.username);
  localStorage.setItem(`${PREFIX}is_admin`, s.is_admin ? 'true' : 'false');
}

export async function clearSession(): Promise<void> {
  const store = await tauriStore();
  if (store) {
    await store.delete(`${PREFIX}user_id`);
    await store.delete(`${PREFIX}username`);
    await store.delete(`${PREFIX}is_admin`);
    await store.save();
    return;
  }
  localStorage.removeItem(`${PREFIX}user_id`);
  localStorage.removeItem(`${PREFIX}username`);
  localStorage.removeItem(`${PREFIX}is_admin`);
}
