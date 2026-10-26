import { useSearchParams } from 'react-router-dom'
import type { HeroMeta } from './types'

export type SortKey = 'tier' | 'winRate' | 'pickRate' | 'trend'

export const SORTS: { key: SortKey; label: string }[] = [
    { key: 'tier', label: 'Tier' },
    { key: 'winRate', label: 'Win rate' },
    { key: 'pickRate', label: 'Pick rate' },
    { key: 'trend', label: 'Trend (rising first)' },
]

export function sortHeroes(heroes: HeroMeta[], key: SortKey): HeroMeta[] {
    const list = [...heroes]
    const change = (hero: HeroMeta) => hero.winRateChange ?? Number.NEGATIVE_INFINITY
    switch (key) {
        case 'winRate':
            return list.sort((a, b) => b.smoothedWinRate - a.smoothedWinRate)
        case 'pickRate':
            return list.sort((a, b) => b.pickRate - a.pickRate)
        case 'trend':
            return list.sort((a, b) => (change(b) === change(a) ? 0 : change(b) > change(a) ? 1 : -1))
        default:
            return list
    }
}

export function biggestMovers(heroes: HeroMeta[], count = 4): HeroMeta[] {
    return heroes
        .filter((hero) => hero.trend === 'RISING' || hero.trend === 'FALLING')
        .sort((a, b) => Math.abs(b.winRateChange ?? 0) - Math.abs(a.winRateChange ?? 0))
        .slice(0, count)
}

export function useTierTools() {
    const [params, setParams] = useSearchParams()
    const q = params.get('q') ?? ''
    const rawSort = params.get('sort')
    const sort: SortKey = SORTS.some((s) => s.key === rawSort) ? (rawSort as SortKey) : 'tier'

    const update = (name: string, value: string, emptyValue: string) => {
        const next = new URLSearchParams(params)
        if (value === emptyValue) {
            next.delete(name)
        } else {
            next.set(name, value)
        }
        setParams(next, { replace: true })
    }

    return {
        q,
        sort,
        setQ: (value: string) => update('q', value, ''),
        setSort: (value: SortKey) => update('sort', value, 'tier'),
    }
}