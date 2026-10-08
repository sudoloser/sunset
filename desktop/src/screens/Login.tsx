import { useState } from 'react';
import { api } from '../api/client';
import { setSession } from '../session';

export function Login({
  serverUrl,
  serverName,
  onDone,
}: {
  serverUrl: string;
  serverName: string;
  onDone: () => void;
}) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const user = await api.login(serverUrl, username, password);
      if (!user) {
        setError('Invalid credentials.');
        return;
      }
      await setSession({ user_id: user.user_id, username: user.username, is_admin: user.is_admin });
      onDone();
    } catch {
      setError('Login failed. Is the server still reachable?');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="sl-center">
      <h1 className="sl-hero">
        sun<span style={{ color: 'var(--sl-accent)' }}>set.</span>
      </h1>
      <p style={{ color: 'var(--sl-text-link)' }}>{serverName || 'welcome back'}</p>
      <form
        className="sl-card"
        style={{ width: 'min(400px, 90vw)', marginTop: 'var(--sl-space-lg)' }}
        onSubmit={e => void handleLogin(e)}
      >
        <label style={{ display: 'block', fontWeight: 700, marginBottom: 'var(--sl-space-sm)' }}>
          Username
        </label>
        <input
          className="sl-input"
          value={username}
          onChange={e => setUsername(e.target.value)}
          autoComplete="username"
          autoFocus
        />
        <label
          style={{
            display: 'block',
            fontWeight: 700,
            margin: 'var(--sl-space-md) 0 var(--sl-space-sm)',
          }}
        >
          Password
        </label>
        <input
          className="sl-input"
          type="password"
          value={password}
          onChange={e => setPassword(e.target.value)}
          autoComplete="current-password"
        />
        {error && <p className="sl-error">{error}</p>}
        <button
          className="sl-button"
          type="submit"
          disabled={busy}
          style={{ marginTop: 'var(--sl-space-md)', width: '100%', padding: '0.7rem' }}
        >
          {busy ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  );
}
