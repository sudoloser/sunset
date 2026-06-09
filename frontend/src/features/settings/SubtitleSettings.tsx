import React, { useState, useEffect } from 'react';
import { Card } from '../../components/common/Card';

const DEFAULT_SUBTITLE_SETTINGS = {
  color: '#ffffff',
  backgroundOpacity: 0,
  size: 100,
  font: 'Inter, sans-serif',
  bold: false,
};

export type SubtitleSettingsType = typeof DEFAULT_SUBTITLE_SETTINGS;

const FONT_OPTIONS = [
  { label: 'Inter', value: 'Inter, sans-serif' },
  { label: 'Google Sans', value: '"Google Sans", sans-serif' },
  { label: 'Arial', value: 'Arial, sans-serif' },
  { label: 'Helvetica', value: 'Helvetica, sans-serif' },
  { label: 'Verdana', value: 'Verdana, sans-serif' },
  { label: 'Times New Roman', value: '"Times New Roman", serif' },
  { label: 'Courier New', value: '"Courier New", monospace' },
];

export function loadSubtitleSettings(): SubtitleSettingsType {
  try {
    const saved = localStorage.getItem('sunset_subtitle_settings');
    if (saved) return { ...DEFAULT_SUBTITLE_SETTINGS, ...JSON.parse(saved) };
  } catch {}
  return { ...DEFAULT_SUBTITLE_SETTINGS };
}

export function saveSubtitleSettings(settings: SubtitleSettingsType) {
  localStorage.setItem('sunset_subtitle_settings', JSON.stringify(settings));
}

export const SubtitleSettings: React.FC = () => {
  const [settings, setSettings] = useState<SubtitleSettingsType>(loadSubtitleSettings);

  useEffect(() => {
    saveSubtitleSettings(settings);
  }, [settings]);

  const update = (partial: Partial<SubtitleSettingsType>) => {
    setSettings(prev => ({ ...prev, ...partial }));
  };

  return (
    <div style={{ maxWidth: '800px', paddingBottom: 'var(--spacing-xxl)' }}>
      <h2 style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: '3rem' }}>Subtitles</h2>

      <Card style={{ backgroundColor: 'var(--surface-color)', marginBottom: '2rem' }}>
        <h3 style={{ fontSize: '1.4rem', marginBottom: '2rem' }}>Style</h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Color */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600 }}>
              Text Color
            </label>
            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
              {['#ffffff', '#ffff00', '#00ff00', '#00ffff', '#ff9900', '#ff66cc', '#ff0000', '#cccccc'].map(c => (
                <button
                  key={c}
                  onClick={() => update({ color: c })}
                  style={{
                    width: '36px', height: '36px', borderRadius: '50%', border: settings.color === c ? '3px solid white' : '2px solid transparent',
                    backgroundColor: c, cursor: 'pointer', transition: 'var(--transition-standard)'
                  }}
                />
              ))}
              <input
                type="color"
                value={settings.color}
                onChange={e => update({ color: e.target.value })}
                style={{ width: '40px', height: '40px', border: 'none', background: 'transparent', cursor: 'pointer' }}
              />
            </div>
          </div>

          {/* Background / Closed Caption */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600 }}>
              Background
            </label>
            <div style={{ display: 'flex', gap: '1rem' }}>
              {[
                { label: 'None', value: 0 },
                { label: 'Black Rectangle (CC)', value: 0.85 },
              ].map(opt => (
                <button
                  key={opt.label}
                  onClick={() => update({ backgroundOpacity: opt.value })}
                  style={{
                    padding: '0.6rem 1.2rem', borderRadius: 'var(--radius-md)',
                    background: settings.backgroundOpacity === opt.value ? 'var(--primary-color)' : 'var(--surface-variant)',
                    border: 'none', color: 'white', cursor: 'pointer', fontWeight: 600, fontSize: '0.9rem'
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Size */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600 }}>
              Size: {settings.size}%
            </label>
            <input
              type="range" min="50" max="200" step="5"
              value={settings.size}
              onChange={e => update({ size: parseInt(e.target.value) })}
              style={{ width: '100%', maxWidth: '300px', accentColor: 'var(--primary-color)' }}
            />
          </div>

          {/* Font */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontWeight: 600 }}>
              Font
            </label>
            <select
              value={settings.font}
              onChange={e => update({ font: e.target.value })}
              style={{
                width: '100%', maxWidth: '300px', padding: '0.7rem 1rem', borderRadius: 'var(--radius-md)',
                background: 'var(--surface-variant)', border: 'none', color: 'white', outline: 'none', fontSize: '0.95rem'
              }}
            >
              {FONT_OPTIONS.map(f => (
                <option key={f.value} value={f.value}>{f.label}</option>
              ))}
            </select>
          </div>

          {/* Bold */}
          <div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={settings.bold}
                onChange={e => update({ bold: e.target.checked })}
                style={{ width: '20px', height: '20px', accentColor: 'var(--primary-color)' }}
              />
              <span style={{ fontSize: '0.95rem', fontWeight: 600 }}>Bold text</span>
            </label>
          </div>
        </div>
      </Card>

      {/* Preview */}
      <Card style={{ backgroundColor: 'var(--surface-color)' }}>
        <h3 style={{ fontSize: '1.4rem', marginBottom: '1.5rem' }}>Preview</h3>
        <div style={{
          position: 'relative',
          width: '100%', aspectRatio: '16/9',
          backgroundColor: '#111',
          borderRadius: 'var(--radius-md)',
          overflow: 'hidden',
          display: 'flex', alignItems: 'flex-end', justifyContent: 'center',
          paddingBottom: '15%',
        }}>
          <div style={{
            color: settings.color,
            fontFamily: settings.font,
            fontSize: `${settings.size}%`,
            fontWeight: settings.bold ? 700 : 400,
            backgroundColor: settings.backgroundOpacity > 0 ? `rgba(0,0,0,${settings.backgroundOpacity})` : 'transparent',
            padding: '0.4rem 1rem',
            borderRadius: '2px',
            lineHeight: 1.5,
            textAlign: 'center',
            maxWidth: '80%',
          }}>
            This is a preview of your subtitle style.
          </div>
        </div>
      </Card>
    </div>
  );
};
