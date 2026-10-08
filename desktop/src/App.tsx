import { useCallback, useEffect, useState } from 'react';
import { Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { api } from './api/client';
import { getServerUrl } from './store';
import { clearSession, getSession, type Session } from './session';
import { ServerSetup } from './screens/ServerSetup';
import { Login } from './screens/Login';
import { Onboarding } from './screens/Onboarding';
import { Home } from './screens/Home';
import { Libraries, LibraryDetail } from './screens/Libraries';
import { Detail } from './screens/Detail';
import { Search } from './screens/Search';
import { Player } from './screens/Player';
import { Settings } from './screens/Settings';

function Shell({
  serverUrl,
  session,
  onLogout,
}: {
  serverUrl: string;
  session: Session;
  onLogout: () => void;
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const tab = location.pathname.startsWith('/libraries')
    ? 'libraries'
    : location.pathname.startsWith('/search')
      ? 'search'
      : location.pathname.startsWith('/settings')
        ? 'settings'
        : 'home';

  return (
    <div>
      <nav
        className="sl-drag"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--sl-space-md)',
          padding: 'var(--sl-space-sm) var(--sl-space-md)',
          borderBottom: '2px solid var(--sl-text)',
          background: 'var(--sl-bg)',
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}
      >
        <strong style={{ fontSize: 'var(--sl-text-lg)' }}>
          sun<span style={{ color: 'var(--sl-accent)' }}>set.</span>
        </strong>
        {(
          [
            ['home', '/', 'Home'],
            ['libraries', '/libraries', 'Libraries'],
            ['search', '/search', 'Search'],
            ['settings', '/settings', 'Settings'],
          ] as const
        ).map(([id, to, label]) => (
          <button
            key={id}
            className="sl-button sl-no-drag"
            style={tab === id ? undefined : { opacity: 0.55 }}
            onClick={() => navigate(to)}
          >
            {label}
          </button>
        ))}
        <span style={{ flex: 1 }} />
        <span className="sl-tag">{session.username}</span>
        <button className="sl-button sl-no-drag" style={{ opacity: 0.7 }} onClick={onLogout}>
          Log out
        </button>
      </nav>
      <Routes>
        <Route path="/" element={<Home serverUrl={serverUrl} session={session} />} />
        <Route path="/libraries" element={<Libraries serverUrl={serverUrl} />} />
        <Route path="/libraries/:id" element={<LibraryDetail serverUrl={serverUrl} session={session} />} />
        <Route path="/item/:id" element={<Detail serverUrl={serverUrl} session={session} />} />
        <Route path="/search" element={<Search serverUrl={serverUrl} session={session} />} />
        <Route path="/play/:id" element={<Player serverUrl={serverUrl} session={session} />} />
        <Route path="/settings" element={<Settings serverUrl={serverUrl} session={session} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}

export default function App() {
  const [serverUrl, setUrl] = useState<string | null>(null);
  const [serverName, setServerName] = useState('');
  const [fresh, setFresh] = useState(false);
  const [session, setSessionState] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);

  const navigate = useNavigate();
  const refresh = useCallback(async () => {
    const url = await getServerUrl();
    setUrl(url);
    if (!url) {
      setReady(true);
      return;
    }
    try {
      const status = await api.getStatus(url);
      setServerName(status.server_name ?? '');
      setFresh(!status.setup_complete);
    } catch {
      // Server unreachable: stay on setup so the user can fix the IP.
      setUrl(null);
    }
    setSessionState(await getSession());
    setReady(true);
    // Native Discord RPC replaces the server pipeline on desktop:
    // reconnect silently at boot when the user opted in.
    try {
      const { loadPref } = await import('./prefs');
      const { startRpc } = await import('./discord');
      const p = await loadPref('sunset_prefs_discord');
      if (p.autoConnect && p.clientId.trim()) {
        await startRpc(p.clientId.trim());
      }
    } catch {
      // Discord not running (or no client ID): presence just stays off.
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const handleDone = useCallback(async () => {
    await refresh();
    navigate('/', { replace: true });
  }, [refresh, navigate]);

  const handleLogout = useCallback(async () => {
    await clearSession();
    setSessionState(null);
  }, []);

  if (!ready) return <div className="sl-center">loading...</div>;

  return (
    <Routes>
      <Route path="/server-setup" element={<ServerSetup onDone={() => void handleDone()} />} />
      <Route
        path="/login"
        element={
          serverUrl ? (
            <Login serverUrl={serverUrl} serverName={serverName} onDone={() => void handleDone()} />
          ) : (
            <Navigate to="/server-setup" replace />
          )
        }
      />
      <Route
        path="/onboarding"
        element={
          serverUrl ? (
            <Onboarding serverUrl={serverUrl} onDone={() => void handleDone()} />
          ) : (
            <Navigate to="/server-setup" replace />
          )
        }
      />
      <Route
        path="/*"
        element={
          !serverUrl ? (
            <Navigate to="/server-setup" replace />
          ) : fresh ? (
            <Navigate to="/onboarding" replace />
          ) : !session ? (
            <Navigate to="/login" replace />
          ) : (
            <Shell serverUrl={serverUrl} session={session} onLogout={() => void handleLogout()} />
          )
        }
      />
    </Routes>
  );
}
