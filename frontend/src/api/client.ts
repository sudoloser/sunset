import type { 
  MediaItem, Library, OnboardRequest, LoginRequest, 
  User, StorageInfo, PlaybackState 
} from '../types';

const BASE_URL = import.meta.env.DEV 
  ? 'http://localhost:7867/api' 
  : '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const url = `${BASE_URL}${path}`;
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
  getRecentlyAdded: () => request<MediaItem[]>('/recently-added'),
  getLibraries: () => request<Library[]>('/libraries'),
  addLibrary: (data: Omit<Library, 'id'>) => request<boolean>('/libraries', { method: 'POST', body: JSON.stringify(data) }),
  updateLibrary: (id: string, data: Partial<Library>) => request<boolean>(`/libraries/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteLibrary: (id: string) => request<boolean>(`/libraries/${id}`, { method: 'DELETE' }),
  getLibraryItems: (id: string) => request<MediaItem[]>(`/libraries/${id}/items`),
  getShowEpisodes: (showTitle: string) => request<MediaItem[]>(`/shows/${encodeURIComponent(showTitle)}/episodes`),
  search: (query: string) => request<MediaItem[]>(`/search?q=${encodeURIComponent(query)}`),
  triggerScan: () => request<boolean>('/scan', { method: 'POST' }),
  getStreamUrl: (id: string) => `${BASE_URL}/stream/${id}`,
  getSubtitles: (id: string) => request<string[]>(`/media/${id}/subtitles`),
  getSubtitleUrl: (id: string, name: string) => `${BASE_URL}/media/${id}/subtitle/${encodeURIComponent(name)}`,
  savePlayback: (data: any) => request<boolean>('/playback', { method: 'POST', body: JSON.stringify(data) }),
  getPlayback: (itemId: string) => request<PlaybackState>(`/playback/${itemId}`),
  updateDiscordConfig: (id: string, token: string, status: string) => request<boolean>(`/users/${id}/discord-config`, { method: 'PUT', body: JSON.stringify({ token, status }) }),
  getStorage: () => request<StorageInfo>('/storage'),
  updateMedia: (id: string, data: any) => request<boolean>(`/media/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  refreshMedia: (id: string) => request<boolean>(`/media/${id}/refresh`, { method: 'POST' }),
  getGenres: () => request<string[]>('/genres'),
  getGenreItems: (genre: string) => request<MediaItem[]>(`/genre/${encodeURIComponent(genre)}`),
  createInvite: () => request<string>('/invite', { method: 'POST' }),
  redeemInvite: (code: string) => request<boolean>('/invite/redeem', { method: 'POST', body: JSON.stringify({ code }) }),
  generateMediaToken: (id: string) => request<string>(`/media/${id}/token`, { method: 'POST' }),
  getUsers: () => request<User[]>('/users'),
  createUser: (data: { username: string; password_hash: string; is_admin: boolean }) => request<boolean>('/users', { method: 'POST', body: JSON.stringify(data) }),
  changePassword: (id: string, current_password: string, new_password: string) => request<boolean>(`/users/${id}/password`, { method: 'PUT', body: JSON.stringify({ current_password, new_password }) }),
  changeUsername: (id: string, new_username: string) => request<boolean>(`/users/${id}/username`, { method: 'PUT', body: JSON.stringify({ new_username }) }),
  getProfilePictureUrl: (id: string) => `${BASE_URL}/users/${id}/profile-picture`,
  uploadProfilePicture: (id: string, image: string) => request<boolean>(`/users/${id}/profile-picture`, { method: 'POST', body: JSON.stringify({ image }) }),
  getContinueWatching: (userId: string) => request<MediaItem[]>(`/continue-watching/${userId}`),
  getUserItems: (userId: string) => request<MediaItem[]>(`/user-items/${userId}`),
  addUserItem: (userId: string, itemId: string) => request<boolean>(`/user-items/${userId}`, { method: 'POST', body: JSON.stringify({ item_id: itemId }) }),
  removeUserItem: (userId: string, itemId: string) => request<boolean>(`/user-items/${userId}/${itemId}`, { method: 'DELETE' }),
};
