import React, { useState, useRef, useEffect, useCallback } from 'react';
import { HomeIcon, LibraryIcon, SettingsIcon } from '../common/Icons';
import { api } from '../../api/client';
import { isDesktop } from '../../desktop';

interface NavigationProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

function useWindowControls() {
  const [maximized, setMaximized] = useState(false);

  useEffect(() => {
    if (!isDesktop()) return;
    import('@tauri-apps/api/window').then(({ getCurrentWindow }) => {
      const w = getCurrentWindow();
      w.isMaximized().then(setMaximized);
      const unlisten = w.onResized(() => w.isMaximized().then(setMaximized));
      return () => { unlisten.then(fn => fn()); };
    });
  }, []);

  const minimize = useCallback(() => {
    if (!isDesktop()) return;
    import('@tauri-apps/api/window').then(({ getCurrentWindow }) => getCurrentWindow().minimize());
  }, []);

  const toggleMaximize = useCallback(() => {
    if (!isDesktop()) return;
    import('@tauri-apps/api/window').then(({ getCurrentWindow }) => {
      getCurrentWindow().toggleMaximize();
      setMaximized(v => !v);
    });
  }, []);

  const close = useCallback(() => {
    if (!isDesktop()) return;
    import('@tauri-apps/api/window').then(({ getCurrentWindow }) => getCurrentWindow().close());
  }, []);

  return { maximized, minimize, toggleMaximize, close };
}

const WindowControls: React.FC = () => {
  const { maximized, minimize, toggleMaximize, close } = useWindowControls();

  return (
    <div style={{ display: 'flex', alignItems: 'center', marginLeft: 'var(--spacing-md)' }}>
      <button onClick={minimize} title="Minimize" style={controlBtnStyle}>
        <svg width="12" height="12" viewBox="0 0 12 12"><rect x="1" y="5.5" width="10" height="1" fill="currentColor"/></svg>
      </button>
      <button onClick={toggleMaximize} title={maximized ? 'Restore' : 'Maximize'} style={controlBtnStyle}>
        {maximized ? (
          <svg width="12" height="12" viewBox="0 0 12 12">
            <rect x="2.5" y="0.5" width="9" height="9" rx="1" fill="none" stroke="currentColor" strokeWidth="1"/>
            <rect x="0.5" y="2.5" width="9" height="9" rx="1" fill="var(--bg-color)" stroke="currentColor" strokeWidth="1"/>
          </svg>
        ) : (
          <svg width="12" height="12" viewBox="0 0 12 12"><rect x="1" y="1" width="10" height="10" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.2"/></svg>
        )}
      </button>
      <button onClick={close} title="Close" style={{ ...controlBtnStyle, marginRight: 0 }}>
        <svg width="12" height="12" viewBox="0 0 12 12">
          <line x1="1" y1="1" x2="11" y2="11" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
          <line x1="11" y1="1" x2="1" y2="11" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
        </svg>
      </button>
    </div>
  );
};

const controlBtnStyle: React.CSSProperties = {
  background: 'transparent',
  border: 'none',
  color: 'var(--text-secondary)',
  cursor: 'pointer',
  width: '36px',
  height: '36px',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  borderRadius: '6px',
  transition: 'background 0.15s',
};

export const Navigation: React.FC<NavigationProps> = ({ activeTab, onTabChange }) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const userId = localStorage.getItem('sunset_user_id') || '';

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const navItems = [
    { id: 'home', label: 'Home', icon: HomeIcon },
    { id: 'libraries', label: 'My Library', icon: LibraryIcon },
  ];

  const handleLogout = () => {
    localStorage.removeItem('sunset_user_id');
    window.location.reload();
  };

  return (
    <>
      {/* Desktop Top Navigation */}
      <nav
        data-tauri-drag-region
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          height: 'var(--header-height)',
          boxSizing: 'border-box',
          padding: 'var(--safe-area-top) var(--spacing-xl) 0',
          backgroundColor: 'rgba(0, 0, 0, 0.3)',
          WebkitBackdropFilter: 'blur(16px)',
          backdropFilter: 'blur(16px)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
          zIndex: 1000,
          transition: 'var(--transition-standard)'
        }}
        className="desktop-nav"
      >
        <div style={{ display: 'flex', alignItems: 'center' }} data-tauri-drag-region>
          <div style={{ color: 'var(--primary-color)', fontSize: '1.8rem', fontWeight: 800, marginRight: 'var(--spacing-xxl)', letterSpacing: '-0.05em', fontFamily: 'var(--font-display)' }}>
            SUNSET
          </div>
          <div style={{ display: 'flex', gap: 'var(--spacing-lg)' }}>
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: activeTab === item.id ? 'white' : 'var(--text-secondary)',
                  fontSize: '0.95rem',
                  fontWeight: activeTab === item.id ? 700 : 500,
                  cursor: 'pointer',
                  transition: 'var(--transition-standard)'
                }}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-md)' }}>
          {isDesktop() && <WindowControls />}
          <div style={{ position: 'relative' }} ref={menuRef}>
            <button
              onClick={() => setMenuOpen(!menuOpen)}
              style={{
                background: 'transparent', border: 'none', cursor: 'pointer', padding: 0,
                display: 'flex', alignItems: 'center', gap: '0.5rem'
              }}
            >
              <img
                src={api.getProfilePictureUrl(userId)}
                alt="profile"
                style={{ width: '36px', height: '36px', borderRadius: '50%', objectFit: 'cover', background: 'var(--surface-variant)' }}
                onError={e => {
                  const el = e.target as HTMLImageElement;
                  el.style.display = 'none';
                  (el.nextElementSibling as HTMLElement)?.style.removeProperty('display');
                }}
              />
              <span style={{
                width: '36px', height: '36px', borderRadius: '50%', display: 'none',
                background: 'var(--primary-color)', color: 'white',
                alignItems: 'center', justifyContent: 'center',
                fontSize: '0.85rem', fontWeight: 700
              }}>
                {localStorage.getItem('sunset_username')?.[0]?.toUpperCase() || 'U'}
              </span>
            </button>

            {menuOpen && (
              <div style={{
                position: 'absolute', top: 'calc(100% + 8px)', right: 0, zIndex: 100,
                background: 'var(--surface-color)', border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)', boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
                minWidth: '180px', overflow: 'hidden'
              }}>
                <div style={{ padding: '0.75rem 1rem', borderBottom: '1px solid var(--border-color)' }}>
                  <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>{localStorage.getItem('sunset_username') || 'User'}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{localStorage.getItem('sunset_is_admin') === 'true' ? 'Admin' : 'User'}</div>
                </div>
                <div
                  onClick={() => { onTabChange('settings'); setMenuOpen(false); }}
                  style={{
                    display: 'flex', alignItems: 'center', gap: '0.75rem',
                    padding: '0.75rem 1rem', cursor: 'pointer', fontSize: '0.9rem',
                    transition: 'background 0.15s'
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  <SettingsIcon size={16} />
                  <span>Settings</span>
                </div>
                <div
                  onClick={handleLogout}
                  style={{
                    display: 'flex', alignItems: 'center', gap: '0.75rem',
                    padding: '0.75rem 1rem', cursor: 'pointer', fontSize: '0.9rem', color: '#ef4444',
                    transition: 'background 0.15s'
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
                  </svg>
                  <span>Log Out</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* Mobile Bottom Navigation */}
      <nav 
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          height: 'calc(var(--bottom-nav-height) + var(--safe-area-bottom))',
          boxSizing: 'border-box',
          paddingBottom: 'var(--safe-area-bottom)',
          backgroundColor: 'rgba(18, 18, 18, 0.9)',
          WebkitBackdropFilter: 'blur(12px)',
          backdropFilter: 'blur(12px)',
          borderTop: '1px solid var(--border-color)',
          display: 'none',
          alignItems: 'center',
          justifyContent: 'space-around',
          zIndex: 1000
        }}
        className="mobile-nav"
      >
        {[...navItems, { id: 'settings', label: 'Settings', icon: SettingsIcon }].map((item) => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            style={{
              background: 'transparent',
              border: 'none',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '4px',
              color: activeTab === item.id ? 'white' : 'var(--text-secondary)',
              cursor: 'pointer'
            }}
          >
            <item.icon size={22} strokeWidth={activeTab === item.id ? 2.5 : 2} />
            <span style={{ fontSize: '0.7rem', fontWeight: 600 }}>{item.label}</span>
          </button>
        ))}
      </nav>

      <style>{`
        @media (max-width: 768px) {
          .desktop-nav { display: none !important; }
          .mobile-nav { display: flex !important; }
        }
      `}</style>
    </>
  );
};
