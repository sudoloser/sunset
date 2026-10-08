import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import type { Session } from '../../session';

export function AccountSettings({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const [username, setUsername] = useState(session.username);
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [msg, setMsg] = useState('');
  const [avatarTick, setAvatarTick] = useState(0);

  useEffect(() => {
    setUsername(session.username);
  }, [session.username]);

  const flash = (m: string) => {
    setMsg(m);
    setTimeout(() => setMsg(''), 3000);
  };

  return (
    <div style={{ maxWidth: 520 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Account</h3>
      <img
        src={`${api.profilePictureUrl(serverUrl, session.user_id)}?t=${avatarTick}`}
        alt=""
        style={{ width: 72, height: 72, borderRadius: 'var(--sl-radius-pill)', border: '2px solid var(--sl-text)', objectFit: 'cover', background: 'var(--sl-bg-soft)' }}
        onError={e => {
          (e.target as HTMLImageElement).style.display = 'none';
        }}
      />
      <div style={{ marginTop: 'var(--sl-space-md)' }}>
        <label style={{ fontWeight: 700 }}>Avatar</label>
        <input
          type="file"
          accept="image/*"
          onChange={e => {
            const file = e.target.files?.[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = async () => {
              try {
                await api.uploadProfilePicture(serverUrl, session.user_id, String(reader.result));
                setAvatarTick(t => t + 1);
                flash('Avatar updated.');
              } catch {
                flash('Avatar upload failed.');
              }
            };
            reader.readAsDataURL(file);
          }}
        />
      </div>
      <div style={{ marginTop: 'var(--sl-space-md)' }}>
        <label style={{ fontWeight: 700 }}>Username</label>
        <div style={{ display: 'flex', gap: 'var(--sl-space-sm)' }}>
          <input className="sl-input" value={username} onChange={e => setUsername(e.target.value)} />
          <button
            className="sl-button"
            onClick={() =>
              void api
                .changeUsername(serverUrl, session.user_id, username.trim())
                .then(ok => flash(ok ? 'Username saved. It applies on next login.' : 'Could not save.'))
                .catch(() => flash('Could not save.'))
            }
          >
            Save
          </button>
        </div>
      </div>
      <div style={{ marginTop: 'var(--sl-space-md)' }}>
        <label style={{ fontWeight: 700 }}>Change password</label>
        <input
          className="sl-input"
          type="password"
          placeholder="Current password"
          value={currentPw}
          onChange={e => setCurrentPw(e.target.value)}
        />
        <input
          className="sl-input"
          style={{ marginTop: 'var(--sl-space-sm)' }}
          type="password"
          placeholder="New password"
          value={newPw}
          onChange={e => setNewPw(e.target.value)}
        />
        <button
          className="sl-button"
          style={{ marginTop: 'var(--sl-space-sm)' }}
          onClick={() =>
            void api
              .changePassword(serverUrl, session.user_id, currentPw, newPw)
              .then(ok => {
                flash(ok ? 'Password changed.' : 'Current password was wrong.');
                if (ok) {
                  setCurrentPw('');
                  setNewPw('');
                }
              })
              .catch(() => flash('Could not change password.'))
          }
        >
          Change password
        </button>
      </div>
      {msg && <p style={{ fontSize: 'var(--sl-text-sm)' }}>{msg}</p>}
    </div>
  );
}
