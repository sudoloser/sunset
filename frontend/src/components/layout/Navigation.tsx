import React from 'react';
import { HomeIcon, LibraryIcon, SettingsIcon } from '../common/Icons';

interface NavigationProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  isAdmin?: boolean;
}

export const Navigation: React.FC<NavigationProps> = ({ activeTab, onTabChange, isAdmin }) => {
  const navItems = [
    { id: 'home', label: 'Home', icon: HomeIcon },
    { id: 'libraries', label: 'Libraries', icon: LibraryIcon },
  ];

  if (isAdmin) {
    navItems.push({ id: 'settings', label: 'Settings', icon: SettingsIcon });
  }

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

        <button 
          onClick={handleLogout}
          style={{ 
            background: 'transparent', 
            border: '1px solid var(--border-color)', 
            color: 'var(--text-secondary)',
            padding: '0.5rem 1rem',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.8rem',
            fontWeight: 600,
            cursor: 'pointer'
          }}
        >
          LOGOUT
        </button>
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
          display: 'none', // Shown via CSS media query
          alignItems: 'center',
          justifyContent: 'space-around',
          zIndex: 1000,
          paddingBottom: 'env(safe-area-inset-bottom)'
        }}
        className="mobile-nav"
      >
        {navItems.map((item) => (
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
