import { Link } from 'react-router-dom'
import type { HeroMeta, Trend } from './types'

const TREND_SYMBOL: Record<Trend, string> = {
    RISING: '▲',
    FALLING: '▼',
    STEADY: '–',
    NOT_ENOUGH_DATA: '?',
}

const TREND_LABEL: Record<Trend, string> = {
    RISING: 'Rising',
    FALLING: 'Falling',
    STEADY: 'Steady',
    NOT_ENOUGH_DATA: 'Not enough data for a trend',
}

function formatChange(change: number | null): string {
    if (change === null) return 'n/a'
    const sign = change > 0 ? '+' : ''
    return `${sign}${change.toFixed(1)}`
}

export default function HeroTile({ hero }: { hero: HeroMeta }) {
    const title =
        `${hero.heroName}: ${hero.winRate.toFixed(2)}% win rate, ` +
        `${hero.pickRate.toFixed(1)}% pick rate, ${hero.games.toLocaleString()} games. ` +
        `${TREND_LABEL[hero.trend]} (${formatChange(hero.winRateChange)})`

    return (
        <Link className="tile" title={title} to={`/hero/${hero.heroId}`}>
            {hero.imageUrl ? (
                <img src={hero.imageUrl} alt={hero.heroName} width={64} height={64} loading="lazy" />
            ) : (
                <div className="tile-fallback" aria-hidden="true">
                    {hero.heroName.charAt(0)}
                </div>
            )}
            <span className="tile-name">{hero.heroName}</span>
            <span className="tile-rate">{hero.smoothedWinRate.toFixed(1)}%</span>
            <span className={`tile-trend trend-${hero.trend.toLowerCase()}`}>
                {TREND_SYMBOL[hero.trend]}
            </span>
        </Link>
    )
}