import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg' | 'icon';
  fullWidth?: boolean;
}

export const Button: React.FC<ButtonProps> = ({ 
  children, 
  variant = 'primary', 
  size = 'md', 
  fullWidth = false,
  className = '',
  style,
  ...props 
}) => {
  const baseStyles: React.CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
    borderRadius: size === 'lg' ? 'var(--radius-xl)' : 'var(--radius-md)',
    cursor: 'pointer',
    transition: 'var(--transition-standard)',
    border: 'none',
    width: fullWidth ? '100%' : 'auto',
    gap: 'var(--spacing-sm)',
    ...style
  };

  const variants: Record<string, React.CSSProperties> = {
    primary: {
      backgroundColor: 'var(--primary-color)',
      color: 'white',
    },
    secondary: {
      backgroundColor: 'rgba(255, 255, 255, 0.15)',
      color: 'white',
      backdropFilter: 'blur(10px)',
    },
    ghost: {
      backgroundColor: 'transparent',
      color: 'var(--text-primary)',
    },
    danger: {
      backgroundColor: '#ef4444',
      color: 'white',
    }
  };

  const sizes: Record<string, React.CSSProperties> = {
    sm: { padding: '0.5rem 1rem', fontSize: '0.85rem' },
    md: { padding: '0.75rem 1.5rem', fontSize: '1rem' },
    lg: { padding: '1rem 2rem', fontSize: '1.2rem', borderRadius: 'var(--radius-xl)' },
    icon: { padding: '0.5rem', borderRadius: '50%' },
  };

  return (
    <button 
      style={{ ...baseStyles, ...variants[variant], ...sizes[size] }}
      className={`btn-${variant} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
};
