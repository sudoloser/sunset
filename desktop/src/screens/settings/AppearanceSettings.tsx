import { useEffect, useState } from 'react';
import { loadPref, savePref, type AppearancePrefs } from '../../prefs';

export function AppearanceSettings() {
  const [prefs, setPrefs] = useState<AppearancePrefs | null>(null);

  useEffect(() => {
    void loadPref('sunset_prefs_appearance').then(setPrefs);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.density = prefs?.density ?? 'comfortable';
  }, [prefs]);

  if (!prefs) return <p>loading...</p>;

  const pick = (density: AppearancePrefs['density']) => {
    const next = { ...prefs, density };
    setPrefs(next);
    void savePref('sunset_prefs_appearance', next);
  };

  return (
    <div style={{ maxWidth: 520 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Appearance</h3>
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
        SunSet desktop follows the sudoloser.design light theme.
      </p>
      <label style={{ fontWeight: 700 }}>Density</label>
      <div style={{ display: 'flex', gap: 'var(--sl-space-sm)', marginTop: 'var(--sl-space-sm)' }}>
        {(['comfortable', 'compact'] as const).map(d => (
          <button
            key={d}
            className="sl-button"
            style={prefs.density === d ? undefined : { opacity: 0.55 }}
            onClick={() => pick(d)}
          >
            {d === 'comfortable' ? 'Comfortable' : 'Compact'}
          </button>
        ))}
      </div>
    </div>
  );
}
