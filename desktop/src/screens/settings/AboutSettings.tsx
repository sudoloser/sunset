import { useEffect, useState } from 'react';
import { appVersion } from '../../discord';

export function AboutSettings({ serverUrl }: { serverUrl: string }) {
  const [version, setVersion] = useState('');

  useEffect(() => {
    void appVersion().then(setVersion);
  }, []);

  return (
    <div style={{ maxWidth: 560 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>About</h3>
      <p>
        <span className="sl-tag">SunSet desktop {version}</span>
      </p>
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
        Connected to {serverUrl}
      </p>
      <p style={{ fontSize: 'var(--sl-text-sm)' }}>
        Anime upscaling plays through mpv with Anime4K shaders. Embedding mpv inside the app window
        is planned; today it opens in its own window.
      </p>
    </div>
  );
}
