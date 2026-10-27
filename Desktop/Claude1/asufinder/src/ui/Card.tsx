import React from 'react';

interface CardProps {
  featured?: boolean;
  className?: string;
  children: React.ReactNode;
}

export function Card({ featured = false, className = '', children }: CardProps) {
  const style: React.CSSProperties = {
    backgroundColor: 'var(--color-surface)',
    outline: featured ? '2px solid var(--color-accent)' : 'none',
    outlineOffset: featured ? '0px' : undefined,
  };

  return (
    <div
      className={`rounded-lg shadow-sm p-4 ${className}`}
      style={style}
    >
      {children}
    </div>
  );
}
