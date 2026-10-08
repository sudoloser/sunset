import type { ReactNode } from 'react';

export function Row({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section style={{ marginBottom: 'var(--sl-space-lg)' }}>
      <h2 style={{ fontSize: 'var(--sl-text-xl)', margin: '0 0 var(--sl-space-md)' }}>{title}</h2>
      <div
        className="sl-scrollbar"
        style={{ display: 'flex', gap: 'var(--sl-space-md)', overflowX: 'auto', paddingBottom: 'var(--sl-space-sm)' }}
      >
        {children}
      </div>
    </section>
  );
}
