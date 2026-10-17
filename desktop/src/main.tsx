import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import App from './App';
import './theme/tokens.css';
import './theme/app.css';

// A pre-rewrite build shipped the PWA frontend, and its service worker has
// been living in this machine's webview profile ever since, serving a stale
// document whose assets no longer exist (a blank window). The desktop app
// has no use for service workers: unregister any survivor and clear caches.
// (Windows profiles are also cleaned natively before the webview starts.)
if ('serviceWorker' in navigator) {
  navigator.serviceWorker
    .getRegistrations()
    .then((regs) => regs.forEach((reg) => { void reg.unregister(); }))
    .catch(() => undefined);
}
if ('caches' in window) {
  caches
    .keys()
    .then((keys) => keys.forEach((key) => { void caches.delete(key); }))
    .catch(() => undefined);
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <HashRouter>
      <App />
    </HashRouter>
  </StrictMode>,
);

// The native window is created hidden (visible: false) so nothing flashes
// while the webview boots. Reveal it from here — after the first frame —
// instead of at page-load time: showing the window mid-load leaves
// WebView2 (Windows) stuck on a blank white surface. The timeout covers
// rAF throttling while the native window is still hidden; the Rust side
// has a last-resort reveal in case this code never runs.
let revealed = false;
function revealMainWindow() {
  if (revealed) return;
  revealed = true;
  import('@tauri-apps/api/core')
    .then(({ invoke }) => invoke('show_main_window'))
    .catch(() => undefined);
}
requestAnimationFrame(() => requestAnimationFrame(revealMainWindow));
setTimeout(revealMainWindow, 300);
