import { describe, it, expect, beforeEach } from 'vitest';
import { setImgurClientId, getImgurClientId, clearCoverCache } from '../coverArt';

describe('coverArt', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('stores and retrieves Imgur client ID', () => {
    setImgurClientId('test-client-id');
    expect(getImgurClientId()).toBe('test-client-id');
  });

  it('returns null when no client ID set', () => {
    expect(getImgurClientId()).toBeNull();
  });

  it('clears cover art cache', () => {
    localStorage.setItem('sunset_cover_item1', 'https://i.imgur.com/test.jpg');
    localStorage.setItem('sunset_cover_item2', 'https://i.imgur.com/test2.jpg');
    localStorage.setItem('other_key', 'keep');
    clearCoverCache();
    expect(localStorage.getItem('sunset_cover_item1')).toBeNull();
    expect(localStorage.getItem('sunset_cover_item2')).toBeNull();
    expect(localStorage.getItem('other_key')).toBe('keep');
  });
});
