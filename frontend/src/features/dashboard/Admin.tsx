import React, { useEffect, useState, useCallback } from 'react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { api } from '../../api/client';
import type { Library, StorageInfo } from '../../types';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

export const Admin: React.FC = () => {
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [uptime, setUptime] = useState(0);
  const [showAdd, setShowAdd] = useState(false);
  const [newLib, setNewLib] = useState<Omit<Library, 'id'>>({ name: '', path: '', lib_type: 'movies' });
  const [storage, setStorage] = useState<StorageInfo | null>(null);
  const [inviteCode, setInviteCode] = useState('');
  const [inviteCopied, setInviteCopied] = useState(false);

  const loadData = useCallback(async () => {
    const [libs, storageData] = await Promise.all([
      api.getLibraries(),
      api.getStorage().catch(() => null),
    ]);
    setLibraries(libs);
    setStorage(storageData);
  }, []);

  useEffect(() => {
    loadData();
    const interval = setInterval(() => {
      api.getUptime().then(setUptime);
    }, 1000);
    return () => clearInterval(interval);
  }, [loadData]);

  const handleAdd = async () => {
    await api.addLibrary(newLib);
    setNewLib({ name: '', path: '', lib_type: 'movies' });
    setShowAdd(false);
    loadData();
  };

  const handleDelete = async (id: string) => {
    if (confirm('Delete this library and all its indexed items?')) {
      await api.deleteLibrary(id);
      loadData();
    }
  };

  const handleCreateInvite = async () => {
    try {
      const code = await api.createInvite();
      setInviteCode(code);
      setInviteCopied(false);
    } catch {}
  };

  const copyInvite = () => {
    navigator.clipboard.writeText(inviteCode);
    setInviteCopied(true);
    setTimeout(() => setInviteCopied(false), 2000);
  };

  return (
    <div style={{ maxWidth: '1000px', paddingBottom: 'var(--spacing-xxl)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '2.5rem', fontWeight: 800 }}>Admin</h2>
        <Button size="lg" onClick={() => api.triggerScan()}>Scan All</Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '2rem' }}>
        <Card style={{ backgroundColor: 'var(--surface-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
            <h3 style={{ fontSize: '1.4rem' }}>Libraries</h3>
            {!showAdd && <Button variant="secondary" size="sm" onClick={() => setShowAdd(true)}>+ Add</Button>}
          </div>

          {showAdd && (
            <div style={{ marginBottom: '2rem', padding: '1.5rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-lg)', backgroundColor: 'var(--surface-variant)' }}>
              <Input label="Library Name" value={newLib.name} onChange={e => setNewLib({...newLib, name: e.target.value})} />
              <Input label="Directory Path" value={newLib.path} onChange={e => setNewLib({...newLib, path: e.target.value})} />
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 'var(--spacing-xs)', fontWeight: 600 }}>Type</label>
                <select 
                  value={newLib.lib_type} 
                  onChange={e => setNewLib({...newLib, lib_type: e.target.value as any})}
                  style={{
                    width: '100%', padding: '0.9rem 1.2rem', borderRadius: 'var(--radius-md)', 
                    background: 'rgba(255, 255, 255, 0.05)', border: 'none', color: 'white', outline: 'none'
                  }}
                >
                  <option value="movies">Movies</option>
                  <option value="shows">TV Shows</option>
                </select>
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <Button onClick={handleAdd} style={{ flex: 1 }}>Save</Button>
                <Button variant="ghost" onClick={() => setShowAdd(false)} style={{ flex: 1 }}>Cancel</Button>
              </div>
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {libraries.map(lib => (
              <div key={lib.id} style={{ padding: '1.2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--surface-variant)' }}>
                <div>
                  <div style={{ fontWeight: 700 }}>{lib.name}</div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{lib.path}</div>
                </div>
                <Button variant="ghost" onClick={() => handleDelete(lib.id)} style={{ color: '#ef4444', fontWeight: 700 }}>Remove</Button>
              </div>
            ))}
          </div>
        </Card>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          <Card style={{ backgroundColor: 'var(--surface-color)' }}>
            <h3 style={{ fontSize: '1.4rem', marginBottom: '2rem' }}>Server Status</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, marginBottom: '0.4rem' }}>UPTIME</div>
                <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--primary-color)' }}>
                  {Math.floor(uptime / 3600)}h {Math.floor((uptime % 3600) / 60)}m {uptime % 60}s
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600, marginBottom: '0.4rem' }}>VERSION</div>
                <div style={{ fontSize: '1.2rem', fontWeight: 700 }}>SunSet v0.1.0</div>
              </div>
            </div>
          </Card>

          {storage && (
            <Card style={{ backgroundColor: 'var(--surface-color)' }}>
              <h3 style={{ fontSize: '1.4rem', marginBottom: '1.5rem' }}>Storage</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>ITEMS</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>{storage.item_count}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>LIBRARIES</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>{storage.library_count}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>USERS</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>{storage.user_count}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>TOTAL SIZE</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>{formatBytes(storage.total_size)}</div>
                </div>
              </div>
            </Card>
          )}

          <Card style={{ backgroundColor: 'var(--surface-color)' }}>
            <h3 style={{ fontSize: '1.4rem', marginBottom: '1.5rem' }}>Invite Codes</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              Generate a code that new users can use to register.
            </p>
            <Button onClick={handleCreateInvite} variant="secondary" style={{ marginBottom: '1rem' }}>Generate Code</Button>
            {inviteCode && (
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <code style={{ 
                  background: 'var(--surface-variant)', padding: '0.5rem 1rem', borderRadius: 'var(--radius-md)',
                  fontSize: '1.2rem', fontWeight: 700, letterSpacing: '0.1em', flex: 1
                }}>
                  {inviteCode}
                </code>
                <Button size="sm" onClick={copyInvite}>
                  {inviteCopied ? 'Copied!' : 'Copy'}
                </Button>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
};
