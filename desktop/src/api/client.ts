export type MediaType = 'movie' | 'episode';

export interface MediaItem {
  id: string;
  title: string;
  show_title?: string;
  collection_name?: string;
  media_type: MediaType;
  year?: number;
  season?: number;
  episode?: number;
  added_at?: string;
  file_path: string;
  description?: string;
  cast?: string;
  genres?: string;
  rating?: number;
  tmdb_id?: string;
  poster_path?: string;
  backdrop_path?: string;
  progress?: number;
  version_tag?: string;
}

export interface Library {
  id: string;
  name: string;
  path: string;
  lib_type: 'movies' | 'shows';
}

export interface User {
  user_id: string;
  username: string;
  is_admin: boolean;
}

export interface ServerStatus {
  setup_complete: boolean;
  server_name?: string | null;
}

export interface OnboardData {
  server_name: string;
  admin_user: { username: string; password_hash: string };
  libraries: { name: string; path: string; lib_type: 'movies' | 'shows' }[];
}

export function apiBase(serverUrl: string): string {
  return `${serverUrl.replace(/\/+$/, '')}/api`;
}

export function assetUrl(serverUrl: string, itemId: string, name: string): string {
  return `${apiBase(serverUrl)}/media/${itemId}/asset/${name}`;
}

async function request<T>(serverUrl: string, path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${apiBase(serverUrl)}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options?.headers },
  });
  if (!res.ok) throw new Error(`Server returned ${res.status}`);
  return res.json() as Promise<T>;
}

export const api = {
  getStatus: (serverUrl: string) => request<ServerStatus>(serverUrl, '/status'),
  onboard: (serverUrl: string, data: OnboardData) =>
    request<boolean>(serverUrl, '/onboard', { method: 'POST', body: JSON.stringify(data) }),
  login: (serverUrl: string, username: string, password_hash: string) =>
    request<User | null>(serverUrl, '/login', {
      method: 'POST',
      body: JSON.stringify({ username, password_hash }),
    }),
  getLibraries: (serverUrl: string) => request<Library[]>(serverUrl, '/libraries'),
  getLibraryItems: (serverUrl: string, id: string, userId?: string) =>
    request<MediaItem[]>(serverUrl, `/libraries/${id}/items${userId ? `?user_id=${userId}` : ''}`),
  getShowEpisodes: (serverUrl: string, showTitle: string, userId?: string) =>
    request<MediaItem[]>(
      serverUrl,
      `/shows/${encodeURIComponent(showTitle)}/episodes${userId ? `?user_id=${userId}` : ''}`,
    ),
  getRecentlyAdded: (serverUrl: string, userId?: string) =>
    request<MediaItem[]>(serverUrl, `/recently-added${userId ? `?user_id=${userId}` : ''}`),
  getContinueWatching: (serverUrl: string, userId: string) =>
    request<MediaItem[]>(serverUrl, `/continue-watching/${userId}`),
  search: (serverUrl: string, query: string, userId?: string) =>
    request<MediaItem[]>(
      serverUrl,
      `/search?q=${encodeURIComponent(query)}${userId ? `&user_id=${userId}` : ''}`,
    ),
  getGenres: (serverUrl: string) => request<string[]>(serverUrl, '/genres'),
  getGenreItems: (serverUrl: string, genre: string, userId?: string) =>
    request<MediaItem[]>(
      serverUrl,
      `/genre/${encodeURIComponent(genre)}${userId ? `?user_id=${userId}` : ''}`,
    ),
  triggerScan: (serverUrl: string) => request<boolean>(serverUrl, '/scan', { method: 'POST' }),
  getCodec: (serverUrl: string, id: string) =>
    request<{ video_codec: string | null; audio_codec: string | null }>(
      serverUrl,
      `/media/${id}/codec`,
    ),
  getSubtitles: (serverUrl: string, id: string) =>
    request<string[]>(serverUrl, `/media/${id}/subtitles`),
  subtitleUrl: (serverUrl: string, id: string, name: string) =>
    `${apiBase(serverUrl)}/media/${id}/subtitle/${encodeURIComponent(name)}`,
  streamUrl: (serverUrl: string, id: string) => `${apiBase(serverUrl)}/stream/${id}`,
  remuxUrl: (serverUrl: string, id: string) => `${apiBase(serverUrl)}/stream/${id}/remux`,
  transcodeUrl: (serverUrl: string, id: string, start = 0) =>
    `${apiBase(serverUrl)}/stream/${id}/transcode${start > 0 ? `?start=${Math.floor(start)}` : ''}`,
  getPlayback: (serverUrl: string, itemId: string, userId?: string) =>
    request<{ timestamp: number; duration?: number } | null>(
      serverUrl,
      `/playback/${itemId}${userId ? `?user_id=${userId}` : ''}`,
    ),
  savePlayback: (
    serverUrl: string,
    data: { item_id: string; timestamp: number; duration?: number; user_id?: string; is_playing?: boolean },
  ) => request<boolean>(serverUrl, '/playback', { method: 'POST', body: JSON.stringify(data) }),
  updateDiscordConfig: (serverUrl: string, userId: string, token: string, status: string, cover_url?: string) =>
    request<boolean>(serverUrl, `/users/${userId}/discord-config`, {
      method: 'PUT',
      body: JSON.stringify({ token, status, cover_url }),
    }),
  stopDiscordRpc: (serverUrl: string, userId: string) =>
    request<boolean>(serverUrl, `/users/${userId}/discord-stop`, { method: 'POST' }),
  getUserProfile: (serverUrl: string, userId: string) =>
    request<
      User & {
        discord_token?: string;
        discord_status?: string;
        discord_cover_url?: string;
        profile_picture?: string;
      }
    >(serverUrl, `/users/${userId}`),
  getUsers: (serverUrl: string) => request<User[]>(serverUrl, '/users'),
  createUser: (serverUrl: string, data: { username: string; password_hash: string; is_admin: boolean }) =>
    request<boolean>(serverUrl, '/users', { method: 'POST', body: JSON.stringify(data) }),
  deleteUser: (serverUrl: string, userId: string) =>
    request<boolean>(serverUrl, `/users/${userId}`, { method: 'DELETE' }),
  changePassword: (serverUrl: string, userId: string, current_password: string, new_password: string) =>
    request<boolean>(serverUrl, `/users/${userId}/password`, {
      method: 'PUT',
      body: JSON.stringify({ current_password, new_password }),
    }),
  changeUsername: (serverUrl: string, userId: string, new_username: string) =>
    request<boolean>(serverUrl, `/users/${userId}/username`, {
      method: 'PUT',
      body: JSON.stringify({ new_username }),
    }),
  uploadProfilePicture: (serverUrl: string, userId: string, image: string) =>
    request<boolean>(serverUrl, `/users/${userId}/profile-picture`, {
      method: 'POST',
      body: JSON.stringify({ image }),
    }),
  profilePictureUrl: (serverUrl: string, userId: string) =>
    `${apiBase(serverUrl)}/users/${userId}/profile-picture`,
  addLibrary: (serverUrl: string, data: { name: string; path: string; lib_type: 'movies' | 'shows' }) =>
    request<boolean>(serverUrl, '/libraries', { method: 'POST', body: JSON.stringify(data) }),
  updateLibrary: (
    serverUrl: string,
    id: string,
    data: { name: string; path: string; lib_type: 'movies' | 'shows' },
  ) => request<boolean>(serverUrl, `/libraries/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteLibrary: (serverUrl: string, id: string) =>
    request<boolean>(serverUrl, `/libraries/${id}`, { method: 'DELETE' }),
  createInvite: (serverUrl: string) => request<string>(serverUrl, '/invite', { method: 'POST' }),
  refreshMedia: (serverUrl: string, id: string) =>
    request<boolean>(serverUrl, `/media/${id}/refresh`, { method: 'POST' }),
  uploadMovie: (serverUrl: string, libraryId: string, form: FormData, onProgress?: (loaded: number, total: number) => void) =>
    new Promise<{ success: boolean; title: string; file_path: string }>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('POST', `${apiBase(serverUrl)}/libraries/${libraryId}/upload/movie`);
      xhr.upload.onprogress = e => {
        if (onProgress && e.lengthComputable) onProgress(e.loaded, e.total);
      };
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            resolve(JSON.parse(xhr.responseText));
          } catch {
            reject(new Error('Bad server response'));
          }
        } else {
          reject(new Error(xhr.responseText || `Upload failed (${xhr.status})`));
        }
      };
      xhr.onerror = () => reject(new Error('Upload failed: network error'));
      xhr.onabort = () => reject(new Error('Upload cancelled'));
      xhr.send(form);
    }),
};
