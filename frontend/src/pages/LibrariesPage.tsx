import { useNavigate, useOutletContext } from 'react-router-dom';
import { LibrariesTab } from '../features/library/LibrariesTab';
import type { MediaItem } from '@sunset/shared';

interface LayoutContext {
  onSelectItem: (item: MediaItem) => void;
  userId?: string;
  isAdmin: boolean;
}

export function LibrariesPage() {
  const navigate = useNavigate();
  const { onSelectItem, userId, isAdmin } = useOutletContext<LayoutContext>();
  return (
    <LibrariesTab
      isAdmin={isAdmin}
      onSelectItem={onSelectItem}
      onGoToSettings={() => navigate('/settings')}
      userId={userId}
    />
  );
}
