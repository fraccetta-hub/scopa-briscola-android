import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  variant?: 'default' | 'search';
}

const MagnifierIcon = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 16 16"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
    style={{ color: 'var(--color-navy)', opacity: 0.5 }}
  >
    <circle cx="6.5" cy="6.5" r="4.5" stroke="currentColor" strokeWidth="1.5" />
    <line x1="9.9" y1="9.9" x2="14" y2="14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
  </svg>
);

export function Input({
  label,
  error,
  variant = 'default',
  id,
  className = '',
  ...props
}: InputProps) {
  const inputId = id ?? (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  const baseStyle: React.CSSProperties = {
    border: error
      ? '1.5px solid var(--color-error)'
      : '1.5px solid color-mix(in srgb, var(--color-navy) 20%, transparent)',
    borderRadius: '0.375rem',
    backgroundColor: 'var(--color-surface)',
    color: 'var(--color-navy)',
    width: '100%',
    padding: variant === 'search' ? '0.5rem 0.75rem 0.5rem 2.25rem' : '0.5rem 0.75rem',
    outline: 'none',
    transition: 'border-color 0.15s, box-shadow 0.15s',
  };

  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      {label && (
        <label
          htmlFor={inputId}
          style={{ color: 'var(--color-navy)', fontSize: '0.875rem', fontWeight: 500 }}
        >
          {label}
        </label>
      )}
      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
        {variant === 'search' && (
          <span
            style={{
              position: 'absolute',
              left: '0.625rem',
              display: 'flex',
              alignItems: 'center',
              pointerEvents: 'none',
            }}
          >
            <MagnifierIcon />
          </span>
        )}
        <input
          id={inputId}
          style={baseStyle}
          className="input-field"
          {...props}
        />
      </div>
      {error && (
        <span style={{ color: 'var(--color-error)', fontSize: '0.75rem' }}>{error}</span>
      )}
    </div>
  );
}
