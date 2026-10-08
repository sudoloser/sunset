import React, { useEffect, useState } from 'react';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { api } from '../../api/client';
import type { Library } from '../../types';

const selectStyle: React.CSSProperties = {
  width: '100%', padding: '0.9rem 1.2rem', borderRadius: 'var(--radius-md)',
  background: 'rgba(255, 255, 255, 0.05)', border: 'none', color: 'white', outline: 'none',
};

const fileInputStyle: React.CSSProperties = {
  width: '100%', padding: '0.9rem 1.2rem', borderRadius: 'var(--radius-md)',
  background: 'rgba(255, 255, 255, 0.05)', border: 'none', color: 'white', outline: 'none',
};

export const UploadMovie: React.FC = () => {
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [libraryId, setLibraryId] = useState('');
  const [name, setName] = useState('');
  const [year, setYear] = useState('');
  const [video, setVideo] = useState<File | null>(null);
  const [subtitle, setSubtitle] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);

  useEffect(() => {
    api.getLibraries()
      .then(libs => {
        const movies = libs.filter(l => l.lib_type === 'movies');
        setLibraries(movies);
        if (movies.length > 0) setLibraryId(movies[0].id);
      })
      .catch(() => {});
  }, []);

  const resetForm = () => {
    setName('');
    setYear('');
    setVideo(null);
    setSubtitle(null);
  };

  const handleUpload = async () => {
    const trimmedName = name.trim();
    const trimmedYear = year.trim();
    if (!libraryId) {
      setMessage({ ok: false, text: 'No movies library found. Add one first.' });
      return;
    }
    if (!trimmedName) {
      setMessage({ ok: false, text: 'Enter the movie name.' });
      return;
    }
    if (!/^\d{4}$/.test(trimmedYear)) {
      setMessage({ ok: false, text: 'Enter a 4-digit year.' });
      return;
    }
    if (!video) {
      setMessage({ ok: false, text: 'Pick a movie file to upload.' });
      return;
    }

    const form = new FormData();
    form.append('title', trimmedName);
    form.append('year', trimmedYear);
    form.append('video', video);
    if (subtitle) form.append('subtitle', subtitle);

    setUploading(true);
    setProgress(0);
    setMessage(null);
    try {
      const res = await api.uploadMovie(libraryId, form, (loaded, total) => {
        setProgress(Math.round((loaded / total) * 100));
      });
      setMessage({ ok: true, text: `"${res.title}" uploaded. It will appear once indexing finishes.` });
      resetForm();
    } catch (e) {
      setMessage({ ok: false, text: e instanceof Error ? e.message : 'Upload failed.' });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <h3 style={{ fontSize: '1.4rem', marginBottom: '0.5rem' }}>Upload Movie</h3>
      <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
        Saved as <code>Name (Year)</code> inside the target library, then indexed automatically.
      </p>

      {libraries.length > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 'var(--spacing-xs)', fontWeight: 600 }}>Library</label>
          <select value={libraryId} onChange={e => setLibraryId(e.target.value)} style={selectStyle} disabled={uploading}>
            {libraries.map(l => (
              <option key={l.id} value={l.id}>{l.name}</option>
            ))}
          </select>
        </div>
      )}

      <Input label="Movie name" placeholder="e.g. Dune" value={name} onChange={e => setName(e.target.value)} disabled={uploading} />
      <Input label="Year" placeholder="e.g. 2021" value={year} onChange={e => setYear(e.target.value.replace(/\D/g, '').slice(0, 4))} inputMode="numeric" disabled={uploading} />

      <div style={{ marginBottom: 'var(--spacing-md)' }}>
        <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 'var(--spacing-xs)', fontWeight: 600 }}>
          Movie file {video && <span style={{ color: 'var(--text-primary)' }}>({video.name})</span>}
        </label>
        <input
          type="file"
          accept="video/*,.mkv,.mk3d,.avi,.wmv,.flv,.ts,.m2ts,.mts,.mpg,.mpeg,.3gp,.ogv"
          onChange={e => setVideo(e.target.files?.[0] ?? null)}
          disabled={uploading}
          style={fileInputStyle}
        />
      </div>

      <div style={{ marginBottom: '1.5rem' }}>
        <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 'var(--spacing-xs)', fontWeight: 600 }}>
          Subtitle file (optional) {subtitle && <span style={{ color: 'var(--text-primary)' }}>({subtitle.name})</span>}
        </label>
        <input
          type="file"
          accept=".srt,.vtt"
          onChange={e => setSubtitle(e.target.files?.[0] ?? null)}
          disabled={uploading}
          style={fileInputStyle}
        />
      </div>

      {uploading && (
        <div style={{ marginBottom: '1.5rem' }}>
          <div style={{ height: '8px', borderRadius: '4px', background: 'rgba(255,255,255,0.1)', overflow: 'hidden' }}>
            <div style={{ width: `${progress}%`, height: '100%', background: 'var(--primary-color)', transition: 'width 0.2s' }} />
          </div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.4rem' }}>{progress}% uploaded</div>
        </div>
      )}

      {message && (
        <div style={{
          fontSize: '0.9rem', marginBottom: '1.5rem', padding: '0.75rem 1rem',
          borderRadius: 'var(--radius-md)',
          background: message.ok ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
          color: message.ok ? '#4ade80' : '#f87171',
        }}>
          {message.text}
        </div>
      )}

      <Button onClick={handleUpload} disabled={uploading} fullWidth>
        {uploading ? 'Uploading...' : 'Upload Movie'}
      </Button>
    </div>
  );
};
