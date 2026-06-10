import React, { useState } from 'react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { api } from '../../api/client';

interface LoginFormProps {
  serverName: string;
  onLogin: (userId: string, isAdmin: boolean, username: string) => void;
}

export const LoginForm: React.FC<LoginFormProps> = ({ serverName, onLogin }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = await api.login({ username, password_hash: password });
      if (data) {
        if (rememberMe) {
          localStorage.setItem('sunset_user_id', data.user_id);
          localStorage.setItem('sunset_is_admin', data.is_admin ? 'true' : 'false');
          localStorage.setItem('sunset_username', data.username);
        }
        onLogin(data.user_id, data.is_admin, data.username);
      } else {
        setError('Invalid credentials');
      }
    } catch (err) {
      setError('Login failed. Please try again.');
    }
  };

  return (
    <div style={{ 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      minHeight: '100vh',
      padding: '2rem'
    }}>
      <Card style={{ width: '100%', maxWidth: '400px' }} glass>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem', letterSpacing: '-0.03em' }}>{serverName}</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Welcome back.</p>
        </div>

        <form onSubmit={handleLogin}>
          <Input 
            label="Username" 
            value={username} 
            onChange={e => setUsername(e.target.value)} 
          />
          <Input 
            label="Password" 
            type="password"
            value={password} 
            onChange={e => setPassword(e.target.value)} 
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

          {error && (
            <p style={{ color: 'var(--primary-color)', fontSize: '0.8rem', marginBottom: '1rem', textAlign: 'center' }}>
              {error}
            </p>
          )}
          <Button fullWidth type="submit" style={{ marginTop: '0.5rem' }}>Login</Button>
        </form>
      </Card>
    </div>
  );
};
