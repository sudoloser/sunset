import { useEffect, useState } from 'react';
import { loadPref, savePref } from '../../prefs';
import { startRpc, stopRpc } from '../../discord';
import { clearCoverCache } from '../../coverArt';

// Desktop drives presence natively and never arms the server-side pipeline
// (the player reports is_playing=false), so this is the only presence
// switch. The server token flow still exists for web clients.
export function DiscordSettings() {
  const [clientId, setClientId] = useState('');
  const [autoConnect, setAutoConnect] = useState(false);
  const [imgurClientId, setImgurClientId] = useState('');
  const [nativeOn, setNativeOn] = useState(false);
  const [msg, setMsg] = useState('');

  useEffect(() => {
    void loadPref('sunset_prefs_discord').then(p => {
      setClientId(p.clientId);
      setAutoConnect(p.autoConnect);
      setImgurClientId(p.imgurClientId ?? '');
    });
  }, []);

  const persist = (patch: { clientId?: string; autoConnect?: boolean; imgurClientId?: string }) => {
    const next = {
      clientId: patch.clientId ?? clientId,
      autoConnect: patch.autoConnect ?? autoConnect,
      imgurClientId: patch.imgurClientId ?? imgurClientId,
    };
    setClientId(next.clientId);
    setAutoConnect(next.autoConnect);
    setImgurClientId(next.imgurClientId);
    void savePref('sunset_prefs_discord', next);
  };

  const flash = (m: string) => {
    setMsg(m);
    setTimeout(() => setMsg(''), 3000);
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Discord</h3>
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
        Native Rich Presence, straight from this app to Discord. Needs an application Client ID
        and the Discord client running. The server pipeline stays off for desktop plays.
      </p>
      <label style={{ fontWeight: 700 }}>Application Client ID</label>
      <div style={{ display: 'flex', gap: 'var(--sl-space-sm)' }}>
        <input
          className="sl-input"
          value={clientId}
          onChange={e => persist({ clientId: e.target.value })}
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
      <label style={{ display: 'flex', gap: 'var(--sl-space-sm)', alignItems: 'center', marginTop: 'var(--sl-space-sm)' }}>
        <input
          type="checkbox"
          checked={autoConnect}
          onChange={e => persist({ autoConnect: e.target.checked })}
        />
        Connect automatically on launch
      </label>

      <h3 style={{ fontSize: 'var(--sl-text-lg)', marginTop: 'var(--sl-space-lg)' }}>Cover art</h3>
      <p style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>
        Posters upload to Imgur once each, then the hosted URL becomes the presence cover.
        Requires an Imgur Client ID (anonymous usage) and "Use External Assets" enabled on
        your Discord application.
      </p>
      <label style={{ fontWeight: 700 }}>Imgur Client ID</label>
      <div style={{ display: 'flex', gap: 'var(--sl-space-sm)' }}>
        <input
          className="sl-input"
          value={imgurClientId}
          onChange={e => persist({ imgurClientId: e.target.value })}
          placeholder="YOUR_IMGUR_CLIENT_ID"
        />
        <button
          className="sl-button"
          style={{ opacity: 0.7 }}
          onClick={() => {
            clearCoverCache();
            flash('Cover cache cleared. Posters re-upload on next play.');
          }}
        >
          Clear cache
        </button>
      </div>
      {msg && <p style={{ fontSize: 'var(--sl-text-sm)' }}>{msg}</p>}
    </div>
  );
}
