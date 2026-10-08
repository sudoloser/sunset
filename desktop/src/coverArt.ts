import { assetUrl } from './api/client';

const IMGUR_API = 'https://api.imgur.com/3/image';
const CACHE_PREFIX = 'sunset_cover_';

function getCachedUrl(itemId: string): string | null {
  try {
    return localStorage.getItem(`${CACHE_PREFIX}${itemId}`);
  } catch {
    return null;
  }
}

function setCachedUrl(itemId: string, url: string) {
  try {
    localStorage.setItem(`${CACHE_PREFIX}${itemId}`, url);
  } catch {
    // Cache is best-effort (private mode etc.).
  }
}

async function uploadToImgur(serverUrl: string, itemId: string, clientId: string): Promise<string> {
  const resp = await fetch(assetUrl(serverUrl, itemId, 'folder.jpg'));
  if (!resp.ok) throw new Error('Failed to fetch poster');
  const blob = await resp.blob();
  const base64: string = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve((reader.result as string).split(',')[1]);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
  const up = await fetch(IMGUR_API, {
    method: 'POST',
    headers: {
      Authorization: `Client-ID ${clientId}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ image: base64, type: 'base64' }),
  });
  const data = await up.json();
  if (!data.success) throw new Error(data.data?.error || 'Imgur upload failed');
  return data.data.link as string;
}

/** Hosted cover URL for Discord art, or null when unconfigured/failing.
 * Posters upload to Imgur once per item, then are cached locally. */
export async function getCoverUrl(
  serverUrl: string,
  itemId: string,
  imgurClientId: string,
): Promise<string | null> {
  if (!imgurClientId) return null;
  const cached = getCachedUrl(itemId);
  if (cached) return cached;
  try {
    const url = await uploadToImgur(serverUrl, itemId, imgurClientId);
    setCachedUrl(itemId, url);
    return url;
  } catch {
    return null;
  }
}

export function clearCoverCache() {
  try {
    const keys: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (k?.startsWith(CACHE_PREFIX)) keys.push(k);
    }
    keys.forEach(k => localStorage.removeItem(k));
  } catch {
    // Nothing cached or storage unavailable.
  }
}
