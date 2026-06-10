import React, { useState, useEffect } from 'react';
import { Card } from '../../components/common/Card';

type Theme = 'dark' | 'light';

const THEME_KEY = 'sunset_theme';

function loadTheme(): Theme {
  return (localStorage.getItem(THEME_KEY) as Theme) || 'dark';
}

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  if (theme === 'light') {
    root.style.setProperty('--bg-color', '#f5f5f7');
    root.style.setProperty('--surface-color', '#ffffff');
    root.style.setProperty('--surface-variant', '#e8e8ed');
    root.style.setProperty('--text-primary', '#1d1d1f');
    root.style.setProperty('--text-secondary', '#6e6e73');
    root.style.setProperty('--border-color', '#d2d2d7');
  } else {
    root.style.setProperty('--bg-color', '#000000');
    root.style.setProperty('--surface-color', '#121212');
    root.style.setProperty('--surface-variant', '#222222');
    root.style.setProperty('--text-primary', '#ffffff');
    root.style.setProperty('--text-secondary', '#B3B3B3');
    root.style.setProperty('--border-color', '#333333');
  }
}

export const AppearanceSettings: React.FC = () => {
  const [theme, setTheme] = useState<Theme>(loadTheme);
  const [useNativePlayer, setUseNativePlayer] = useState<boolean>(() => {
    return localStorage.getItem('sunset_use_native_player') === 'true';
  });

  const isAndroid = !!(window as any).SunSetAndroid;

  useEffect(() => {
    localStorage.setItem(THEME_KEY, theme);
    applyTheme(theme);
  }, [theme]);

  useEffect(() => {
    localStorage.setItem('sunset_use_native_player', useNativePlayer.toString());
  }, [useNativePlayer]);

  return (
    <div style={{ maxWidth: '600px', paddingBottom: 'var(--spacing-xxl)' }}>
      <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '3rem' }}>Appearance</h2>
      
      <Card style={{ backgroundColor: 'var(--surface-color)', marginBottom: '2rem' }}>
        <h3 style={{ fontSize: '1.4rem', marginBottom: '1.5rem' }}>Theme</h3>
        <div style={{ display: 'flex', gap: '1rem' }}>
          {[
            { id: 'dark' as Theme, label: 'Dark', icon: '🌙' },
            { id: 'light' as Theme, label: 'Light', icon: '☀️' },
          ].map(opt => (
            <button
              key={opt.id}
              onClick={() => setTheme(opt.id)}
              style={{
                flex: 1, padding: '2rem 1rem', borderRadius: 'var(--radius-lg)',
                background: theme === opt.id ? 'var(--primary-color)' : 'var(--surface-variant)',
                border: theme === opt.id ? '2px solid white' : '2px solid transparent',
                color: 'white', cursor: 'pointer', textAlign: 'center', transition: 'var(--transition-standard)'
              }}
            >
              <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>{opt.icon}</div>
              <div style={{ fontWeight: 700 }}>{opt.label}</div>
            </button>
          ))}
        </div>
      </Card>

      {isAndroid && (
        <Card style={{ backgroundColor: 'var(--surface-color)' }}>
          <h3 style={{ fontSize: '1.4rem', marginBottom: '0.5rem' }}>Android Settings</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
            Experimental features for the native Android application.
          </p>
          
          <div 
            onClick={() => setUseNativePlayer(!useNativePlayer)}
            style={{ 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'space-between',
              padding: '1rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--surface-variant)',
              cursor: 'pointer'
            }}
          >
            <div>
              <div style={{ fontWeight: 700 }}>Native ExoPlayer</div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Use native player for better performance and PiP</div>
            </div>
            <div style={{
              width: '48px', height: '24px', borderRadius: '12px',
              backgroundColor: useNativePlayer ? 'var(--primary-color)' : '#444',
              position: 'relative', transition: 'background 0.3s'
            }}>
              <div style={{
                position: 'absolute', top: '2px', left: useNativePlayer ? '26px' : '2px',
                width: '20px', height: '20px', borderRadius: '10px', backgroundColor: 'white',
                transition: 'left 0.3s'
              }} />
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};
