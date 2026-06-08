export type MediaType = 'movie' | 'episode';

export interface MediaItem {
  id: string;
  title: string;
  show_title?: string;
  media_type: MediaType;
  year?: number;
  season?: number;
  episode?: number;
  added_at?: string;
  file_path: string;
  description?: string;
  cast?: string;
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
