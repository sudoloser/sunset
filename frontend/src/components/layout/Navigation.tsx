import React, { useState, useRef, useEffect } from 'react';
import { HomeIcon, LibraryIcon, SettingsIcon } from '../common/Icons';
import { api } from '../../api/client';
import styles from './Navigation.module.css';

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
      <nav className={styles.nav}>
        <div className={styles.left}>
          <span className={styles.brand}>SUNSET</span>
          <div style={{ display: 'flex', gap: 'var(--spacing-lg)' }}>
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                className={activeTab === item.id ? styles.navLinkActive : styles.navLinkInactive}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        <div className={styles.right}>
          <div style={{ position: 'relative' }} ref={menuRef}>
            <button className={styles.profileBtn} onClick={() => setMenuOpen(!menuOpen)}>
              <img
                src={api.getProfilePictureUrl(userId)}
                alt="profile"
                className={styles.avatar}
                onError={e => {
                  const el = e.target as HTMLImageElement;
                  el.style.display = 'none';
                  (el.nextElementSibling as HTMLElement)?.style.removeProperty('display');
                }}
              />
              <span className={styles.avatarFallback}>
                {localStorage.getItem('sunset_username')?.[0]?.toUpperCase() || 'U'}
              </span>
            </button>

            {menuOpen && (
              <div className={styles.menu}>
                <div className={styles.menuHeader}>
                  <div className={styles.menuUserName}>{localStorage.getItem('sunset_username') || 'User'}</div>
                  <div className={styles.menuUserRole}>{localStorage.getItem('sunset_is_admin') === 'true' ? 'Admin' : 'User'}</div>
                </div>
                <div
                  className={styles.menuItem}
                  onClick={() => { onTabChange('settings'); setMenuOpen(false); }}
                  onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-variant)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  <SettingsIcon size={16} />
                  <span>Settings</span>
                </div>
                <div
                  className={styles.menuItemDanger}
                  onClick={handleLogout}
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

      <nav className={styles.mobileNav}>
        {[...navItems, { id: 'settings', label: 'Settings', icon: SettingsIcon }].map((item) => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            className={styles.mobileNavBtn}
            style={{ color: activeTab === item.id ? 'white' : 'var(--text-secondary)' }}
          >
            <item.icon size={22} strokeWidth={activeTab === item.id ? 2.5 : 2} />
            <span className={styles.mobileNavLabel} style={{ color: activeTab === item.id ? 'white' : 'var(--text-secondary)' }}>
              {item.label}
            </span>
          </button>
        ))}
      </nav>
    </>
  );
};
