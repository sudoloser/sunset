import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { ErrorBoundary } from './ErrorBoundary';
import { AppLayout } from './layouts/AppLayout';
import { DashboardPage } from './pages/DashboardPage';
import { LibrariesPage } from './pages/LibrariesPage';
import { SettingsPage } from './pages/SettingsPage';
import { CollectionPage } from './pages/CollectionPage';
import { ServerSetupPage } from './pages/ServerSetup';
import { OnboardingWizard } from './features/onboarding/OnboardingWizard';
import { LoginForm } from './features/auth/LoginForm';
import { api, getCurrentServerUrl } from './api/client';
import { isDesktop } from './desktop';

const loadingStyle = {
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  minHeight: '100vh', background: '#000',
} as const;

function LoadingScreen() {
  return (
    <div style={loadingStyle}>
      <div style={{
        width: 32, height: 32,
        border: '3px solid rgba(255,255,255,0.15)',
        borderTopColor: '#e50914',
        borderRadius: '50%',
        animation: 'spin 0.7s linear infinite',
      }} />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

function FullPageFallback({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', background: 'var(--bg-color)' }}>
      {children}
    </div>
  );
}

function OnboardingRoute() {
  const navigate = useNavigate();
  const [ready, setReady] = useState(false);
  const [needsSetup, setNeedsSetup] = useState(true);

  useEffect(() => {
    api.getStatus().then(data => {
      if (data.setup_complete) {
        setNeedsSetup(false);
      }
      setReady(true);
    }).catch(() => setReady(true));
  }, []);

  if (!ready) return <LoadingScreen />;
  if (!needsSetup) return <Navigate to="/login" replace />;
  return (
    <FullPageFallback>
      <OnboardingWizard onComplete={() => navigate('/login', { replace: true })} />
    </FullPageFallback>
  );
}

function LoginRoute() {
  const navigate = useNavigate();
  const [ready, setReady] = useState(false);
  const [serverName, setServerName] = useState('SunSet');

  useEffect(() => {
    const uid = localStorage.getItem('sunset_user_id');
    if (uid) {
      api.getUserProfile(uid).then(profile => {
        if (profile) navigate('/home', { replace: true });
      }).catch(() => {});
    }
    api.getStatus().then(data => setServerName(data.server_name || 'SunSet')).catch(() => {});
    setReady(true);
  }, [navigate]);

  if (!ready) return <LoadingScreen />;
  return (
    <FullPageFallback>
      <LoginForm
        serverName={serverName}
        onLogin={(_uid, admin, username) => {
          localStorage.setItem('sunset_username', username);
          localStorage.setItem('sunset_is_admin', admin ? 'true' : 'false');
          navigate('/home', { replace: true });
        }}
      />
    </FullPageFallback>
  );
}

export default function App() {
  const needsServerUrl = isDesktop() && !getCurrentServerUrl();

  return (
    <ErrorBoundary>
      {needsServerUrl ? (
        <ServerSetupPage />
      ) : (
        <Routes>
          <Route path="/server-setup" element={<ServerSetupPage />} />
          <Route path="/onboarding" element={<OnboardingRoute />} />
          <Route path="/login" element={<LoginRoute />} />
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/home" replace />} />
            <Route path="home" element={<DashboardPage />} />
            <Route path="libraries" element={<LibrariesPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="collection/:name" element={<CollectionPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/home" replace />} />
        </Routes>
      )}
    </ErrorBoundary>
  );
}
