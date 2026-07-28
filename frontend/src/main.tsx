import { HashRouter } from 'react-router-dom';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { isDesktop } from './desktop';
import './index.css';

// Skip service worker in desktop builds to avoid caching issues with Tauri custom protocol
if (!isDesktop() && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}

const root = document.getElementById('root')!;

// Render with error catching at the root level
try {
  createRoot(root).render(
    <StrictMode>
      <HashRouter>
        <App />
      </HashRouter>
    </StrictMode>,
  );
} catch (e) {
  root.innerHTML = `<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;background:#000;color:#fff;padding:2rem;text-align:center;gap:1rem;font-family:sans-serif">
    <h2>Failed to start</h2>
    <p style="color:#b3b3b3;font-size:0.9rem;max-width:400px">${e instanceof Error ? e.message : 'Unknown error'}</p>
    <button onclick="location.reload()" style="background:#e50914;color:white;border:none;padding:0.75rem 1.5rem;border-radius:8px;font-weight:700;cursor:pointer">Reload</button>
  </div>`;
}
