import { useNavigate } from 'react-router-dom';
import { assetUrl, type MediaItem } from '../api/client';

export function Hero({ serverUrl, item }: { serverUrl: string; item: MediaItem }) {
  const navigate = useNavigate();
  const backdrop = assetUrl(serverUrl, item.id, 'backdrop.jpg');
  const logo = assetUrl(serverUrl, item.id, 'logo.png');

  return (
    <div
      style={{
        borderRadius: 'var(--sl-radius-md)',
        border: '2px solid var(--sl-text)',
        minHeight: 320,
        display: 'flex',
        alignItems: 'flex-end',
        padding: 'var(--sl-space-lg)',
        background: `var(--sl-bg-soft) url("${backdrop}") center / cover`,
        marginBottom: 'var(--sl-space-lg)',
      }}
    >
      <div
        className="sl-card"
        style={{ maxWidth: 560, background: 'rgba(255,255,255,0.92)' }}
      >
        <img
          src={logo}
          alt=""
          style={{ maxHeight: 72, maxWidth: '100%', display: 'block', marginBottom: 'var(--sl-space-sm)' }}
          onError={e => {
            (e.target as HTMLImageElement).style.display = 'none';
          }}
        />
        <div style={{ fontSize: 'var(--sl-text-2xl)', fontWeight: 700 }}>
          {item.show_title ?? item.title}
        </div>
        {item.description && (
          <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
            {item.description.slice(0, 220)}
            {item.description.length > 220 ? '...' : ''}
          </p>
        )}
        <div style={{ display: 'flex', gap: 'var(--sl-space-sm)', marginTop: 'var(--sl-space-md)' }}>
          <button className="sl-button" onClick={() => navigate(`/play/${item.id}`)}>
            Play
          </button>
          <button className="sl-button" onClick={() => navigate(`/item/${item.id}`)}>
            More info
          </button>
        </div>
      </div>
    </div>
  );
}
