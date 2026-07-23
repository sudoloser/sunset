import React, { useState, useEffect } from 'react';
import { Card } from '../../components/common/Card';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { api } from '../../api/client';
import { isDesktop, connectDiscord, disconnectDiscord } from '../../desktop';
import { getImgurClientId, setImgurClientId, clearCoverCache } from '../../coverArt';

interface DiscordSettingsProps {
  sudoloserMode?: boolean;
}

export const DiscordSettings: React.FC<DiscordSettingsProps> = ({ sudoloserMode = false }) => {
  const desktop = isDesktop();

  const [discordClientId, setDiscordClientId] = useState('');
  const [imgurClientId, setLocalImgurClientId] = useState('');
  const [token, setToken] = useState('');
  const [status, setStatus] = useState('online');
  const [coverUrl, setCoverUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const userId = localStorage.getItem('sunset_user_id');

  useEffect(() => {
    setLocalImgurClientId(getImgurClientId() || '');
    if (userId && !desktop) {
      api.getUserProfile(userId).then(profile => {
        if (profile?.discord_token) setToken(profile.discord_token);
        if (profile?.discord_status) setStatus(profile.discord_status);
        if (profile?.discord_cover_url) setCoverUrl(profile.discord_cover_url);
      });
    }
  }, [userId, desktop]);

  const handleConnect = async () => {
    setLoading(true);
    try {
      const ok = desktop
        ? await connectDiscord(discordClientId)
        : await connectDiscord(token);
      if (ok) {
        setConnected(true);
        setSaveStatus('success');
      } else {
        setSaveStatus('error');
      }
      setTimeout(() => setSaveStatus('idle'), 3000);
    } catch {
      setSaveStatus('error');
    } finally {
      setLoading(false);
    }
  };

  const handleDisconnect = async () => {
    try {
      await disconnectDiscord();
      setConnected(false);
      setSaveStatus('success');
      setTimeout(() => setSaveStatus('idle'), 3000);
    } catch {
      setSaveStatus('error');
    }
  };

  const handleImgurSave = () => {
    setImgurClientId(imgurClientId);
    setSaveStatus('success');
    setTimeout(() => setSaveStatus('idle'), 3000);
  };

  return (
    <div style={{ maxWidth: '600px' }}>
      <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '3rem' }}>Discord RPC</h2>

      <Card style={{ backgroundColor: 'var(--surface-color)' }}>
        <h3 style={{ fontSize: '1.4rem', marginBottom: '1.5rem' }}>Rich Presence</h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '2rem' }}>
          {desktop
            ? 'Show what you\'re watching on Discord via native Rich Presence. Requires Discord to be running.'
            : 'Sync your "Watching SunSet" status to Discord. You\'ll need your Discord User Token (not a bot token). Keep this token private!'}
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {desktop ? (
            <Input
              label="Discord Application Client ID"
              value={discordClientId}
              onChange={(e) => setDiscordClientId(e.target.value)}
              placeholder="YOUR_CLIENT_ID"
            />
          ) : (
            <Input
              label="Discord User Token"
              type="password"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="PASTE_TOKEN_HERE"
            />
          )}

          {sudoloserMode && (
            <Input
              label="Cover Art Base URL"
              value={coverUrl}
              onChange={(e) => setCoverUrl(e.target.value)}
              placeholder="https://cdn.qzz.io/public/media"
            />
          )}

          {/* Imgur Client ID for automatic cover art upload */}
          <Input
            label="Imgur Client ID (auto-upload cover art)"
            value={imgurClientId}
            onChange={(e) => setLocalImgurClientId(e.target.value)}
            placeholder="YOUR_IMGUR_CLIENT_ID"
          />
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginTop: '-0.5rem' }}>
            <Button onClick={handleImgurSave} variant="secondary" size="sm">
              Save Imgur Key
            </Button>
            <Button onClick={clearCoverCache} variant="ghost" size="sm">
              Clear Cache
            </Button>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Posters upload to Imgur once, then cached locally
            </span>
          </div>

          <div style={{ marginBottom: '1.5rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 'var(--spacing-xs)', fontWeight: 600 }}>
              Presence Status
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              style={{
                width: '100%',
                padding: '0.9rem 1.2rem',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(255, 255, 255, 0.05)',
                border: 'none',
                color: 'white',
                outline: 'none',
                fontSize: '1rem'
              }}
            >
              <option value="online">Online</option>
              <option value="dnd">Do Not Disturb</option>
              <option value="idle">Idle</option>
              <option value="invisible">Invisible</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            {connected ? (
              <Button onClick={handleDisconnect} variant="danger">
                Disconnect
              </Button>
            ) : (
              <Button onClick={handleConnect} disabled={loading}>
                {loading ? 'Connecting...' : (saveStatus === 'success' ? '✓ Connected' : 'Connect')}
              </Button>
            )}
            {saveStatus === 'error' && <span style={{ color: '#ef4444', fontSize: '0.8rem' }}>Failed to connect</span>}
          </div>
        </div>
      </Card>

      <Card style={{ marginTop: '2rem', backgroundColor: 'var(--surface-variant)', border: '1px dashed var(--border-color)' }}>
        <h4 style={{ marginBottom: '0.5rem' }}>
          {desktop ? 'How to get your Client ID?' : 'How to find your token?'}
        </h4>
        {desktop ? (
          <ol style={{ paddingLeft: '1.2rem', fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <li>Go to the <strong>Discord Developer Portal</strong> (discord.com/developers/applications).</li>
            <li>Create a new application and give it a name.</li>
            <li>Copy the <strong>Application ID</strong> (Client ID) and paste it above.</li>
            <li>Make sure Discord desktop is running on your machine.</li>
          </ol>
        ) : (
          <ol style={{ paddingLeft: '1.2rem', fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <li>Open Discord in your browser and log in.</li>
            <li>Press <strong>Ctrl+Shift+I</strong> to open Developer Tools.</li>
            <li>Go to the <strong>Network</strong> tab and type <code>/api</code> in the filter.</li>
            <li>Refresh the page or click a channel.</li>
            <li>Click on an entry like <code>science</code> or <code>messages</code>.</li>
            <li>Find the <code>authorization</code> header in the request headers — that's your token.</li>
          </ol>
        )}
      </Card>

      <Card style={{ marginTop: '2rem', backgroundColor: 'var(--surface-variant)', border: '1px dashed var(--border-color)' }}>
        <h4 style={{ marginBottom: '0.5rem' }}>Cover Art &amp; Imgur</h4>
        <ol style={{ paddingLeft: '1.2rem', fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          <li>Go to <strong>imgur.com</strong> and sign in (or create an account).</li>
          <li>Go to <a href="https://api.imgur.com/oauth2/addclient" style={{ color: 'var(--primary-color)' }}>api.imgur.com/oauth2/addclient</a>.</li>
          <li>Select <strong>Anonymous usage without authorization</strong> — set a name and paste the Client ID above.</li>
          <li>When you play media, the poster uploads to Imgur once and is cached for the session.</li>
          <li>Requires "Use External Assets" enabled in your Discord Application's Rich Presence settings.</li>
        </ol>
      </Card>
    </div>
  );
};
