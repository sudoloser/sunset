import { useOutletContext } from 'react-router-dom';
import { Settings } from '../features/settings/Settings';

interface LayoutContext {
  isAdmin: boolean;
}

export function SettingsPage() {
  const { isAdmin } = useOutletContext<LayoutContext>();
  return <Settings isAdmin={isAdmin} />;
}
