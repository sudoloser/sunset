import React, { useState } from 'react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { api } from '../../api/client';
import type { OnboardRequest, Library } from '../../types';

interface OnboardingWizardProps {
  onComplete: () => void;
}

export const OnboardingWizard: React.FC<OnboardingWizardProps> = ({ onComplete }) => {
  const [step, setStep] = useState(0);
  const [serverName, setServerName] = useState('SunSet Media');
  const [admin, setAdmin] = useState({ username: '', password: '' });
  const [rememberMe, setRememberMe] = useState(true);
  const [libraries, setLibraries] = useState<Omit<Library, 'id'>[]>([]);
  const [newLib, setNewLib] = useState<Omit<Library, 'id'>>({ name: '', path: '', lib_type: 'movies' });

  const handleNext = () => setStep(s => s + 1);
  const handleBack = () => setStep(s => s - 1);

  const addLibrary = () => {
    if (newLib.name && newLib.path) {
      setLibraries([...libraries, newLib]);
      setNewLib({ name: '', path: '', lib_type: 'movies' });
    }
  };

  const finalize = async () => {
    const payload: OnboardRequest = {
      server_name: serverName,
      admin_user: { username: admin.username, password_hash: admin.password },
      libraries
    };
    await api.onboard(payload);
    
    // Auto login after onboard if rememberMe is set
    const data = await api.login({ username: admin.username, password_hash: admin.password });
    if (data && rememberMe) {
      localStorage.setItem('sunset_user_id', data.user_id);
      localStorage.setItem('sunset_is_admin', data.is_admin ? 'true' : 'false');
    }
    
    onComplete();
  };

  return (
    <div style={{ 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      minHeight: '100vh',
      padding: '2rem'
    }}>
      <Card style={{ width: '100%', maxWidth: '480px' }} glass>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>SunSet Setup</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            {step === 0 && 'Give your media server a name.'}
            {step === 1 && 'Create your administrator account.'}
            {step === 2 && 'Add your first media libraries.'}
          </p>
        </div>

        {step === 0 && (
          <div>
            <Input 
              label="Server Name" 
              value={serverName} 
              onChange={e => setServerName(e.target.value)} 
            />
            <Button fullWidth onClick={handleNext} style={{ marginTop: '1rem' }}>Next</Button>
          </div>
        )}

        {step === 1 && (
          <div>
            <Input 
              label="Admin Username" 
              value={admin.username} 
              onChange={e => setAdmin({ ...admin, username: e.target.value })} 
            />
            <Input 
              label="Password" 
              type="password"
              value={admin.password} 
              onChange={e => setAdmin({ ...admin, password: e.target.value })} 
            />
            <div style={{ marginBottom: '1.5rem' }}>
              <label className="checkbox-container">
                <input 
                  type="checkbox" 
                  checked={rememberMe} 
                  onChange={e => setRememberMe(e.target.checked)} 
                />
                Remember me
              </label>
            </div>
            <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
              <Button variant="secondary" onClick={handleBack} style={{ flex: 1 }}>Back</Button>
              <Button onClick={handleNext} style={{ flex: 1 }}>Next</Button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div>
            <div style={{ marginBottom: '1.5rem' }}>
              {libraries.map((lib, i) => (
                <div key={i} className="glass" style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', marginBottom: '0.5rem', fontSize: '0.9rem' }}>
                  <strong>{lib.name}</strong> <small>({lib.lib_type})</small>
                </div>
              ))}
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1.5rem' }}>
              <Input placeholder="Library Name (e.g. Movies)" value={newLib.name} onChange={e => setNewLib({...newLib, name: e.target.value})} />
              <Input placeholder="Path (e.g. /data/movies)" value={newLib.path} onChange={e => setNewLib({...newLib, path: e.target.value})} />
              <select 
                value={newLib.lib_type} 
                onChange={e => setNewLib({...newLib, lib_type: e.target.value as any})}
                style={{
                  padding: '0.7rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid var(--border-color)',
                  color: 'white',
                  outline: 'none'
                }}
              >
                <option value="movies">Movies</option>
                <option value="shows">TV Shows</option>
              </select>
              <Button variant="secondary" onClick={addLibrary}>Add Library</Button>
            </div>

            <div style={{ display: 'flex', gap: '1rem' }}>
              <Button variant="secondary" onClick={handleBack} style={{ flex: 1 }}>Back</Button>
              <Button onClick={finalize} style={{ flex: 1 }}>Finish Setup</Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};
