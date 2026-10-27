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
  live: { backgroundColor: '#D1FAE5', color: '#065F46' },
  pending: { backgroundColor: '#FEF3C7', color: '#92400E' },
  needs_review: { backgroundColor: '#FFEDD5', color: '#9A3412' },
  closed: { backgroundColor: '#F3F4F6', color: '#4B5563' },
  rejected: { backgroundColor: '#FEE2E2', color: '#991B1B' },
};

const variantStyles: Record<Exclude<BadgeVariant, 'status'>, React.CSSProperties> = {
  category: { backgroundColor: 'var(--color-mint)', color: 'var(--color-primary)' },
  role: { backgroundColor: 'var(--color-navy)', color: '#fff' },
  featured: { backgroundColor: 'var(--color-accent)', color: '#fff' },
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
