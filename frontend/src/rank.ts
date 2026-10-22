import { useSearchParams } from 'react-router-dom'
import type { Tier } from './types'

export const RANKS = [
    { minTier: 0, label: 'All ranks' },
    { minTier: 2, label: 'Seeker' },
    { minTier: 3, label: 'Acolyte' },
    { minTier: 4, label: 'Sentinel' },
    { minTier: 5, label: 'Mystic' },
    { minTier: 6, label: 'Ritualist' },
    { minTier: 7, label: 'Emissary' },
    { minTier: 8, label: 'Oracle' },
    { minTier: 9, label: 'Phantom' },
    { minTier: 10, label: 'Ascendant' },
    { minTier: 11, label: 'Eternus' },
]

const VALID = new Set(RANKS.map((rank) => rank.minTier))

export function rankImage(minTier: number): string | null {
    if (minTier === 0) return null
    const number = String(minTier).padStart(2, '0')
    return `https://assets-bucket.deadlock-api.com/assets-api-res/images/ranks/rank${number}_lg.webp`
}

export function rankNote(minTier: number): string {
    const rank = RANKS.find((r) => r.minTier === minTier)
    if (!rank || minTier === 0) return 'All ranks'
    return minTier === 11 ? `${rank.label} only` : `${rank.label} and above`
}

export function tierLabel(tier: Tier): string {
    return tier === 'NOT_ENOUGH_DATA' ? '?' : tier
}

export function useRank() {
    const [params, setParams] = useSearchParams()
    const raw = Number(params.get('minTier'))
    const minTier = VALID.has(raw) ? raw : 0

    const setMinTier = (value: number) => {
        const next = new URLSearchParams(params)
        if (value === 0) {
            next.delete('minTier')
        } else {
            next.set('minTier', String(value))
        }
        setParams(next, { replace: true })
    }

    return { minTier, setMinTier }
}