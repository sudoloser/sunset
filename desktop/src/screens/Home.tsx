import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, type Library, type MediaItem } from '../api/client';
import type { Session } from '../session';
import { cacheItems } from '../itemCache';
import { Hero } from '../components/Hero';
import { Poster } from '../components/Poster';
import { Row } from '../components/Row';

function dedupeShows(items: MediaItem[]): MediaItem[] {
  const seen = new Set<string>();
  return items.filter(item => {
    if (item.media_type === 'episode' && item.show_title) {
      if (seen.has(item.show_title)) return false;
      seen.add(item.show_title);
    }
    return true;
  });
}

export function Home({ serverUrl, session }: { serverUrl: string; session: Session }) {
  const navigate = useNavigate();
  const [recent, setRecent] = useState<MediaItem[]>([]);
  const [continueWatching, setContinueWatching] = useState<MediaItem[]>([]);
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [libraryItems, setLibraryItems] = useState<Record<string, MediaItem[]>>({});
  const [genres, setGenres] = useState<string[]>([]);
  const [genreItems, setGenreItems] = useState<Record<string, MediaItem[]>>({});
  const [collections, setCollections] = useState<{ name: string; items: MediaItem[] }[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState(false);

  const open = useCallback(
    (item: MediaItem) => {
      cacheItems([item]);
      navigate(`/item/${item.id}`, { state: item });
    },
    [navigate],
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [recentData, libs] = await Promise.all([
          api.getRecentlyAdded(serverUrl, session.user_id),
          api.getLibraries(serverUrl),
        ]);
        if (cancelled) return;
        cacheItems(recentData);
        setRecent(dedupeShows(recentData));
        setLibraries(libs);

        const cw = await api.getContinueWatching(serverUrl, session.user_id).catch(() => []);
        if (cancelled) return;
        cacheItems(cw);
        setContinueWatching(dedupeShows(cw));

        const perLib = await Promise.all(
          libs.map(async lib => ({
            lib,
            items: await api.getLibraryItems(serverUrl, lib.id, session.user_id).catch(() => []),
          })),
        );
        if (cancelled) return;
        const map: Record<string, MediaItem[]> = {};
        const all: MediaItem[] = [];
        for (const { lib, items } of perLib) {
          cacheItems(items);
          const grouped =
            lib.lib_type === 'shows' ? dedupeShows(items) : items;
          map[lib.id] = grouped.slice(0, 15);
          all.push(...items);
        }
        setLibraryItems(map);

        const groups = new Map<string, MediaItem[]>();
        for (const item of all) {
          if (!item.collection_name) continue;
          const list = groups.get(item.collection_name) ?? [];
          list.push(item);
          groups.set(item.collection_name, list);
        }
        setCollections(
          [...groups.entries()]
            .filter(([, items]) => items.length > 1)
            .map(([name, items]) => ({ name, items })),
        );

        const genreList = await api.getGenres(serverUrl).catch(() => []);
        if (cancelled) return;
        setGenres(genreList.slice(0, 5));
        const gmap: Record<string, MediaItem[]> = {};
        await Promise.all(
          genreList.slice(0, 5).map(async g => {
            const items = await api.getGenreItems(serverUrl, g, session.user_id).catch(() => []);
            cacheItems(items);
            gmap[g] = dedupeShows(items);
          }),
        );
        if (!cancelled) setGenreItems(gmap);
        if (!cancelled) setLoaded(true);
      } catch {
        if (!cancelled) {
          setLoaded(true);
          setLoadError(true);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [serverUrl, session.user_id]);

  if (!loaded) {
    return (
      <div style={{ padding: 'var(--sl-space-lg)', maxWidth: 1100, margin: '0 auto' }}>
        <div className="sl-skeleton" style={{ height: 320, marginBottom: 'var(--sl-space-lg)' }} />
        <div style={{ display: 'flex', gap: 'var(--sl-space-md)' }}>
          {[0, 1, 2, 3, 4].map(i => (
            <div key={i} className="sl-skeleton" style={{ width: 150, height: 225, flexShrink: 0 }} />
          ))}
        </div>
      </div>
    );
  }

  if (loadError && recent.length === 0 && libraries.length === 0) {
    return (
      <div className="sl-center" style={{ minHeight: '60vh' }}>
        <div className="sl-card" style={{ maxWidth: 440, textAlign: 'center' }}>
          <p>Couldn't reach the server. Check the IP in Settings, Server.</p>
          <button className="sl-button" onClick={() => navigate('/settings')}>
            Open settings
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: 'var(--sl-space-lg)', maxWidth: 1100, margin: '0 auto' }}>
      {recent[0] && <Hero serverUrl={serverUrl} item={recent[0]} />}
      {recent.length === 0 && (
        <div className="sl-card" style={{ textAlign: 'center' }}>
          Nothing indexed yet. Add a library in Settings, Admin, then scan.
        </div>
      )}

      {continueWatching.length > 0 && (
        <Row title="Continue watching">
          {continueWatching.map(item => (
            <Poster
              key={item.id}
              serverUrl={serverUrl}
              item={item}
              subtitle={item.show_title ? `${item.season}x${String(item.episode).padStart(2, '0')}` : item.year?.toString()}
              onClick={() => open(item)}
            />
          ))}
        </Row>
      )}

      <Row title="Recently added">
        {recent.map(item => (
          <Poster
            key={item.id}
            serverUrl={serverUrl}
            item={item}
            subtitle={item.show_title ? 'TV show' : item.year?.toString()}
            onClick={() => open(item)}
          />
        ))}
      </Row>

      {collections.map(c => (
        <Row key={c.name} title={c.name}>
          {c.items.map(item => (
            <Poster
              key={item.id}
              serverUrl={serverUrl}
              item={item}
              subtitle={item.year?.toString()}
              onClick={() => open(item)}
            />
          ))}
        </Row>
      ))}

      {libraries.map(lib => (
        <Row key={lib.id} title={lib.name}>
          {(libraryItems[lib.id] ?? []).map(item => (
            <Poster
              key={item.id}
              serverUrl={serverUrl}
              item={item}
              subtitle={lib.lib_type === 'shows' ? 'TV show' : item.year?.toString()}
              onClick={() => open(item)}
            />
          ))}
        </Row>
      ))}

      {genres.map(
        g =>
          (genreItems[g] ?? []).length > 0 && (
            <Row key={g} title={g}>
              {genreItems[g].map(item => (
                <Poster key={item.id} serverUrl={serverUrl} item={item} onClick={() => open(item)} />
              ))}
            </Row>
          ),
      )}
    </div>
  );
}
