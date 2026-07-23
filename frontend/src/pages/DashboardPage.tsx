import { useOutletContext } from 'react-router-dom';
import { Dashboard } from '../features/dashboard/Dashboard';
import type { MediaItem } from '@sunset/shared';

interface LayoutContext {
  onSelectItem: (item: MediaItem) => void;
  onPlayItem: (item: MediaItem) => void;
  onSearch: () => void;
  userId?: string;
}

export function DashboardPage() {
  const { onSelectItem, onPlayItem, onSearch } = useOutletContext<LayoutContext>();
  return <Dashboard onSelectItem={onSelectItem} onPlayItem={onPlayItem} onSearch={onSearch} />;
}
