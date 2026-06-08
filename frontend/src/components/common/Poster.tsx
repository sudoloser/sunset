import React from 'react';

interface PosterProps {
  itemId?: string;
  title: string;
  subtitle?: string;
  onClick?: () => void;
  className?: string;
}

export const Poster: React.FC<PosterProps> = ({ itemId, title, subtitle, onClick }) => {
  const posterUrl = itemId ? `/api/media/${itemId}/asset/folder.jpg` : null;

  return (
    <div 
      onClick={onClick}
      style={{
        minWidth: '160px',
        width: '160px',
        aspectRatio: '2/3',
        backgroundColor: 'var(--surface-variant)',
        backgroundImage: posterUrl ? `url(${posterUrl})` : 'none',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        borderRadius: 'var(--radius-md)',
        cursor: 'pointer',
        overflow: 'hidden',
        position: 'relative',
        transition: 'var(--transition-standard)',
        transformOrigin: 'center',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'flex-end',
        padding: '1rem',
        boxShadow: '0 4px 10px rgba(0,0,0,0.5)'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'scale(1.05)';
        e.currentTarget.style.zIndex = '10';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'scale(1)';
        e.currentTarget.style.zIndex = '1';
      }}
    >
      {/* Background Gradient for readability */}
      <div style={{
        position: 'absolute',
        inset: 0,
        background: 'linear-gradient(to top, rgba(0,0,0,0.9) 0%, transparent 60%)',
        zIndex: 1
      }} />

      <div style={{ position: 'relative', zIndex: 2 }}>
        <div style={{ 
          fontWeight: 700, 
          fontSize: '0.9rem', 
          lineHeight: '1.2',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden'
        }}>
          {title}
        </div>
        {subtitle && (
          <div style={{ 
            fontSize: '0.75rem', 
            color: 'var(--text-secondary)', 
            marginTop: '0.2rem' 
          }}>
            {subtitle}
          </div>
        )}
      </div>
    </div>
  );
};
