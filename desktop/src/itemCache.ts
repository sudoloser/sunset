import type { MediaItem } from './api/client';

const KEY = 'sunset_item_cache';

function readAll(): Record<string, MediaItem> {
  try {
    return JSON.parse(sessionStorage.getItem(KEY) ?? '{}') as Record<string, MediaItem>;
  } catch {
    return {};
  }
}

/** Remember items from list responses so detail screens survive refresh. */
export function cacheItems(items: MediaItem[]): void {
  try {
    const all = readAll();
    for (const item of items) all[item.id] = item;
    sessionStorage.setItem(KEY, JSON.stringify(all));
  } catch {
    // Cache is best-effort; browsing works without it.
  }
}

export function getCachedItem(id: string): MediaItem | null {
  return readAll()[id] ?? null;
}
