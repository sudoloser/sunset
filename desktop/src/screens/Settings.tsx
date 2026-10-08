import { useState } from 'react';
import type { Session } from '../session';
import { ServerSettings } from './settings/ServerSettings';
import { AccountSettings } from './settings/AccountSettings';
import { AppearanceSettings } from './settings/AppearanceSettings';
import { SubtitleSettings } from './settings/SubtitleSettings';
import { PlaybackSettings } from './settings/PlaybackSettings';
import { DiscordSettings } from './settings/DiscordSettings';
import { AdminSettings } from './settings/AdminSettings';
import { AboutSettings } from './settings/AboutSettings';

type Tab = 'server' | 'account' | 'appearance' | 'subtitles' | 'playback' | 'discord' | 'admin' | 'about';

export function Settings({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const [tab, setTab] = useState<Tab>('server');

  const tabs: { id: Tab; label: string; admin?: boolean }[] = [
    { id: 'server', label: 'Server' },
    { id: 'account', label: 'Account' },
    { id: 'appearance', label: 'Appearance' },
    { id: 'subtitles', label: 'Subtitles' },
    { id: 'playback', label: 'Playback' },
    { id: 'discord', label: 'Discord' },
    { id: 'admin', label: 'Admin', admin: true },
    { id: 'about', label: 'About' },
  ];

  return (
    <div style={{ padding: 'var(--sl-space-lg)', maxWidth: 900, margin: '0 auto' }}>
      <h1 className="sl-section-title">Settings</h1>
      <hr className="sl-divider" />
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--sl-space-sm)', marginBottom: 'var(--sl-space-lg)' }}>
        {tabs
          .filter(t => !t.admin || session.is_admin)
          .map(t => (
            <button
              key={t.id}
              className="sl-button"
              style={tab === t.id ? undefined : { opacity: 0.55 }}
              onClick={() => setTab(t.id)}
            >
              {t.label}
            </button>
          ))}
      </div>
      {tab === 'server' && <ServerSettings serverUrl={serverUrl} />}
      {tab === 'account' && <AccountSettings serverUrl={serverUrl} session={session} />}
      {tab === 'appearance' && <AppearanceSettings />}
      {tab === 'subtitles' && <SubtitleSettings />}
      {tab === 'playback' && <PlaybackSettings />}
      {tab === 'discord' && <DiscordSettings />}
      {tab === 'admin' && session.is_admin && <AdminSettings serverUrl={serverUrl} />}
      {tab === 'about' && <AboutSettings serverUrl={serverUrl} />}
    </div>
  );
}
