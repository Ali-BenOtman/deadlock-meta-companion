import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { RANKS, rankImage, rankNote, useRank } from './rank'

function RankIcon({ minTier }: { minTier: number }) {
    const url = rankImage(minTier)
    if (!url) return <span className="rank-icon" aria-hidden="true" />
    return (
        <img
            className="rank-icon"
            src={url}
            alt=""
            width={24}
            height={24}
            onError={(e) => {
                e.currentTarget.style.visibility = 'hidden'
            }}
        />
    )
}

function RankMenu() {
    const { minTier, setMinTier } = useRank()
    const [open, setOpen] = useState(false)
    const [active, setActive] = useState(0)
    const rootRef = useRef<HTMLDivElement>(null)
    const current = RANKS.find((rank) => rank.minTier === minTier) ?? RANKS[0]

    useEffect(() => {
        if (!open) return
        const onDown = (event: MouseEvent) => {
            if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
                setOpen(false)
            }
        }
        document.addEventListener('mousedown', onDown)
        return () => document.removeEventListener('mousedown', onDown)
    }, [open])

    const openMenu = () => {
        setActive(Math.max(0, RANKS.findIndex((rank) => rank.minTier === minTier)))
        setOpen(true)
    }

    const choose = (value: number) => {
        setMinTier(value)
        setOpen(false)
    }

    const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
        if (!open) {
            if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                openMenu()
            }
            return
        }
        if (event.key === 'Escape') {
            setOpen(false)
        } else if (event.key === 'ArrowDown') {
            event.preventDefault()
            setActive((i) => Math.min(i + 1, RANKS.length - 1))
        } else if (event.key === 'ArrowUp') {
            event.preventDefault()
            setActive((i) => Math.max(i - 1, 0))
        } else if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            choose(RANKS[active].minTier)
        }
    }

    return (
        <div className="rank-select" ref={rootRef}>
            <span className="rank-label">Rank</span>
            <button
                type="button"
                className="rank-trigger"
                title={rankNote(minTier)}
                aria-haspopup="listbox"
                aria-expanded={open}
                aria-activedescendant={open ? `rank-option-${active}` : undefined}
                onClick={() => (open ? setOpen(false) : openMenu())}
                onKeyDown={onKeyDown}
            >
                {current.minTier !== 0 && <RankIcon minTier={current.minTier} />}
                <span>{current.label}</span>
                <span className="rank-caret" aria-hidden="true">
                    ▾
                </span>
            </button>
            {open && (
                <ul className="rank-menu-list" role="listbox" aria-label="Rank">
                    {RANKS.map((rank, index) => (
                        <li
                            key={rank.minTier}
                            id={`rank-option-${index}`}
                            role="option"
                            aria-selected={rank.minTier === minTier}
                            className={index === active ? 'rank-option active' : 'rank-option'}
                            onMouseEnter={() => setActive(index)}
                            onClick={() => choose(rank.minTier)}
                        >
                            <RankIcon minTier={rank.minTier} />
                            <span>{rank.label}</span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}

export default function RankFilter({ variant = 'select' }: { variant?: 'chips' | 'select' }) {
    const { minTier, setMinTier } = useRank()

    if (variant === 'select') {
        return <RankMenu />
    }

    return (
        <div className="rank-chips" role="group" aria-label="Rank filter">
            <span className="rank-label">Rank</span>
            {RANKS.map((rank) => (
                <button
                    key={rank.minTier}
                    type="button"
                    className={rank.minTier === minTier ? 'rank-chip active' : 'rank-chip'}
                    aria-pressed={rank.minTier === minTier}
                    onClick={() => setMinTier(rank.minTier)}
                >
                    {rank.label}
                </button>
            ))}
        </div>
    )
}