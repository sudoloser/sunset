import React, { useState } from 'react';
import { Admin } from '../dashboard/Admin';
import { SubtitleSettings } from './SubtitleSettings';
import { AppearanceSettings } from './AppearanceSettings';
import { DiscordSettings } from './DiscordSettings';
import { AccountSettings } from './AccountSettings';

interface SettingsProps {
  isAdmin: boolean;
}

type SettingsTab = 'account' | 'subtitles' | 'appearance' | 'discord' | 'admin';

export const Settings: React.FC<SettingsProps> = ({ isAdmin }) => {
  const [tab, setTab] = useState<SettingsTab>('account');
  const userId = localStorage.getItem('sunset_user_id') || '';
  const username = localStorage.getItem('sunset_username') || 'User';

  const tabs: { id: SettingsTab; label: string }[] = [
    { id: 'account', label: 'Account' },
    { id: 'subtitles', label: 'Subtitles' },
    { id: 'appearance', label: 'Appearance' },
    { id: 'discord', label: 'Discord' },
  ];
  if (isAdmin) {
    tabs.push({ id: 'admin', label: 'Admin' });
  }

  return (
    <div style={{ height: 'calc(100vh - var(--header-height) - var(--bottom-nav-height) - 2rem)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem', overflowX: 'auto', flexShrink: 0 }}>
        {tabs.map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            style={{
              background: 'transparent', border: 'none', color: tab === t.id ? 'white' : 'var(--text-secondary)',
              padding: '0.5rem 1.25rem', borderRadius: 'var(--radius-md)', cursor: 'pointer',
              fontWeight: tab === t.id ? 700 : 500, fontSize: '0.95rem', whiteSpace: 'nowrap',
              borderBottom: tab === t.id ? '2px solid var(--primary-color)' : '2px solid transparent',
              transition: 'var(--transition-standard)'
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div style={{ flex: 1, overflowY: 'auto', paddingTop: '1.5rem' }}>
        {tab === 'account' && <AccountSettings userId={userId} currentUsername={username} isAdmin={isAdmin} />}
        {tab === 'subtitles' && <SubtitleSettings />}
        {tab === 'appearance' && <AppearanceSettings />}
        {tab === 'discord' && <DiscordSettings />}
        {tab === 'admin' && <Admin />}
      </div>
    </div>
  );
};
