export interface Presence {
  state: string;
  details: string;
  large_image?: string;
  large_text?: string;
  start_timestamp?: number;
}

async function invoke<T>(cmd: string, args?: Record<string, unknown>): Promise<T> {
  const { invoke } = await import('@tauri-apps/api/core');
  return invoke<T>(cmd, args);
}

export function isDesktop(): boolean {
  return (
    typeof window !== 'undefined' &&
    (typeof (window as unknown as { __TAURI_INTERNALS__?: unknown }).__TAURI_INTERNALS__ !== 'undefined' ||
      typeof (window as unknown as { __TAURI__?: unknown }).__TAURI__ !== 'undefined')
  );
}

export async function appVersion(): Promise<string> {
  if (!isDesktop()) return 'web';
  try {
    return await invoke<string>('app_version');
  } catch {
    return 'unknown';
  }
}

export async function startRpc(clientId: string): Promise<void> {
  if (!isDesktop()) return;
  await invoke('start_discord_rpc', { clientId });
}

export async function stopRpc(): Promise<void> {
  if (!isDesktop()) return;
  await invoke('stop_discord_rpc');
}

export async function updatePresence(p: Presence): Promise<void> {
  if (!isDesktop()) return;
  try {
    await invoke('update_discord_presence', {
      presence: {
        state: p.state,
        details: p.details,
        large_image: p.large_image ?? null,
        large_text: p.large_text ?? null,
        small_image: null,
        small_text: null,
        start_timestamp: p.start_timestamp ?? null,
      },
    });
  } catch {
    // RPC not connected; presence is best-effort.
  }
}

export async function mpvAvailable(mpvPath: string): Promise<boolean> {
  if (!isDesktop()) return false;
  try {
    return await invoke<boolean>('mpv_available', { mpvPath });
  } catch {
    return false;
  }
}

export async function mpvPlay(args: {
  mpvPath: string;
  url: string;
  title: string;
  subPath?: string;
  shaders?: string[];
}): Promise<void> {
  await invoke('mpv_play', {
    mpvPath: args.mpvPath,
    url: args.url,
    title: args.title,
    subPath: args.subPath ?? null,
    shaders: args.shaders ?? [],
  });
}
