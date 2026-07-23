import { useParams, useLocation, useNavigate, useOutletContext } from 'react-router-dom';
import { CollectionView } from '../features/library/CollectionView';
import type { MediaItem } from '@sunset/shared';

interface LayoutContext {
  onSelectItem: (item: MediaItem) => void;
}

export function CollectionPage() {
  const { name } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { onSelectItem } = useOutletContext<LayoutContext>();
  const items = (location.state as any)?.items as MediaItem[] | undefined;

  if (!name || !items) {
    return <div style={{ padding: '2rem', color: 'var(--text-secondary)' }}>Collection not found</div>;
  }

  return (
    <CollectionView
      name={name}
      items={items}
      onSelectItem={onSelectItem}
      onBack={() => navigate(-1)}
    />
  );
}
