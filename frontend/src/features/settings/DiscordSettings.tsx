import React, { useState, useEffect } from 'react';
import { Card } from '../../components/common/Card';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { api } from '../../api/client';

export const DiscordSettings: React.FC = () => {
  const [token, setToken] = useState('');
  const [status, setStatus] = useState('online');
  const [loading, setLoading] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const userId = localStorage.getItem('sunset_user_id');

  useEffect(() => {
    if (userId) {
      api.getUserProfile(userId).then(profile => {
        if (profile?.discord_token) setToken(profile.discord_token);
        if (profile?.discord_status) setStatus(profile.discord_status);
      });
    }
  }, [userId]);

  const handleSave = async () => {
    if (!userId) return;
    setLoading(true);
    try {
      await api.updateDiscordConfig(userId, token, status);
      setSaveStatus('success');
      setTimeout(() => setSaveStatus('idle'), 3000);
    } catch (e) {
      setSaveStatus('error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '600px' }}>
      <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '3rem' }}>Discord RPC</h2>
      
      <Card style={{ backgroundColor: 'var(--surface-color)' }}>
        <h3 style={{ fontSize: '1.4rem', marginBottom: '1.5rem' }}>Rich Presence</h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '2rem' }}>
          Sync your "Watching SunSet" status to Discord. You'll need your Discord User Token (not a bot token). 
          Keep this token private!
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <Input 
            label="Discord User Token"
            type="password"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="PASTE_TOKEN_HERE"
          />

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
            <Button onClick={handleSave} disabled={!token || loading}>
              {loading ? 'Saving...' : (saveStatus === 'success' ? '✓ Saved' : 'Save Config')}
            </Button>
            {saveStatus === 'error' && <span style={{ color: '#ef4444', fontSize: '0.8rem' }}>Failed to save config</span>}
          </div>
        </div>
      </Card>

      <Card style={{ marginTop: '2rem', backgroundColor: 'var(--surface-variant)', border: '1px dashed var(--border-color)' }}>
        <h4 style={{ marginBottom: '0.5rem' }}>How to find your token?</h4>
        <ol style={{ paddingLeft: '1.2rem', fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          <li>Open Discord in your browser and log in.</li>
          <li>Press <strong>Ctrl+Shift+I</strong> to open Developer Tools.</li>
          <li>Go to the <strong>Network</strong> tab and type <code>/api</code> in the filter.</li>
          <li>Refresh the page or click a channel.</li>
          <li>Click on an entry like <code>science</code> or <code>messages</code>.</li>
          <li>Find the <code>authorization</code> header in the request headers — that's your token.</li>
        </ol>
      </Card>
    </div>
  );
};
