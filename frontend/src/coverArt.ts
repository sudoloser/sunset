const IMGUR_API = 'https://api.imgur.com/3/image';
const CACHE_PREFIX = 'sunset_cover_';

function getClientId(): string | null {
  return localStorage.getItem('sunset_imgur_client_id');
}

function getCachedUrl(itemId: string): string | null {
  return localStorage.getItem(`${CACHE_PREFIX}${itemId}`);
}

function setCachedUrl(itemId: string, url: string) {
  localStorage.setItem(`${CACHE_PREFIX}${itemId}`, url);
}

async function fetchPosterBlob(itemId: string): Promise<Blob> {
  const resp = await fetch(`/api/media/${itemId}/asset/folder.jpg`);
  if (!resp.ok) throw new Error('Failed to fetch poster');
  return resp.blob();
}

async function uploadToImgur(blob: Blob, clientId: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = async () => {
      const base64 = (reader.result as string).split(',')[1];
      try {
        const resp = await fetch(IMGUR_API, {
          method: 'POST',
          headers: {
            Authorization: `Client-ID ${clientId}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ image: base64, type: 'base64' }),
        });
        const data = await resp.json();
        if (data.success) resolve(data.data.link);
        else reject(new Error(data.data?.error || 'Imgur upload failed'));
      } catch (e) {
        reject(e);
      }
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

export async function getCoverUrl(itemId: string): Promise<string | null> {
  const clientId = getClientId();
  if (!clientId) return null;

  const cached = getCachedUrl(itemId);
  if (cached) return cached;

  try {
    const blob = await fetchPosterBlob(itemId);
    const url = await uploadToImgur(blob, clientId);
    setCachedUrl(itemId, url);
    return url;
  } catch {
    return null;
  }
}

export function setImgurClientId(clientId: string) {
  localStorage.setItem('sunset_imgur_client_id', clientId);
}

export function getImgurClientId(): string | null {
  return localStorage.getItem('sunset_imgur_client_id');
}

export function clearCoverCache() {
  const keys = Object.keys(localStorage).filter(k => k.startsWith(CACHE_PREFIX));
  keys.forEach(k => localStorage.removeItem(k));
}
