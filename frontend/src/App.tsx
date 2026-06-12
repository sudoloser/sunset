import { useState, useEffect } from 'react';
import { api } from './api/client';
import { Navigation } from './components/layout/Navigation';
import { OnboardingWizard } from './features/onboarding/OnboardingWizard';
import { LoginForm } from './features/auth/LoginForm';
import { Dashboard } from './features/dashboard/Dashboard';
import { Settings } from './features/settings/Settings';
import { VideoPlayer } from './features/player/VideoPlayer';
import { LibrariesTab } from './features/library/LibrariesTab';
import { MediaDetails } from './features/library/MediaDetails';
import { SearchOverlay } from './features/search/SearchOverlay';
import { CollectionView } from './features/library/CollectionView';
import type { SetupStatus, MediaItem } from './types';

type AppStep = 'loading' | 'onboarding' | 'login' | 'dashboard' | 'admin' | 'player' | 'libraries' | 'settings' | 'collection';

function App() {
  // Apply saved theme on load
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

  const [step, setStep] = useState<AppStep>('loading');
  const [status, setStatus] = useState<SetupStatus | null>(null);
  const [activeTab, setActiveTab] = useState('home');
  const [selectedItem, setSelectedItem] = useState<MediaItem | null>(null);
  const [selectedCollection, setSelectedCollection] = useState<{ name: string; items: MediaItem[] } | null>(null);
  const [playingMedia, setPlayingMedia] = useState<MediaItem | null>(null);
  const [previousStep, setPreviousStep] = useState<AppStep>('dashboard');
  const [isAdmin, setIsAdmin] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const [userId, setUserId] = useState<string | undefined>(localStorage.getItem('sunset_user_id') || undefined);

  useEffect(() => {
    checkStatus();
  }, []);

  const checkStatus = async () => {
    try {
      const data = await api.getStatus();
      setStatus(data);
      if (!data.setup_complete) {
        setStep('onboarding');
      } else {
        // Check for persisted login
        const savedUserId = localStorage.getItem('sunset_user_id');
        if (savedUserId) {
          try {
            const profile = await api.getUserProfile(savedUserId);
            if (profile) {
              setUserId(savedUserId);
              setIsAdmin(profile.is_admin);
              localStorage.setItem('sunset_is_admin', profile.is_admin ? 'true' : 'false');
              localStorage.setItem('sunset_username', profile.username);
              setStep('dashboard');
            } else {
              setStep('login');
            }
          } catch (err) {
            setStep('login');
          }
        } else {
          setStep('login');
        }
      }
    } catch (err) {
      setStep('onboarding');
    }
  };

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    if (tab === 'home') setStep('dashboard');
    else if (tab === 'libraries') setStep('libraries');
    else if (tab === 'settings') setStep('settings');
  };

  if (step === 'loading') return <div style={{ color: 'white', padding: '2rem' }}>Loading...</div>;
  if (step === 'onboarding') return <OnboardingWizard onComplete={checkStatus} />;
  if (step === 'login') return <LoginForm serverName={status?.server_name || 'SunSet'} onLogin={(uid, admin, username) => { setUserId(uid); setIsAdmin(admin); localStorage.setItem('sunset_username', username); setStep('dashboard'); }} />;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navigation activeTab={activeTab} onTabChange={handleTabChange} />
      
      <main style={{ 
        flex: 1, 
        paddingTop: step === 'dashboard' ? 'var(--safe-area-top)' : 'calc(var(--header-height) + var(--safe-area-top))',
        paddingBottom: 'calc(var(--bottom-nav-height) + var(--safe-area-bottom))',
        maxWidth: '100vw',
        overflowX: 'hidden'
      }}>
        <div style={{ padding: step === 'dashboard' ? 0 : 'var(--spacing-xl)' }}>
          {step === 'dashboard' && (
            <Dashboard 
              onSelectItem={item => {
                if ((item as any).is_collection) {
                  setSelectedCollection({ name: item.title, items: (item as any).items });
                  setPreviousStep(step);
                  setStep('collection');
                } else {
                  setSelectedItem(item);
                }
              }}
              onPlayItem={item => { setPlayingMedia(item); setPreviousStep(step); setStep('player'); }}
              onSearch={() => setShowSearch(true)}
            />
          )}

          {step === 'collection' && selectedCollection && (
            <CollectionView 
              name={selectedCollection.name}
              items={selectedCollection.items}
              onSelectItem={(item: MediaItem) => setSelectedItem(item)}
              onBack={() => setStep('dashboard')}
            />
          )}

          {step === 'libraries' && (
            <LibrariesTab 
              isAdmin={isAdmin}
              onSelectItem={item => setSelectedItem(item)}
              onGoToSettings={() => handleTabChange('settings')}
              userId={userId}
            />
          )}

          {step === 'settings' && <Settings isAdmin={isAdmin} />}
        </div>

        {selectedItem && (
          <MediaDetails 
            item={selectedItem} 
            onClose={() => setSelectedItem(null)} 
            onPlay={item => { setPlayingMedia(item); setPreviousStep(step); setStep('player'); setSelectedItem(null); }} 
            userId={userId}
          />
        )}

        {step === 'player' && playingMedia && (
          <VideoPlayer 
            item={playingMedia} 
            onClose={() => setStep(previousStep)} 
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

export default App;
