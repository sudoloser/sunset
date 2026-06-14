import type {
  MediaItem, Library, OnboardRequest, LoginRequest,
  User, StorageInfo, PlaybackState
} from '../types';

function getDefaultBaseUrl(): string {
  return import.meta.env.DEV
    ? 'http://localhost:7867/api'
    : '/api';
}

function loadBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const custom = localStorage.getItem('sunset_server_url');
    if (custom) return `${custom.replace(/\/+$/, '')}/api`;
  }
  return getDefaultBaseUrl();
}

let baseUrl = loadBaseUrl();

export function setServerUrl(url: string) {
  localStorage.setItem('sunset_server_url', url);
  baseUrl = `${url.replace(/\/+$/, '')}/api`;
}

export function getCurrentServerUrl(): string {
  const stored = localStorage.getItem('sunset_server_url');
  return stored || getDefaultBaseUrl().replace(/\/api$/, '');
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const url = `${baseUrl}${path}`;
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    throw new Error(`API Error: ${response.statusText}`);
  }

  return response.json();
}

export const api = {
  getStatus: () => request<any>('/status'),
  getUptime: () => request<number>('/uptime'),
  onboard: (data: OnboardRequest) => request<boolean>('/onboard', { method: 'POST', body: JSON.stringify(data) }),
  login: (data: LoginRequest) => request<User | null>('/login', { method: 'POST', body: JSON.stringify(data) }),
  getUserProfile: (id: string) => request<User | null>(`/users/${id}`),
  getRecentlyAdded: (userId?: string) => request<MediaItem[]>(`/recently-added${userId ? `?user_id=${userId}` : ''}`),
  getLibraries: () => request<Library[]>('/libraries'),
  addLibrary: (data: Omit<Library, 'id'>) => request<boolean>('/libraries', { method: 'POST', body: JSON.stringify(data) }),
  updateLibrary: (id: string, data: Partial<Library>) => request<boolean>(`/libraries/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteLibrary: (id: string) => request<boolean>(`/libraries/${id}`, { method: 'DELETE' }),
  getLibraryItems: (id: string, userId?: string) => request<MediaItem[]>(`/libraries/${id}/items${userId ? `?user_id=${userId}` : ''}`),
  getShowEpisodes: (showTitle: string, userId?: string) => request<MediaItem[]>(`/shows/${encodeURIComponent(showTitle)}/episodes${userId ? `?user_id=${userId}` : ''}`),
  search: (query: string, userId?: string) => request<MediaItem[]>(`/search?q=${encodeURIComponent(query)}${userId ? `&user_id=${userId}` : ''}`),
  triggerScan: () => request<boolean>('/scan', { method: 'POST' }),
  getStreamUrl: (id: string) => `${baseUrl}/stream/${id}`,
  getSubtitles: (id: string) => request<string[]>(`/media/${id}/subtitles`),
  getSubtitleUrl: (id: string, name: string) => `${baseUrl}/media/${id}/subtitle/${encodeURIComponent(name)}`,
  savePlayback: (data: any) => request<boolean>('/playback', { method: 'POST', body: JSON.stringify(data) }),
  getPlayback: (itemId: string) => request<PlaybackState>(`/playback/${itemId}`),
  updateDiscordConfig: (id: string, token: string, status: string) => request<boolean>(`/users/${id}/discord-config`, { method: 'PUT', body: JSON.stringify({ token, status }) }),
  stopDiscordRpc: (id: string) => request<boolean>(`/users/${id}/discord-stop`, { method: 'POST' }),
  getStorage: () => request<StorageInfo>('/storage'),
  updateMedia: (id: string, data: any) => request<boolean>(`/media/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  refreshMedia: (id: string) => request<boolean>(`/media/${id}/refresh`, { method: 'POST' }),
  getGenres: () => request<string[]>('/genres'),
  getGenreItems: (genre: string, userId?: string) => request<MediaItem[]>(`/genre/${encodeURIComponent(genre)}${userId ? `?user_id=${userId}` : ''}`),
  createInvite: () => request<string>('/invite', { method: 'POST' }),
  redeemInvite: (code: string) => request<boolean>('/invite/redeem', { method: 'POST', body: JSON.stringify({ code }) }),
  generateMediaToken: (id: string) => request<string>(`/media/${id}/token`, { method: 'POST' }),
  getUsers: () => request<User[]>('/users'),
  createUser: (data: { username: string; password_hash: string; is_admin: boolean }) => request<boolean>('/users', { method: 'POST', body: JSON.stringify(data) }),
  deleteUser: (id: string) => request<boolean>(`/users/${id}`, { method: 'DELETE' }),
  changePassword: (id: string, current_password: string, new_password: string) => request<boolean>(`/users/${id}/password`, { method: 'PUT', body: JSON.stringify({ current_password, new_password }) }),
  changeUsername: (id: string, new_username: string) => request<boolean>(`/users/${id}/username`, { method: 'PUT', body: JSON.stringify({ new_username }) }),
  getProfilePictureUrl: (id: string) => `${baseUrl}/users/${id}/profile-picture`,
  uploadProfilePicture: (id: string, image: string) => request<boolean>(`/users/${id}/profile-picture`, { method: 'POST', body: JSON.stringify({ image }) }),
  getContinueWatching: (userId: string) => request<MediaItem[]>(`/continue-watching/${userId}`),
  getUserItems: (userId: string) => request<MediaItem[]>(`/user-items/${userId}`),
  addUserItem: (userId: string, itemId: string) => request<boolean>(`/user-items/${userId}`, { method: 'POST', body: JSON.stringify({ item_id: itemId }) }),
  removeUserItem: (userId: string, itemId: string) => request<boolean>(`/user-items/${userId}/${itemId}`, { method: 'DELETE' }),
};
