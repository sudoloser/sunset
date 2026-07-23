import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setServerUrl, getCurrentServerUrl } from '../api/client';

export function ServerSetupPage() {
  const navigate = useNavigate();
  const [url, setUrl] = useState(getCurrentServerUrl());
  const [error, setError] = useState('');
  const [testing, setTesting] = useState(false);

  const handleConnect = async () => {
    const trimmed = url.trim().replace(/\/+$/, '');
    if (!trimmed) {
      setError('Please enter a server URL');
      return;
    }
    if (!/^https?:\/\/.+/.test(trimmed)) {
      setError('URL must start with http:// or https://');
      return;
    }

    setTesting(true);
    setError('');
    const testUrl = `${trimmed}/api/status`;
    try {
      const res = await fetch(testUrl);
      if (!res.ok) throw new Error(`Server returned ${res.status}`);
      setServerUrl(trimmed);
      navigate('/', { replace: true });
    } catch (e) {
      setError('Could not connect to server. Check the URL and ensure the server is running.');
    } finally {
      setTesting(false);
    }
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      background: 'var(--bg-color)',
      padding: '2rem',
    }}>
      <div style={{
        width: '100%',
        maxWidth: '440px',
        textAlign: 'center',
      }}>
        <div style={{ marginBottom: '3rem' }}>
          <h1 style={{
            fontFamily: 'var(--font-display)',
            fontSize: '3rem',
            fontWeight: 700,
            letterSpacing: '-0.04em',
            marginBottom: '0.5rem',
          }}>
            Sun<span style={{ color: 'var(--primary-color)' }}>Set</span>
          </h1>
          <p style={{
            color: 'var(--text-secondary)',
            fontSize: '1.1rem',
          }}>
            Connect to your media server
          </p>
        </div>

        <div style={{
          background: 'var(--surface-color)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '2rem',
          textAlign: 'left',
        }}>
          <label style={{
            display: 'block',
            fontSize: '0.85rem',
            color: 'var(--text-secondary)',
            marginBottom: '0.5rem',
            fontWeight: 600,
          }}>
            Server URL
          </label>
          <input
            value={url}
            onChange={e => { setUrl(e.target.value); setError(''); }}
            onKeyDown={e => { if (e.key === 'Enter') handleConnect(); }}
            placeholder="http://192.168.1.100:7867"
            style={{
              width: '100%',
              backgroundColor: 'var(--surface-variant)',
              border: error ? '1px solid var(--primary-color)' : '1px solid transparent',
              borderRadius: 'var(--radius-md)',
              padding: '0.9rem 1.2rem',
              color: 'var(--text-primary)',
              fontSize: '1rem',
              outline: 'none',
              transition: 'var(--transition-standard)',
              marginBottom: '1rem',
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

          {error && (
            <p style={{
              color: 'var(--primary-color)',
              fontSize: '0.85rem',
              marginBottom: '1rem',
              lineHeight: '1.4',
            }}>
              {error}
            </p>
          )}

          <button
            onClick={handleConnect}
            disabled={testing}
            style={{
              width: '100%',
              padding: '0.9rem 1.5rem',
              backgroundColor: testing ? 'rgba(229, 9, 20, 0.5)' : 'var(--primary-color)',
              color: 'white',
              border: 'none',
              borderRadius: 'var(--radius-xl)',
              fontSize: '1rem',
              fontWeight: 700,
              cursor: testing ? 'not-allowed' : 'pointer',
              transition: 'var(--transition-standard)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
            }}
          >
            {testing && (
              <span style={{
                width: '18px', height: '18px',
                border: '2px solid rgba(255,255,255,0.3)',
                borderTopColor: 'white',
                borderRadius: '50%',
                animation: 'spin 0.8s linear infinite',
                display: 'inline-block',
              }} />
            )}
            {testing ? 'Connecting...' : 'Connect'}
          </button>
        </div>
      </div>
    </div>
  );
}
