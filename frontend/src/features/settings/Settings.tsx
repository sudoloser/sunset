import React, { useState } from 'react';
import { Admin } from '../dashboard/Admin';
import { SubtitleSettings } from './SubtitleSettings';
import { AppearanceSettings } from './AppearanceSettings';

interface SettingsProps {
  isAdmin: boolean;
}

type SettingsTab = 'subtitles' | 'appearance' | 'admin';

export const Settings: React.FC<SettingsProps> = ({ isAdmin }) => {
  const [tab, setTab] = useState<SettingsTab>('subtitles');

  const tabs: { id: SettingsTab; label: string }[] = [
    { id: 'subtitles', label: 'Subtitles' },
    { id: 'appearance', label: 'Appearance' },
  ];
  if (isAdmin) {
    tabs.push({ id: 'admin', label: 'Admin' });
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '2rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
        {tabs.map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            style={{
              background: 'transparent', border: 'none', color: tab === t.id ? 'white' : 'var(--text-secondary)',
              padding: '0.5rem 1.25rem', borderRadius: 'var(--radius-md)', cursor: 'pointer',
              fontWeight: tab === t.id ? 700 : 500, fontSize: '0.95rem',
              borderBottom: tab === t.id ? '2px solid var(--primary-color)' : '2px solid transparent',
              transition: 'var(--transition-standard)'
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'subtitles' && <SubtitleSettings />}
      {tab === 'appearance' && <AppearanceSettings />}
      {tab === 'admin' && <Admin />}
    </div>
  );
};
