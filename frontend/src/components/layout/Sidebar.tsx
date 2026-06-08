import React from 'react';

interface SidebarProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onTabChange }) => {
  const navItems = [
    { id: 'home', label: 'Home', icon: '🏠' },
    { id: 'movies', label: 'Movies', icon: '🎬' },
    { id: 'shows', label: 'TV Shows', icon: '📺' },
    { id: 'admin', label: 'Admin', icon: '⚙️' },
  ];

  return (
    <aside 
      className="glass"
      style={{
        width: 'var(--sidebar-width)',
        height: '100vh',
        position: 'fixed',
        top: 0,
        left: 0,
        borderRight: '1px solid var(--border-color)',
        padding: '2rem 1rem',
        display: 'flex',
        flexDirection: 'column',
        zIndex: 100,
        borderRadius: 0, // Fill the side
        borderTop: 'none',
        borderBottom: 'none',
        borderLeft: 'none'
      }}
    >
      <div style={{ marginBottom: '3rem', paddingLeft: '1rem' }}>
        <h1 style={{ fontSize: '1.2rem', color: 'var(--primary-color)' }}>SunSet</h1>
      </div>

      <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-md)',
              border: 'none',
              background: activeTab === item.id ? 'rgba(108, 142, 255, 0.1)' : 'transparent',
              color: activeTab === item.id ? 'var(--primary-color)' : 'var(--text-secondary)',
              cursor: 'pointer',
              fontSize: '0.95rem',
              fontWeight: activeTab === item.id ? 600 : 400,
              transition: 'var(--transition-fast)',
              textAlign: 'left'
            }}
          >
            <span style={{ fontSize: '1.2rem' }}>{item.icon}</span>
            {item.label}
          </button>
        ))}
      </nav>
    </aside>
  );
};
