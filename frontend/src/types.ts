export type Tier = 'S' | 'A' | 'B' | 'C' | 'D' | 'NOT_ENOUGH_DATA'

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

export interface ItemStat {
    itemId: number
    itemName: string
    buyers: number
    buyRate: number
    winRate: number
    winRateDelta: number
    medianBuySeconds: number | null
    imageUrl?: string | null
}

export interface BuildStat {
    itemIds: number[]
    itemNames: string[]
    games: number
    winRate: number
    winRateDelta: number
    imageUrls?: string | null
}