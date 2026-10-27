import React from 'react';

type Variant = 'primary' | 'secondary' | 'ghost';
type Size = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  children: React.ReactNode;
}

const sizeClasses: Record<Size, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg',
};

const variantStyle: Record<Variant, React.CSSProperties> = {
  primary: {
    backgroundColor: 'var(--color-primary)',
    color: 'var(--color-white)',
  },
  secondary: {
    backgroundColor: 'transparent',
    color: 'var(--color-primary)',
    border: '1.5px solid var(--color-primary)',
  },
  ghost: {
    backgroundColor: 'transparent',
    color: 'var(--color-navy)',
    border: 'none',
  },
};

export function Button({
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...props
}: ButtonProps) {
  const base =
    'inline-flex items-center justify-center rounded-md font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none';

  return (
    <button
      className={`${base} ${sizeClasses[size]} btn-${variant} ${className}`}
      style={variantStyle[variant]}
      {...props}
    >
      {children}
    </button>
  );
}
