import { useState } from 'react';
import { api } from '../api/client';
import { setSession } from '../session';

interface DraftLib {
  name: string;
  path: string;
  lib_type: 'movies' | 'shows';
}

export function Onboarding({ serverUrl, onDone }: { serverUrl: string; onDone: () => void }) {
  const [serverName, setServerName] = useState('SunSet');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [libs, setLibs] = useState<DraftLib[]>([{ name: 'Movies', path: '', lib_type: 'movies' }]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const updateLib = (i: number, patch: Partial<DraftLib>) =>
    setLibs(prev => prev.map((l, j) => (j === i ? { ...l, ...patch } : l)));

  const handleFinish = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError('Pick an admin username and password.');
      return;
    }
    if (libs.some(l => !l.name.trim() || !l.path.trim())) {
      setError('Every library needs a name and a folder path on the server.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await api.onboard(serverUrl, {
        server_name: serverName.trim() || 'SunSet',
        admin_user: { username: username.trim(), password_hash: password },
        libraries: libs.map(l => ({ name: l.name.trim(), path: l.path.trim(), lib_type: l.lib_type })),
      });
      // Onboarding creates the admin user; log straight in.
      const user = await api.login(serverUrl, username.trim(), password);
      if (user) await setSession({ user_id: user.user_id, username: user.username, is_admin: true });
      onDone();
    } catch {
      setError('Setup failed. Check the server and try again.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="sl-center">
      <h1 className="sl-hero">
        sun<span style={{ color: 'var(--sl-accent)' }}>set.</span>
      </h1>
      <p style={{ color: 'var(--sl-text-link)' }}>first-time setup</p>
      <form
        className="sl-card"
        style={{ width: 'min(480px, 94vw)', marginTop: 'var(--sl-space-lg)' }}
        onSubmit={e => void handleFinish(e)}
      >
        <label style={{ display: 'block', fontWeight: 700, marginBottom: 'var(--sl-space-sm)' }}>
          Server name
        </label>
        <input className="sl-input" value={serverName} onChange={e => setServerName(e.target.value)} />

        <h3 className="sl-section-title" style={{ margin: 'var(--sl-space-lg) 0 var(--sl-space-md)' }}>
          Admin account
        </h3>
        <label style={{ display: 'block', fontWeight: 700, marginBottom: 'var(--sl-space-sm)' }}>
          Username
        </label>
        <input className="sl-input" value={username} onChange={e => setUsername(e.target.value)} />
        <label
          style={{ display: 'block', fontWeight: 700, margin: 'var(--sl-space-md) 0 var(--sl-space-sm)' }}
        >
          Password
        </label>
        <input
          className="sl-input"
          type="password"
          value={password}
          onChange={e => setPassword(e.target.value)}
        />

        <h3 className="sl-section-title" style={{ margin: 'var(--sl-space-lg) 0 var(--sl-space-md)' }}>
          Libraries
        </h3>
        {libs.map((lib, i) => (
          <div key={i} className="sl-card" style={{ marginBottom: 'var(--sl-space-md)', padding: 'var(--sl-space-md)' }}>
            <input
              className="sl-input"
              placeholder="Name (e.g. Movies)"
              value={lib.name}
              onChange={e => updateLib(i, { name: e.target.value })}
            />
            <input
              className="sl-input"
              style={{ marginTop: 'var(--sl-space-sm)' }}
              placeholder="Folder on the server (e.g. /media/movies)"
              value={lib.path}
              onChange={e => updateLib(i, { path: e.target.value })}
            />
            <div style={{ display: 'flex', gap: 'var(--sl-space-sm)', marginTop: 'var(--sl-space-sm)' }}>
              <button
                type="button"
                className="sl-button"
                style={lib.lib_type === 'movies' ? undefined : { opacity: 0.5 }}
                onClick={() => updateLib(i, { lib_type: 'movies' })}
              >
                Movies
              </button>
              <button
                type="button"
                className="sl-button"
                style={lib.lib_type === 'shows' ? undefined : { opacity: 0.5 }}
                onClick={() => updateLib(i, { lib_type: 'shows' })}
              >
                TV Shows
              </button>
              {libs.length > 1 && (
                <button
                  type="button"
                  className="sl-button"
                  style={{ opacity: 0.6 }}
                  onClick={() => setLibs(prev => prev.filter((_, j) => j !== i))}
                >
                  Remove
                </button>
              )}
            </div>
          </div>
        ))}
        <button
          type="button"
          className="sl-button"
          style={{ width: '100%' }}
          onClick={() => setLibs(prev => [...prev, { name: '', path: '', lib_type: 'shows' }])}
        >
          + Add library
        </button>

        {error && <p className="sl-error">{error}</p>}
        <button
          className="sl-button"
          type="submit"
          disabled={busy}
          style={{ marginTop: 'var(--sl-space-md)', width: '100%', padding: '0.7rem' }}
        >
          {busy ? 'Setting up...' : 'Finish setup'}
        </button>
      </form>
    </div>
  );
}
