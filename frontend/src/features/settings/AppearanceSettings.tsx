import React, { useState, useEffect } from 'react';
import { Card } from '../../components/common/Card';

type ThemeMode = 'system' | 'dark' | 'light';

const THEME_KEY = 'sunset_theme';

function loadThemeMode(): ThemeMode {
  return (localStorage.getItem(THEME_KEY) as ThemeMode) || 'system';
}

function getSystemTheme(): 'dark' | 'light' {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function resolveTheme(mode: ThemeMode): 'dark' | 'light' {
  return mode === 'system' ? getSystemTheme() : mode;
}

function applyTheme(mode: ThemeMode) {
  const theme = resolveTheme(mode);
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
  const [themeMode, setThemeMode] = useState<ThemeMode>(loadThemeMode);
  const [useNativePlayer, setUseNativePlayer] = useState<boolean>(() => {
    return localStorage.getItem('sunset_use_native_player') === 'true';
  });

  const isAndroid = !!(window as any).SunSetAndroid;
  const [systemDark, setSystemDark] = useState(getSystemTheme() === 'dark');

  // Listen for system theme changes
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = (e: MediaQueryListEvent) => setSystemDark(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  // Apply theme whenever mode or system preference changes
  useEffect(() => {
    localStorage.setItem(THEME_KEY, themeMode);
    applyTheme(themeMode);
  }, [themeMode, systemDark]);

  useEffect(() => {
    localStorage.setItem('sunset_use_native_player', useNativePlayer.toString());
  }, [useNativePlayer]);

  return (
    <div style={{ maxWidth: '600px', paddingBottom: 'var(--spacing-xxl)' }}>
      <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '3rem' }}>Appearance</h2>
      
      <Card style={{ backgroundColor: 'var(--surface-color)', marginBottom: '2rem' }}>
        <h3 style={{ fontSize: '1.4rem', marginBottom: '1.5rem' }}>Theme</h3>
        <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600 }}>
          Theme Mode
        </label>
        <div style={{ position: 'relative' }}>
          <select
            value={themeMode}
            onChange={e => setThemeMode(e.target.value as ThemeMode)}
            style={{
              width: '100%', padding: '0.75rem 1rem', borderRadius: 'var(--radius-md)',
              background: 'var(--surface-variant)', border: '1px solid var(--border-color)',
              color: 'var(--text-primary)', fontSize: '0.95rem', fontWeight: 500,
              cursor: 'pointer', outline: 'none', appearance: 'none',
              WebkitAppearance: 'none', MozAppearance: 'none',
              transition: 'var(--transition-standard)'
            }}
          >
            <option value="system">System — follow device theme</option>
            <option value="dark">Dark — always dark mode</option>
            <option value="light">Light — always light mode</option>
          </select>
          <div style={{
            position: 'absolute', right: '1rem', top: '50%', transform: 'translateY(-50%)',
            pointerEvents: 'none', color: 'var(--text-secondary)', fontSize: '0.8rem'
          }}>
            ▼
          </div>
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
