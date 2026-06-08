import { useState, useEffect } from 'react';
import { api } from './api/client';
import { Navigation } from './components/layout/Navigation';
import { OnboardingWizard } from './features/onboarding/OnboardingWizard';
import { LoginForm } from './features/auth/LoginForm';
import { Dashboard } from './features/dashboard/Dashboard';
import { LibraryView } from './features/library/LibraryView';
import { Admin } from './features/dashboard/Admin';
import { VideoPlayer } from './features/player/VideoPlayer';
import { LibrariesTab } from './features/library/LibrariesTab';
import { MediaDetails } from './features/library/MediaDetails';
import type { SetupStatus, MediaItem, Library } from './types';

type AppStep = 'loading' | 'onboarding' | 'login' | 'dashboard' | 'admin' | 'library' | 'player' | 'libraries' | 'settings';

function App() {
  const [step, setStep] = useState<AppStep>('loading');
  const [status, setStatus] = useState<SetupStatus | null>(null);
  const [activeTab, setActiveTab] = useState('home');
  const [selectedLibrary, setSelectedLibrary] = useState<Library | null>(null);
  const [selectedItem, setSelectedItem] = useState<MediaItem | null>(null);
  const [playingMedia, setPlayingMedia] = useState<MediaItem | null>(null);
  const [previousStep, setPreviousStep] = useState<AppStep>('dashboard');
  const [isAdmin, setIsAdmin] = useState(false);

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
              setIsAdmin(profile.is_admin);
              localStorage.setItem('sunset_is_admin', profile.is_admin ? 'true' : 'false');
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
  if (step === 'login') return <LoginForm serverName={status?.server_name || 'SunSet'} onLogin={(_, admin) => { setIsAdmin(admin); setStep('dashboard'); }} />;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navigation activeTab={activeTab} onTabChange={handleTabChange} isAdmin={isAdmin} />
      
      <main style={{ 
        flex: 1, 
        paddingTop: step === 'dashboard' ? 0 : 'var(--header-height)',
        paddingBottom: 'var(--bottom-nav-height)',
        maxWidth: '100vw',
        overflowX: 'hidden'
      }}>
        <div style={{ padding: step === 'dashboard' ? 0 : 'var(--spacing-xl)' }}>
          {step === 'dashboard' && (
            <Dashboard 
              onSelectItem={item => setSelectedItem(item)}
            />
          )}

          {step === 'libraries' && (
            <LibrariesTab 
              isAdmin={isAdmin}
              onSelectLibrary={lib => { setSelectedLibrary(lib); setStep('library'); }}
              onGoToSettings={() => handleTabChange('settings')}
            />
          )}

          {step === 'library' && selectedLibrary && (
            <LibraryView 
              library={selectedLibrary}
              onSelectItem={item => setSelectedItem(item)}
              onBack={() => setStep('libraries')}
            />
          )}

          {step === 'settings' && <Admin />}
        </div>

        {selectedItem && (
          <MediaDetails 
            item={selectedItem} 
            onClose={() => setSelectedItem(null)} 
            onPlay={item => { setPlayingMedia(item); setPreviousStep(step); setStep('player'); setSelectedItem(null); }} 
          />
        )}

        {step === 'player' && playingMedia && (
          <VideoPlayer 
            item={playingMedia} 
            onClose={() => setStep(previousStep)} 
          />
        )}
      </main>
    </div>
  );
}

export default App;
