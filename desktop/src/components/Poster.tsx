import { assetUrl, type MediaItem } from '../api/client';

interface PosterProps {
  serverUrl: string;
  item: MediaItem;
  /** Poster is keyed off this item so grouped shows use their first episode. */
  imageFrom?: MediaItem;
  title?: string;
  subtitle?: string;
  onClick?: () => void;
}

export function Poster({ serverUrl, item, imageFrom, title, subtitle, onClick }: PosterProps) {
  const src = assetUrl(serverUrl, (imageFrom ?? item).id, 'folder.jpg');
  return (
    <div className="sl-poster-lazy" style={{ width: 150, flexShrink: 0 }}>
      <div
        onClick={onClick}
        style={{
          width: 150,
          height: 225,
          borderRadius: 'var(--sl-radius-md)',
          border: '2px solid var(--sl-text)',
          background: `var(--sl-bg-soft) url("${src}") center / cover`,
          cursor: onClick ? 'pointer' : 'default',
        }}
      />
      <div style={{ fontWeight: 700, marginTop: 'var(--sl-space-xs)', fontSize: 'var(--sl-text-sm)' }}>
        {title ?? item.show_title ?? item.title}
      </div>
      {subtitle && (
        <div style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-xs)' }}>{subtitle}</div>
      )}
    </div>
  );
}
