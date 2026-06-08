import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input: React.FC<InputProps> = ({ label, error, style, ...props }) => {
  return (
    <div style={{ width: '100%', marginBottom: 'var(--spacing-md)' }}>
      {label && (
        <label style={{ 
          display: 'block', 
          fontSize: '0.85rem', 
          color: 'var(--text-secondary)', 
          marginBottom: 'var(--spacing-xs)',
          fontWeight: 600
        }}>
          {label}
        </label>
      )}
      <input
        style={{
          width: '100%',
          backgroundColor: 'var(--surface-variant)',
          border: '1px solid transparent',
          borderRadius: 'var(--radius-md)',
          padding: '0.9rem 1.2rem',
          color: 'var(--text-primary)',
          fontSize: '1rem',
          outline: 'none',
          transition: 'var(--transition-standard)',
          ...style
        }}
        onFocus={(e) => {
          e.currentTarget.style.borderColor = 'var(--primary-color)';
          e.currentTarget.style.backgroundColor = 'var(--surface-color)';
        }}
        onBlur={(e) => {
          e.currentTarget.style.borderColor = 'transparent';
          e.currentTarget.style.backgroundColor = 'var(--surface-variant)';
        }}
        {...props}
      />
      {error && (
        <span style={{ 
          fontSize: '0.8rem', 
          color: 'var(--primary-color)', 
          marginTop: 'var(--spacing-xs)',
          display: 'block'
        }}>
          {error}
        </span>
      )}
    </div>
  );
};
