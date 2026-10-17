import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import App from './App';
import './theme/tokens.css';
import './theme/app.css';

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
