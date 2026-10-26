export default function Logo({ size = 28 }: { size?: number }) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 32 32"
            aria-hidden="true"
            style={{ display: 'block', flex: 'none' }}
        >
            <rect width="32" height="32" rx="7" style={{ fill: 'var(--accent)' }} />
            <circle cx="16" cy="13" r="4.6" style={{ fill: 'var(--bg)' }} />
            <path d="M13.4 15.5 H18.6 L20.6 25.5 H11.4 Z" style={{ fill: 'var(--bg)' }} />
        </svg>
    )
}