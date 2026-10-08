import { useState } from 'react';
import { api } from '../../api/client';
import { setServerUrl } from '../../store';

export function ServerSettings({ serverUrl }: { serverUrl: string }) {
  const [url, setUrl] = useState(serverUrl);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  const save = async () => {
    const trimmed = url.trim().replace(/\/+$/, '');
    if (!/^https?:\/\/.+/.test(trimmed)) {
      setError('URL must start with http:// or https://');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await api.getStatus(trimmed);
      await setServerUrl(trimmed);
      setSaved(true);
      setTimeout(() => window.location.reload(), 800);
    } catch {
      setError('Could not connect to a SunSet server there.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Server connection</h3>
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
        Point the app at a different SunSet server. The app reloads after switching.
      </p>
      <div style={{ display: 'flex', gap: 'var(--sl-space-sm)', maxWidth: 520 }}>
        <input className="sl-input" value={url} onChange={e => setUrl(e.target.value)} />
        <button className="sl-button" onClick={() => void save()} disabled={busy}>
          {saved ? 'Saved' : busy ? '...' : 'Save'}
        </button>
      </div>
      {error && <p className="sl-error">{error}</p>}
    </div>
  );
}
