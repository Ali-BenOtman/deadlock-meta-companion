import { useState } from 'react'

type Theme = 'light' | 'dark'

function currentTheme(): Theme {
    return document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark'
}

export default function ThemeToggle() {
    const [theme, setTheme] = useState<Theme>(currentTheme)

    const toggle = () => {
        const next: Theme = theme === 'light' ? 'dark' : 'light'
        document.documentElement.setAttribute('data-theme', next)
        document
            .querySelector('meta[name="theme-color"]')
            ?.setAttribute('content', next === 'light' ? '#f4f3ee' : '#0f1115')
        try {
            localStorage.setItem('theme', next)
        } catch {
            // Storage can be unavailable; the choice then lasts until the page is closed.
        }
        setTheme(next)
    }

    return (
        <button
            type="button"
            className="theme-toggle"
            onClick={toggle}
            aria-label={theme === 'light' ? 'Switch to the dark theme' : 'Switch to the light theme'}
            title={theme === 'light' ? 'Dark theme' : 'Light theme'}
        >
            {theme === 'light' ? (
                <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path d="M21 12.8A8.5 8.5 0 1 1 11.2 3a6.8 6.8 0 0 0 9.8 9.8z" />
                </svg>
            ) : (
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    aria-hidden="true"
                >
                    <circle cx="12" cy="12" r="4" />
                    <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
                </svg>
            )}
        </button>
    )
}