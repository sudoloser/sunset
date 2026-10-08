import { useEffect, useState } from 'react';
import { loadPref, savePref, type SubtitlePrefs } from '../../prefs';

const COLORS = ['#ffffff', '#ffff00', '#00ffff', '#ffaaaa'];
const FONTS = ['sans-serif', 'serif', 'monospace', 'Varela Round'];

export function SubtitleSettings() {
  const [prefs, setPrefs] = useState<SubtitlePrefs | null>(null);

  useEffect(() => {
    void loadPref('sunset_prefs_subtitles').then(setPrefs);
  }, []);

  if (!prefs) return <p>loading...</p>;

  const update = (patch: Partial<SubtitlePrefs>) => {
    const next = { ...prefs, ...patch };
    setPrefs(next);
    void savePref('sunset_prefs_subtitles', next);
  };

  return (
    <div style={{ maxWidth: 520 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Subtitles</h3>
      <label style={{ fontWeight: 700 }}>Color</label>
      <div style={{ display: 'flex', gap: 'var(--sl-space-sm)', margin: 'var(--sl-space-sm) 0' }}>
        {COLORS.map(c => (
          <button
            key={c}
            onClick={() => update({ color: c })}
            title={c}
            style={{
              width: 36,
              height: 36,
              borderRadius: 'var(--sl-radius-pill)',
              background: c,
              border: prefs.color === c ? '3px solid var(--sl-text)' : '2px solid transparent',
              cursor: 'pointer',
            }}
          />
        ))}
        <input
          type="color"
          value={prefs.color}
          onChange={e => update({ color: e.target.value })}
          style={{ width: 36, height: 36, border: 'none', background: 'none', cursor: 'pointer' }}
        />
      </div>
      <label style={{ fontWeight: 700 }}>Size: {prefs.size}%</label>
      <input
        type="range"
        min={50}
        max={200}
        step={10}
        value={prefs.size}
        onChange={e => update({ size: Number(e.target.value) })}
        style={{ width: '100%' }}
      />
      <label style={{ fontWeight: 700, display: 'block', marginTop: 'var(--sl-space-sm)' }}>Font</label>
      <select
        className="sl-input"
        value={prefs.font}
        onChange={e => update({ font: e.target.value })}
      >
        {FONTS.map(f => (
          <option key={f} value={f}>
            {f}
          </option>
        ))}
      </select>
      <label style={{ display: 'flex', gap: 'var(--sl-space-sm)', alignItems: 'center', marginTop: 'var(--sl-space-sm)' }}>
        <input type="checkbox" checked={prefs.bold} onChange={e => update({ bold: e.target.checked })} />
        Bold
      </label>
      <label style={{ fontWeight: 700, display: 'block', marginTop: 'var(--sl-space-sm)' }}>
        Background opacity: {prefs.backgroundOpacity}
      </label>
      <input
        type="range"
        min={0}
        max={0.9}
        step={0.1}
        value={prefs.backgroundOpacity}
        onChange={e => update({ backgroundOpacity: Number(e.target.value) })}
        style={{ width: '100%' }}
      />
      <div
        className="sl-card"
        style={{ marginTop: 'var(--sl-space-md)', background: '#000', borderColor: 'var(--sl-text)' }}
      >
        <span
          style={{
            color: prefs.color,
            fontFamily: prefs.font,
            fontSize: `${prefs.size}%`,
            fontWeight: prefs.bold ? 700 : 400,
            background:
              prefs.backgroundOpacity > 0 ? `rgba(0,0,0,${prefs.backgroundOpacity})` : 'transparent',
          }}
        >
          Preview subtitle line
        </span>
      </div>
    </div>
  );
}
