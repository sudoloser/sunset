import React, { useEffect, useState } from 'react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { api } from '../../api/client';
import type { Library } from '../../types';

export const Admin: React.FC = () => {
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [uptime, setUptime] = useState(0);
  const [showAdd, setShowAdd] = useState(false);
  const [newLib, setNewLib] = useState<Omit<Library, 'id'>>({ name: '', path: '', lib_type: 'movies' });

  useEffect(() => {
    api.getLibraries().then(setLibraries);
    const interval = setInterval(() => {
      api.getUptime().then(setUptime);
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  const handleAdd = async () => {
    await api.addLibrary(newLib);
    setNewLib({ name: '', path: '', lib_type: 'movies' });
    setShowAdd(false);
    api.getLibraries().then(setLibraries);
  };

  const handleDelete = async (id: string) => {
    if (confirm('Delete this library and all its indexed items?')) {
      await api.deleteLibrary(id);
      api.getLibraries().then(setLibraries);
    }
  };

  return (
    <div style={{ maxWidth: '1000px', paddingBottom: 'var(--spacing-xxl)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem' }}>
        <h2 style={{ fontSize: '2.5rem', fontWeight: 800 }}>Admin</h2>
        <Button size="lg" onClick={() => api.triggerScan()}>Trigger Full Scan</Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '2rem' }}>
        <Card style={{ backgroundColor: 'var(--surface-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
            <h3 style={{ fontSize: '1.4rem' }}>Libraries</h3>
            {!showAdd && <Button variant="secondary" size="sm" onClick={() => setShowAdd(true)}>+ Add New</Button>}
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
              <div style={{ fontSize: '1.2rem', fontWeight: 700 }}>SunSet v0.1.0-expressive</div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};
