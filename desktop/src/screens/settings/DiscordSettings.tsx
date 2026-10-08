import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import type { Session } from '../../session';
import { startRpc, stopRpc } from '../../discord';

export function DiscordSettings({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const [token, setToken] = useState('');
  const [status, setStatus] = useState('online');
  const [clientId, setClientId] = useState('');
  const [nativeOn, setNativeOn] = useState(false);
  const [msg, setMsg] = useState('');

  useEffect(() => {
    api
      .getUserProfile(serverUrl, session.user_id)
      .then(p => {
        if (p.discord_token) setToken(p.discord_token);
        if (p.discord_status) setStatus(p.discord_status);
      })
      .catch(() => {});
  }, [serverUrl, session.user_id]);

  const flash = (m: string) => {
    setMsg(m);
    setTimeout(() => setMsg(''), 3000);
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Discord</h3>
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
        Server-driven presence uses your user token (kept on the server). Native presence talks to
        Discord directly from this app and needs an application Client ID plus the Discord client running.
      </p>
      <label style={{ fontWeight: 700 }}>Discord user token</label>
      <input
        className="sl-input"
        type="password"
        value={token}
        onChange={e => setToken(e.target.value)}
        placeholder="PASTE_TOKEN_HERE"
      />
      <label style={{ fontWeight: 700, display: 'block', marginTop: 'var(--sl-space-sm)' }}>Status</label>
      <select className="sl-input" value={status} onChange={e => setStatus(e.target.value)}>
        <option value="online">Online</option>
        <option value="idle">Idle</option>
        <option value="dnd">Do Not Disturb</option>
        <option value="invisible">Invisible</option>
      </select>
      <div style={{ display: 'flex', gap: 'var(--sl-space-sm)', marginTop: 'var(--sl-space-sm)' }}>
        <button
          className="sl-button"
          onClick={() =>
            void api
              .updateDiscordConfig(serverUrl, session.user_id, token, status)
              .then(ok => flash(ok ? 'Presence saved.' : 'Save failed.'))
              .catch(() => flash('Save failed.'))
          }
        >
          Save presence
        </button>
        <button
          className="sl-button"
          style={{ opacity: 0.7 }}
          onClick={() =>
            void api
              .stopDiscordRpc(serverUrl, session.user_id)
              .then(() => flash('Presence stopped.'))
              .catch(() => flash('Stop failed.'))
          }
        >
          Stop
        </button>
      </div>

      <h3 style={{ fontSize: 'var(--sl-text-lg)', marginTop: 'var(--sl-space-lg)' }}>Native RPC</h3>
      <label style={{ fontWeight: 700 }}>Application Client ID</label>
      <div style={{ display: 'flex', gap: 'var(--sl-space-sm)' }}>
        <input
          className="sl-input"
          value={clientId}
          onChange={e => setClientId(e.target.value)}
          placeholder="1234567890"
        />
        {nativeOn ? (
          <button
            className="sl-button"
            onClick={() =>
              void stopRpc()
                .then(() => setNativeOn(false))
                .catch(() => flash('Disconnect failed.'))
            }
          >
            Disconnect
          </button>
        ) : (
          <button
            className="sl-button"
            disabled={!clientId.trim()}
            onClick={() =>
              void startRpc(clientId.trim())
                .then(() => setNativeOn(true))
                .catch(() => flash('Connect failed. Is Discord running?'))
            }
          >
            Connect
          </button>
        )}
      </div>
      {msg && <p style={{ fontSize: 'var(--sl-text-sm)' }}>{msg}</p>}
    </div>
  );
}
