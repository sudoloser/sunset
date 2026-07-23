import { useState, useEffect, useCallback } from 'react';
import { Outlet, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { Navigation } from '../components/layout/Navigation';
import { MediaDetails } from '../features/library/MediaDetails';
import { SearchOverlay } from '../features/search/SearchOverlay';
import { VideoPlayer } from '../features/player/VideoPlayer';
import { api } from '../api/client';
import type { MediaItem } from '@sunset/shared';

export function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const [userId, setUserId] = useState<string | undefined>(
    localStorage.getItem('sunset_user_id') || undefined
  );
  const [isAdmin, setIsAdmin] = useState(
    localStorage.getItem('sunset_is_admin') === 'true'
  );
  const [selectedItem, setSelectedItem] = useState<MediaItem | null>(null);
  const [playingMedia, setPlayingMedia] = useState<MediaItem | null>(null);
  const [showSearch, setShowSearch] = useState(false);

  const uid = localStorage.getItem('sunset_user_id');
  if (!uid) {
    return <Navigate to="/login" replace />;
  }

  useEffect(() => {
    api.getUserProfile(uid).then(profile => {
      if (!profile) {
        localStorage.clear();
        navigate('/login', { replace: true });
        return;
      }
      setUserId(uid);
      setIsAdmin(profile.is_admin);
      localStorage.setItem('sunset_is_admin', profile.is_admin ? 'true' : 'false');
      localStorage.setItem('sunset_username', profile.username);
    }).catch(() => {
      navigate('/login', { replace: true });
    });
  }, []);

  // Apply saved theme
  useEffect(() => {
    const saved = localStorage.getItem('sunset_theme');
    if (saved === 'light') {
      const root = document.documentElement;
      root.style.setProperty('--bg-color', '#f5f5f7');
      root.style.setProperty('--surface-color', '#ffffff');
      root.style.setProperty('--surface-variant', '#e8e8ed');
      root.style.setProperty('--text-primary', '#1d1d1f');
      root.style.setProperty('--text-secondary', '#6e6e73');
      root.style.setProperty('--border-color', '#d2d2d7');
    }
  }, []);

  const activeTab = location.pathname === '/' || location.pathname.startsWith('/home') ? 'home'
    : location.pathname.startsWith('/libraries') ? 'libraries'
    : location.pathname.startsWith('/settings') ? 'settings'
    : 'home';

  const handleTabChange = useCallback((tab: string) => {
    if (tab === 'home') navigate('/home');
    else if (tab === 'libraries') navigate('/libraries');
    else if (tab === 'settings') navigate('/settings');
  }, [navigate]);

  const handlePlayItem = useCallback((item: MediaItem) => {
    setPlayingMedia(item);
  }, []);

  const handleClosePlayer = useCallback(() => {
    setPlayingMedia(null);
  }, []);

  const handleSelectItem = useCallback((item: MediaItem) => {
    if ((item as any).is_collection) {
      navigate(`/collection/${encodeURIComponent(item.title)}`, {
        state: { items: (item as any).items },
      });
    } else {
      setSelectedItem(item);
    }
  }, [navigate]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navigation activeTab={activeTab} onTabChange={handleTabChange} />

      <main style={{
        flex: 1,
        paddingTop: 'calc(var(--header-height) + var(--safe-area-top))',
        paddingBottom: 'calc(var(--bottom-nav-height) + var(--safe-area-bottom))',
        maxWidth: '100vw',
        overflowX: 'hidden',
      }}>
        <div style={{ padding: activeTab === 'home' ? 0 : 'var(--spacing-xl)' }}>
          <Outlet context={{ onSelectItem: handleSelectItem, onPlayItem: handlePlayItem, onSearch: () => setShowSearch(true), userId, isAdmin }} />
        </div>

        {selectedItem && (
          <MediaDetails
            item={selectedItem}
            onClose={() => setSelectedItem(null)}
            onPlay={item => { setPlayingMedia(item); setSelectedItem(null); }}
            userId={userId}
          />
        )}

        {playingMedia && (
          <VideoPlayer
            item={playingMedia}
            onClose={handleClosePlayer}
            onSelectItem={item => setPlayingMedia(item)}
            userId={userId}
          />
        )}

        {showSearch && (
          <SearchOverlay
            onClose={() => setShowSearch(false)}
            onSelect={item => setSelectedItem(item)}
          />
        )}
      </main>
    </div>
  );
}
