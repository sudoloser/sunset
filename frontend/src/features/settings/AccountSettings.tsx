import React, { useState, useRef } from 'react';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { api } from '../../api/client';

interface AccountSettingsProps {
  userId: string;
  currentUsername: string;
  isAdmin: boolean;
}

export const AccountSettings: React.FC<AccountSettingsProps> = ({ userId, currentUsername, isAdmin }) => {
  const [username, setUsername] = useState(currentUsername);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [passwordMsg, setPasswordMsg] = useState('');
  const [usernameMsg, setUsernameMsg] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  const handleLogout = () => {
    localStorage.removeItem('sunset_user_id');
    window.location.reload();
  };

  const handleChangePassword = async () => {
    if (!currentPassword || !newPassword) return;
    try {
      const ok = await api.changePassword(userId, currentPassword, newPassword);
      setPasswordMsg(ok ? 'Password changed.' : 'Current password is incorrect.');
      if (ok) { setCurrentPassword(''); setNewPassword(''); }
    } catch { setPasswordMsg('Failed to change password.'); }
  };

  const handleChangeUsername = async () => {
    if (!username || username === currentUsername) return;
    try {
      await api.changeUsername(userId, username);
      setUsernameMsg('Username updated. (Will apply on next login)');
    } catch { setUsernameMsg('Failed to change username.'); }
  };

  const handleProfilePicture = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onloadend = async () => {
      const dataUrl = reader.result as string;
      try {
        await api.uploadProfilePicture(userId, dataUrl);
        window.location.reload();
      } catch {}
    };
    reader.readAsDataURL(file);
  };

  return (
    <div style={{ maxWidth: '500px', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h3 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Profile Picture</h3>
        <input ref={fileRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleProfilePicture} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <img
            src={api.getProfilePictureUrl(userId)}
            alt="profile"
            style={{ width: '72px', height: '72px', borderRadius: '50%', objectFit: 'cover', background: 'var(--surface-variant)' }}
            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
          <Button variant="secondary" onClick={() => fileRef.current?.click()}>Upload Photo</Button>
        </div>
      </div>

      <div>
        <h3 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Username</h3>
        <Input value={username} onChange={e => setUsername(e.target.value)} />
        <div style={{ marginTop: '0.5rem' }}><Button size="sm" onClick={handleChangeUsername}>Save</Button></div>
        {usernameMsg && <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.5rem' }}>{usernameMsg}</p>}
      </div>

      <div>
        <h3 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Password</h3>
        <Input label="Current Password" type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} />
        <Input label="New Password" type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} />
        <div style={{ marginTop: '0.5rem' }}><Button size="sm" onClick={handleChangePassword}>Change Password</Button></div>
        {passwordMsg && <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.5rem' }}>{passwordMsg}</p>}
      </div>

      <div>
        <Button variant="ghost" onClick={handleLogout} style={{ color: '#ef4444', fontWeight: 700, fontSize: '0.9rem' }}>
          Log Out
        </Button>
      </div>
    </div>
  );
};
