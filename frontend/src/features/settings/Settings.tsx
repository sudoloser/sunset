import React, { useState } from 'react';
import { Admin } from '../dashboard/Admin';
import { SubtitleSettings } from './SubtitleSettings';
import { AppearanceSettings } from './AppearanceSettings';
import { DiscordSettings } from './DiscordSettings';
import { AccountSettings } from './AccountSettings';

interface SettingsProps {
  isAdmin: boolean;
}

type SettingsTab = 'media' | 'account' | 'appearance' | 'discord' | 'admin';

export const Settings: React.FC<SettingsProps> = ({ isAdmin }) => {
  const [tab, setTab] = useState<SettingsTab>('media');
  const userId = localStorage.getItem('sunset_user_id') || '';
  const username = localStorage.getItem('sunset_username') || 'User';

  const tabs: { id: SettingsTab; label: string }[] = [
    { id: 'media', label: 'Media' },
    { id: 'account', label: 'Account' },
    { id: 'appearance', label: 'Appearance' },
    { id: 'discord', label: 'Discord' },
  ];
  if (isAdmin) {
    tabs.push({ id: 'admin', label: 'Admin' });
  }

  return (
    <div style={{ height: 'calc(100vh - var(--header-height) - var(--safe-area-top) - var(--bottom-nav-height) - var(--safe-area-bottom) - 2rem)', display: 'flex', flexDirection: 'column' }}>
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
        {tab === 'media' && (
          <div style={{ maxWidth: '800px', paddingBottom: 'var(--spacing-xxl)' }}>
            <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '3rem' }}>Media</h2>

            <SubtitleSettings />
          </div>
        )}
        {tab === 'account' && <AccountSettings userId={userId} currentUsername={username} isAdmin={isAdmin} />}
        {tab === 'appearance' && <AppearanceSettings />}
        {tab === 'discord' && <DiscordSettings />}
        {tab === 'admin' && <Admin />}
      </div>
    </div>
  );
};
