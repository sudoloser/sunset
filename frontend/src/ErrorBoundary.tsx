import React from 'react';

interface Props {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

const fallbackStyle: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
  minHeight: '100vh', background: '#000', color: '#fff',
  padding: '2rem', textAlign: 'center', gap: '1rem', fontFamily: 'system-ui, sans-serif',
};

export class ErrorBoundary extends React.Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <div style={fallbackStyle}>
          <h2 style={{ margin: 0 }}>Something went wrong</h2>
          <p style={{ color: '#b3b3b3', fontSize: '0.9rem', maxWidth: '400px', margin: 0 }}>
            {this.state.error?.message || 'An unexpected error occurred'}
          </p>
          <button
            onClick={() => { this.setState({ hasError: false, error: undefined }); window.location.reload(); }}
            style={{
              background: '#e50914', color: 'white', border: 'none',
              padding: '0.75rem 1.5rem', borderRadius: '8px',
              fontWeight: 700, cursor: 'pointer', fontSize: '0.9rem',
            }}
          >
            Reload
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
