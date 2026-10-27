import React from 'react';

type BadgeVariant = 'category' | 'status' | 'role' | 'featured';
type StatusValue = 'live' | 'pending' | 'needs_review' | 'closed' | 'rejected';

interface BadgeProps {
  variant: BadgeVariant;
  status?: StatusValue;
  className?: string;
  children: React.ReactNode;
}

const statusStyles: Record<StatusValue, React.CSSProperties> = {
  live: { backgroundColor: 'var(--color-status-live-bg)', color: 'var(--color-status-live-fg)' },
  pending: { backgroundColor: 'var(--color-status-pending-bg)', color: 'var(--color-status-pending-fg)' },
  needs_review: { backgroundColor: 'var(--color-status-review-bg)', color: 'var(--color-status-review-fg)' },
  closed: { backgroundColor: 'var(--color-status-closed-bg)', color: 'var(--color-status-closed-fg)' },
  rejected: { backgroundColor: 'var(--color-status-rejected-bg)', color: 'var(--color-status-rejected-fg)' },
};

const variantStyles: Record<Exclude<BadgeVariant, 'status'>, React.CSSProperties> = {
  category: { backgroundColor: 'var(--color-mint)', color: 'var(--color-primary)' },
  role: { backgroundColor: 'var(--color-navy)', color: 'var(--color-white)' },
  featured: { backgroundColor: 'var(--color-accent)', color: 'var(--color-white)' },
};

export function Badge({ variant, status, className = '', children }: BadgeProps) {
  let style: React.CSSProperties;

  if (variant === 'status' && status) {
    style = statusStyles[status];
  } else if (variant !== 'status') {
    style = variantStyles[variant];
  } else {
    style = statusStyles.pending;
  }

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${className}`}
      style={style}
    >
      {children}
    </span>
  );
}
