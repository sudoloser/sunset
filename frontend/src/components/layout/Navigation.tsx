import React, { useState, useRef, useEffect } from 'react';
import { HomeIcon, LibraryIcon, SettingsIcon } from '../common/Icons';
import { api } from '../../api/client';

interface NavigationProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

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
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          height: 'var(--header-height)',
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          backdropFilter: 'blur(20px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 var(--spacing-xl)',
          zIndex: 1000,
          transition: 'var(--transition-standard)'
        }}
        className="desktop-nav"
      >
        <div style={{ display: 'flex', alignItems: 'center' }}>
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
      </nav>

      {/* Mobile Bottom Navigation */}
      <nav 
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          height: 'var(--bottom-nav-height)',
          backgroundColor: '#121212',
          borderTop: '1px solid var(--border-color)',
          display: 'none',
          alignItems: 'center',
          justifyContent: 'space-around',
          zIndex: 1000,
          paddingBottom: 'env(safe-area-inset-bottom)'
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
