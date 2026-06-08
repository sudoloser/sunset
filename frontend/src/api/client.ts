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
  onboard: (data: any) => request<boolean>('/onboard', { method: 'POST', body: JSON.stringify(data) }),
  login: (data: any) => request<{ user_id: string, username: string, is_admin: boolean } | null>('/login', { method: 'POST', body: JSON.stringify(data) }),
  getUserProfile: (id: string) => request<{ user_id: string, username: string, is_admin: boolean } | null>(`/users/${id}`),
  getRecentlyAdded: () => request<any[]>('/recently-added'),
  getLibraries: () => request<any[]>('/libraries'),
  addLibrary: (data: any) => request<boolean>('/libraries', { method: 'POST', body: JSON.stringify(data) }),
  updateLibrary: (id: string, data: any) => request<boolean>(`/libraries/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteLibrary: (id: string) => request<boolean>(`/libraries/${id}`, { method: 'DELETE' }),
  getLibraryItems: (id: string) => request<any[]>(`/libraries/${id}/items`),
  getShowEpisodes: (showTitle: string) => request<any[]>(`/shows/${encodeURIComponent(showTitle)}/episodes`),
  search: (query: string) => request<any[]>(`/search?q=${encodeURIComponent(query)}`),
  triggerScan: () => request<boolean>('/scan', { method: 'POST' }),
  getStreamUrl: (id: string) => `${BASE_URL}/stream/${id}`,
  getSubtitles: (id: string) => request<string[]>(`/media/${id}/subtitles`),
  getSubtitleUrl: (id: string, name: string) => `${BASE_URL}/media/${id}/subtitle/${encodeURIComponent(name)}`,
};
