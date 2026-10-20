export type Tier = 'S' | 'A' | 'B' | 'C' | 'D'

export type Trend = 'RISING' | 'STEADY' | 'FALLING' | 'NOT_ENOUGH_DATA'

export interface HeroMeta {
    heroId: number
    heroName: string
    tier: Tier
    smoothedWinRate: number
    winRate: number
    pickRate: number
    games: number
    trend: Trend
    winRateChange: number | null
    imageUrl: string | null
}