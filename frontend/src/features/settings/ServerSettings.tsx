import { useState } from 'react';
import { getCurrentServerUrl, setServerUrl } from '../../api/client';

export function ServerSettings() {
  const [url, setUrl] = useState(getCurrentServerUrl());
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    const trimmed = url.trim().replace(/\/+$/, '');
    if (!trimmed) {
      setError('Please enter a server URL');
      return;
    }
    if (!/^https?:\/\/.+/.test(trimmed)) {
      setError('URL must start with http:// or https://');
      return;
    }

    setError('');
    const testUrl = `${trimmed}/api/status`;
    try {
      const res = await fetch(testUrl);
      if (!res.ok) throw new Error();
      setServerUrl(trimmed);
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch {
      setError('Could not connect to server. Check the URL.');
    }
  };

  return (
    <div>
      <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem' }}>Server Connection</h3>
      <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
        Configure which SunSet server this desktop app connects to.
      </p>

      <label style={{
        display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)',
        marginBottom: '0.5rem', fontWeight: 600,
      }}>
        Server URL
      </label>
      <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
        <input
          value={url}
          onChange={e => { setUrl(e.target.value); setError(''); setSaved(false); }}
          onKeyDown={e => { if (e.key === 'Enter') handleSave(); }}
          placeholder="http://192.168.1.100:7867"
          style={{
            flex: 1,
            backgroundColor: 'var(--surface-variant)',
            border: error ? '1px solid var(--primary-color)' : '1px solid transparent',
            borderRadius: 'var(--radius-md)',
            padding: '0.75rem 1rem',
            color: 'var(--text-primary)',
            fontSize: '0.95rem',
            outline: 'none',
            transition: 'var(--transition-standard)',
            fontFamily: 'monospace',
          }}
          onFocus={e => {
            e.currentTarget.style.borderColor = 'var(--primary-color)';
            e.currentTarget.style.backgroundColor = 'var(--surface-color)';
          }}
          onBlur={e => {
            if (!error) e.currentTarget.style.borderColor = 'transparent';
            e.currentTarget.style.backgroundColor = 'var(--surface-variant)';
          }}
        />
        <button
          onClick={handleSave}
          style={{
            padding: '0.75rem 1.5rem',
            backgroundColor: saved ? '#22c55e' : 'var(--primary-color)',
            color: 'white',
            border: 'none',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.9rem',
            fontWeight: 700,
            cursor: 'pointer',
            transition: 'var(--transition-standard)',
            whiteSpace: 'nowrap',
          }}
        >
          {saved ? 'Saved' : 'Save'}
        </button>
      </div>
      {error && (
        <p style={{ color: 'var(--primary-color)', fontSize: '0.85rem', marginTop: '0.5rem' }}>
          {error}
        </p>
      )}
    </div>
  );
}
