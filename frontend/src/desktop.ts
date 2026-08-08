import { api } from './api/client';

export interface DesktopPresence {
  state: string;
  details: string;
  large_image?: string;
  large_text?: string;
  small_image?: string;
  small_text?: string;
  start_timestamp?: number;
}

type InvokeFn = (cmd: string, args?: Record<string, unknown>) => Promise<unknown>;

let invoke: InvokeFn | null = null;

try {
  const tauriInternals = (window as any).__TAURI_INTERNALS__;
  if (tauriInternals?.invoke) {
    invoke = tauriInternals.invoke.bind(tauriInternals);
  }
} catch {
  // not in Tauri
}

export const isDesktop = () =>
  typeof window !== 'undefined' &&
  (typeof (window as any).__TAURI_INTERNALS__ !== 'undefined' ||
    typeof (window as any).__TAURI__ !== 'undefined');

export const desktopApi = {
  startDiscordRpc: async (clientId: string): Promise<void> => {
    if (!invoke) throw new Error('Not in desktop environment');
    await invoke('start_discord_rpc', { clientId });
  },

  stopDiscordRpc: async (): Promise<void> => {
    if (!invoke) throw new Error('Not in desktop environment');
    await invoke('stop_discord_rpc');
  },

  updateDiscordPresence: async (presence: DesktopPresence): Promise<void> => {
    if (!invoke) throw new Error('Not in desktop environment');
    await invoke('update_discord_presence', { presence });
  },

  isDiscordRunning: async (): Promise<boolean> => {
    if (!invoke) return false;
    return (await invoke('is_discord_running')) as boolean;
  },
};

export async function connectDiscord(clientId: string): Promise<boolean> {
  if (isDesktop()) {
    try {
      await desktopApi.startDiscordRpc(clientId);
      return true;
    } catch {
      return false;
    }
  }

  // Web/Android — uses existing backend RPC pipeline (user token saved to backend)
  const userId = localStorage.getItem('sunset_user_id');
  if (!userId) return false;

  try {
    await api.updateDiscordConfig(userId, clientId, 'online');
    return true;
  } catch {
    return false;
  }
}

export async function disconnectDiscord(): Promise<boolean> {
  if (isDesktop()) {
    try {
      await desktopApi.stopDiscordRpc();
      return true;
    } catch {
      return false;
    }
  }

  const userId = localStorage.getItem('sunset_user_id');
  if (!userId) return false;

  try {
    await api.stopDiscordRpc(userId);
    return true;
  } catch {
    return false;
  }
}

export async function updatePresence(presence: DesktopPresence): Promise<void> {
  if (isDesktop()) {
    await desktopApi.updateDiscordPresence(presence);
  }
}
