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

/** Season-major, episode-minor order. Sorting by episode alone mixes
 * seasons together (S2E1 lands before S1E2) and breaks prev/next. */
export function sortEpisodes(items: MediaItem[]): MediaItem[] {
  return [...items].sort(
    (a, b) => (a.season ?? 1) - (b.season ?? 1) || (a.episode ?? 0) - (b.episode ?? 0),
  );
}
