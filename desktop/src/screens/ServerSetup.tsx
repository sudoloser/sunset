import { useState } from 'react';
import { api } from '../api/client';
import { setServerUrl } from '../store';

function normalize(raw: string): string {
  const trimmed = raw.trim().replace(/\/+$/, '');
  // Bare IP or hostname gets http + default port.
  if (/^[\w.-]+(:\d+)?$/.test(trimmed)) return `http://${trimmed || '127.0.0.1:7867'}`;
  return trimmed;
}

export function ServerSetup({ onDone }: { onDone: () => void }) {
  const [url, setUrl] = useState('192.168.12.200:7867');
  const [error, setError] = useState('');
  const [serverName, setServerName] = useState('');
  const [testing, setTesting] = useState(false);

  const handleConnect = async () => {
    const candidate = normalize(url);
    if (!/^https?:\/\/.+/.test(candidate)) {
      setError('Enter an IP/hostname like 192.168.1.100:7867, or a full http(s) URL.');
      return;
    }
    setTesting(true);
    setError('');
    try {
      const status = await api.getStatus(candidate);
      setServerName(status.server_name ?? '');
      await setServerUrl(candidate);
      onDone();
    } catch {
      setError('Could not reach a SunSet server there. Check the IP and port.');
    } finally {
      setTesting(false);
    }
  };

  return (
    <div className="sl-center">
      <h1 className="sl-hero">
        sun<span style={{ color: 'var(--sl-accent)' }}>set.</span>
      </h1>
      <p style={{ color: 'var(--sl-text-link)' }}>point the app at your server</p>
      <div className="sl-card" style={{ width: 'min(420px, 90vw)', marginTop: 'var(--sl-space-lg)' }}>
        <label style={{ display: 'block', fontWeight: 700, marginBottom: 'var(--sl-space-sm)' }}>
          Server IP
        </label>
        <input
          className="sl-input sl-no-drag"
          value={url}
          onChange={e => { setUrl(e.target.value); setError(''); }}
          onKeyDown={e => { if (e.key === 'Enter') void handleConnect(); }}
          placeholder="192.168.1.100:7867"
          autoFocus
        />
        {serverName && <p className="sl-tag" style={{ marginTop: 'var(--sl-space-md)' }}>{serverName}</p>}
        {error && <p className="sl-error">{error}</p>}
        <button
          className="sl-button sl-no-drag"
          style={{ marginTop: 'var(--sl-space-md)', width: '100%', padding: '0.7rem' }}
          onClick={() => void handleConnect()}
          disabled={testing}
        >
          {testing ? 'Connecting...' : 'Connect'}
        </button>
      </div>
    </div>
  );
}
