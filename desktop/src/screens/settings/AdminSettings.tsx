import { useEffect, useState } from 'react';
import { api, type Library, type User } from '../../api/client';

export function AdminSettings({ serverUrl }: { serverUrl: string }) {
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [invite, setInvite] = useState('');
  const [msg, setMsg] = useState('');
  const [newLib, setNewLib] = useState({ name: '', path: '', lib_type: 'movies' as 'movies' | 'shows' });
  const [newUser, setNewUser] = useState({ username: '', password_hash: '', is_admin: false });
  const [upName, setUpName] = useState('');
  const [upYear, setUpYear] = useState('');
  const [upLib, setUpLib] = useState('');
  const [upVideo, setUpVideo] = useState<File | null>(null);
  const [upSub, setUpSub] = useState<File | null>(null);
  const [upProgress, setUpProgress] = useState(0);
  const [uploading, setUploading] = useState(false);

  const reload = () =>
    Promise.all([
      api.getLibraries(serverUrl).then(setLibraries).catch(() => {}),
      api.getUsers(serverUrl).then(setUsers).catch(() => {}),
    ]);

  useEffect(() => {
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [serverUrl]);

  useEffect(() => {
    const movies = libraries.filter(l => l.lib_type === 'movies');
    if (!upLib && movies.length > 0) setUpLib(movies[0].id);
  }, [libraries, upLib]);

  const flash = (m: string) => {
    setMsg(m);
    setTimeout(() => setMsg(''), 4000);
  };

  const upload = async () => {
    if (!upLib || !upName.trim() || !/^\d{4}$/.test(upYear.trim()) || !upVideo) {
      flash('Need a library, a name, a 4-digit year and a movie file.');
      return;
    }
    const form = new FormData();
    form.append('title', upName.trim());
    form.append('year', upYear.trim());
    form.append('video', upVideo);
    if (upSub) form.append('subtitle', upSub);
    setUploading(true);
    setUpProgress(0);
    try {
      const res = await api.uploadMovie(serverUrl, upLib, form, (loaded, total) =>
        setUpProgress(Math.round((loaded / total) * 100)),
      );
      flash(`Uploaded ${res.title}. Indexing runs in the background.`);
      setUpName('');
      setUpYear('');
      setUpVideo(null);
      setUpSub(null);
    } catch (e) {
      flash(e instanceof Error ? e.message : 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div style={{ maxWidth: 640 }}>
      <h3 style={{ fontSize: 'var(--sl-text-lg)' }}>Admin</h3>
      {msg && <p className="sl-tag">{msg}</p>}

      <h4>Libraries</h4>
      {libraries.map(lib => (
        <div
          key={lib.id}
          className="sl-card"
          style={{ marginBottom: 'var(--sl-space-sm)', padding: 'var(--sl-space-sm) var(--sl-space-md)' }}
        >
          <strong>{lib.name}</strong> <span className="sl-tag">{lib.lib_type}</span>
          <div style={{ color: 'var(--sl-text-link)', fontSize: 'var(--sl-text-sm)' }}>{lib.path}</div>
          <button
            className="sl-button"
            style={{ marginTop: 'var(--sl-space-sm)', opacity: 0.7 }}
            onClick={() => {
              if (!confirm(`Delete "${lib.name}" and its indexed items?`)) return;
              void api.deleteLibrary(serverUrl, lib.id).then(() => void reload());
            }}
          >
            Remove
          </button>
        </div>
      ))}
      <div className="sl-card" style={{ padding: 'var(--sl-space-md)' }}>
        <input className="sl-input" placeholder="Name" value={newLib.name} onChange={e => setNewLib({ ...newLib, name: e.target.value })} />
        <input
          className="sl-input"
          style={{ marginTop: 'var(--sl-space-sm)' }}
          placeholder="Folder on the server"
          value={newLib.path}
          onChange={e => setNewLib({ ...newLib, path: e.target.value })}
        />
        <div style={{ display: 'flex', gap: 'var(--sl-space-sm)', marginTop: 'var(--sl-space-sm)' }}>
          <select
            className="sl-input"
            style={{ width: 'auto' }}
            value={newLib.lib_type}
            onChange={e => setNewLib({ ...newLib, lib_type: e.target.value as 'movies' | 'shows' })}
          >
            <option value="movies">Movies</option>
            <option value="shows">TV Shows</option>
          </select>
          <button
            className="sl-button"
            onClick={() =>
              void api
                .addLibrary(serverUrl, newLib)
                .then(() => {
                  setNewLib({ name: '', path: '', lib_type: 'movies' });
                  return reload();
                })
                .catch(() => flash('Could not add library.'))
            }
          >
            Add library
          </button>
          <button className="sl-button" onClick={() => void api.triggerScan(serverUrl).then(() => flash('Scan started.'))}>
            Scan all
          </button>
        </div>
      </div>

      <h4 style={{ marginTop: 'var(--sl-space-lg)' }}>Upload movie</h4>
      <div className="sl-card" style={{ padding: 'var(--sl-space-md)' }}>
        {libraries.filter(l => l.lib_type === 'movies').length > 1 && (
          <select className="sl-input" value={upLib} onChange={e => setUpLib(e.target.value)}>
            {libraries
              .filter(l => l.lib_type === 'movies')
              .map(l => (
                <option key={l.id} value={l.id}>
                  {l.name}
                </option>
              ))}
          </select>
        )}
        <input
          className="sl-input"
          style={{ marginTop: 'var(--sl-space-sm)' }}
          placeholder="Movie name"
          value={upName}
          onChange={e => setUpName(e.target.value)}
          disabled={uploading}
        />
        <input
          className="sl-input"
          style={{ marginTop: 'var(--sl-space-sm)' }}
          placeholder="Year (e.g. 2021)"
          value={upYear}
          onChange={e => setUpYear(e.target.value.replace(/\D/g, '').slice(0, 4))}
          disabled={uploading}
        />
        <label style={{ display: 'block', marginTop: 'var(--sl-space-sm)', fontWeight: 700 }}>
          Movie file {upVideo && `(${upVideo.name})`}
        </label>
        <input
          type="file"
          accept="video/*,.mkv,.mk3d,.avi,.wmv,.flv,.ts,.m2ts,.mts,.mpg,.mpeg,.3gp,.ogv"
          onChange={e => setUpVideo(e.target.files?.[0] ?? null)}
          disabled={uploading}
        />
        <label style={{ display: 'block', marginTop: 'var(--sl-space-sm)', fontWeight: 700 }}>
          Subtitle (optional) {upSub && `(${upSub.name})`}
        </label>
        <input
          type="file"
          accept=".srt,.vtt"
          onChange={e => setUpSub(e.target.files?.[0] ?? null)}
          disabled={uploading}
        />
        {uploading && <p>Uploading... {upProgress}%</p>}
        <button className="sl-button" style={{ marginTop: 'var(--sl-space-sm)', width: '100%' }} onClick={() => void upload()} disabled={uploading}>
          {uploading ? 'Uploading...' : 'Upload movie'}
        </button>
      </div>

      <h4 style={{ marginTop: 'var(--sl-space-lg)' }}>Users</h4>
      {users.map(u => (
        <div
          key={u.user_id}
          className="sl-card"
          style={{
            marginBottom: 'var(--sl-space-sm)',
            padding: 'var(--sl-space-sm) var(--sl-space-md)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <span>
            <strong>{u.username}</strong> <span className="sl-tag">{u.is_admin ? 'admin' : 'user'}</span>
          </span>
          <button
            className="sl-button"
            style={{ opacity: 0.7 }}
            onClick={() => {
              if (!confirm(`Delete user "${u.username}"?`)) return;
              void api.deleteUser(serverUrl, u.user_id).then(() => void reload());
            }}
          >
            Delete
          </button>
        </div>
      ))}
      <div className="sl-card" style={{ padding: 'var(--sl-space-md)' }}>
        <input
          className="sl-input"
          placeholder="Username"
          value={newUser.username}
          onChange={e => setNewUser({ ...newUser, username: e.target.value })}
        />
        <input
          className="sl-input"
          style={{ marginTop: 'var(--sl-space-sm)' }}
          type="password"
          placeholder="Password"
          value={newUser.password_hash}
          onChange={e => setNewUser({ ...newUser, password_hash: e.target.value })}
        />
        <label style={{ display: 'flex', gap: 'var(--sl-space-sm)', alignItems: 'center', marginTop: 'var(--sl-space-sm)' }}>
          <input
            type="checkbox"
            checked={newUser.is_admin}
            onChange={e => setNewUser({ ...newUser, is_admin: e.target.checked })}
          />
          Admin
        </label>
        <button
          className="sl-button"
          style={{ marginTop: 'var(--sl-space-sm)' }}
          onClick={() =>
            void api
              .createUser(serverUrl, newUser)
              .then(() => {
                setNewUser({ username: '', password_hash: '', is_admin: false });
                return reload();
              })
              .catch(() => flash('Could not create user.'))
          }
        >
          Create user
        </button>
      </div>

      <h4 style={{ marginTop: 'var(--sl-space-lg)' }}>Invite codes</h4>
      <button
        className="sl-button"
        onClick={() =>
          void api
            .createInvite(serverUrl)
            .then(code => setInvite(code))
            .catch(() => flash('Could not create invite.'))
        }
      >
        Generate code
      </button>
      {invite && (
        <p>
          <span className="sl-tag">{invite}</span>
        </p>
      )}
    </div>
  );
}
