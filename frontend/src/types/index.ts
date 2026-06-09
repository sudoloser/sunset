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
}

export interface User {
  user_id: string;
  username: string;
  is_admin: boolean;
  discord_token?: string;
  discord_status?: string;
}

export interface PlaybackState {
  item_id: string;
  user_id: string;
  timestamp: number;
  duration?: number;
  updated_at?: string;
}

export interface StorageInfo {
  total_size: number;
  item_count: number;
  library_count: number;
  user_count: number;
}

export interface Library {
  id: string;
  name: string;
  path: string;
  lib_type: 'movies' | 'shows';
}

export interface SetupStatus {
  setup_complete: boolean;
  server_name?: string;
}

export interface UserConfig {
  username: string;
  password_hash: string;
}

export interface OnboardRequest {
  server_name: string;
  admin_user: UserConfig;
  libraries: Omit<Library, 'id'>[];
}

export interface LoginRequest {
  username: string;
  password_hash: string;
}
