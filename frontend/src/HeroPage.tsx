import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { apiUrl } from './api'
import ItemIcon from './ItemIcon'
import RankFilter from './RankFilter'
import { tierLabel, useRank } from './rank'
import type { BuildStat, HeroMeta, ItemStat } from './types'

const SECTIONS = [
    { id: 'builds', label: 'Builds' },
    { id: 'items', label: 'Items' },
]

function formatTime(seconds: number | null): string {
    if (seconds === null) return 'n/a'
    const minutes = Math.floor(seconds / 60)
    const rest = seconds % 60
    return `${minutes}:${rest.toString().padStart(2, '0')}`
}

function formatDelta(delta: number): string {
    const sign = delta > 0 ? '+' : ''
    return `${sign}${delta.toFixed(1)}`
}

async function getJson<T>(url: string): Promise<T> {
    const response = await fetch(apiUrl(url))
    if (!response.ok) {
        throw new Error(`API returned ${response.status}`)
    }
    return (await response.json()) as T
}

export default function HeroPage() {
    const { heroId } = useParams()
    const { search } = useLocation()
    const { minTier } = useRank()
    const [hero, setHero] = useState<HeroMeta | null>(null)
    const [items, setItems] = useState<ItemStat[]>([])
    const [builds, setBuilds] = useState<BuildStat[]>([])
    const [minBuyRate, setMinBuyRate] = useState(5)
    const [active, setActive] = useState('builds')
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let cancelled = false
        getJson<HeroMeta[]>(`/api/meta?minTier=${minTier}`)
            .then(async (heroes) => {
                const found = heroes.find((h) => String(h.heroId) === heroId)
                if (!found) return
                const [itemData, buildData] = await Promise.all([
                    getJson<ItemStat[]>(`/api/heroes/${heroId}/items?minTier=${minTier}`),
                    getJson<BuildStat[]>(`/api/heroes/${heroId}/builds?minTier=${minTier}`),
                ])
                if (cancelled) return
                setHero(found)
                setItems(itemData)
                setBuilds(buildData)
            })
            .catch((e: Error) => {
                if (!cancelled) setError(e.message)
            })
            .finally(() => {
                if (!cancelled) setLoading(false)
            })
        return () => {
            cancelled = true
        }
    }, [heroId, minTier])

    useEffect(() => {
        if (!hero) return
        const observer = new IntersectionObserver(
            (entries) => {
                const visible = entries
                    .filter((entry) => entry.isIntersecting)
                    .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
                if (visible.length > 0) setActive(visible[0].target.id)
            },
            { rootMargin: '-90px 0px -60% 0px' },
        )
        SECTIONS.forEach(({ id }) => {
            const element = document.getElementById(id)
            if (element) observer.observe(element)
        })
        return () => observer.disconnect()
    }, [hero])

    if (loading) return <p className="status">Loading...</p>
    if (error) return <p className="status">Failed to load: {error}</p>
    if (!hero) {
        return (
            <main className="page">
                <Link to={{ pathname: '/tier-list', search }}>← Back to tier list</Link>
                <h1>Hero not found</h1>
                <p className="subtitle">There is no data for this hero at the selected rank.</p>
                <RankFilter />
            </main>
        )
    }

    const shownItems = items.filter((item) => item.buyRate >= minBuyRate).slice(0, 20)

    return (
        <main className="page">
            <Link to={{ pathname: '/tier-list', search }}>← Back to tier list</Link>

            <header className="hero-header">
                {hero.imageUrl ? (
                    <img src={hero.imageUrl} alt={hero.heroName} width={96} height={96} />
                ) : (
                    <div className="hero-header-fallback" aria-hidden="true">
                        {hero.heroName.charAt(0)}
                    </div>
                )}
                <div>
                    <h1>{hero.heroName}</h1>
                    <p className="hero-summary">
                        <span className={`tier-badge tier-${hero.tier}`}>{tierLabel(hero.tier)}</span>{' '}
                        {hero.winRate.toFixed(1)}% win rate · {hero.pickRate.toFixed(1)}% pick rate ·{' '}
                        {hero.games.toLocaleString()} games
                    </p>
                </div>
            </header>

            <nav className="section-bar" aria-label="Hero page sections">
                {SECTIONS.map(({ id, label }) => (
                    <Link
                        key={id}
                        to={{ search, hash: `#${id}` }}
                        replace
                        className={active === id ? 'active' : undefined}
                    >
                        {label}
                    </Link>
                ))}
                <RankFilter variant="select" />
            </nav>

            <section id="builds" className="hero-section">
                <h2>Starting builds</h2>
                <p className="subtitle">
                    The most-played first six items. The order they were bought in is not tracked.
                </p>
                <ul className="build-list">
                    {builds.map((build) => (
                        <li key={build.itemIds.join('-')}>
                            <strong>{build.winRate.toFixed(1)}%</strong> ({formatDelta(build.winRateDelta)}) ·{' '}
                            {build.games.toLocaleString()} games
                            <div className="build-items">
                                {build.itemNames.map((name, index) => (
                                    <span key={build.itemIds[index]} className="chip">
                                        <ItemIcon url={build.imageUrls?.[index]} name={name} size={22} />
                                        {name}
                                    </span>
                                ))}
                            </div>
                        </li>
                    ))}
                </ul>
            </section>

            <section id="items" className="hero-section">
                <h2>Best items</h2>
                <p className="subtitle">
                    Item win rates are correlational: players who are ahead buy items sooner and win more.
                </p>
                <label>
                    Minimum buy rate: {minBuyRate}%{' '}
                    <input
                        type="range"
                        min={0}
                        max={30}
                        step={1}
                        value={minBuyRate}
                        onChange={(e) => setMinBuyRate(Number(e.target.value))}
                    />
                </label>
                <table className="stats-table">
                    <thead>
                        <tr>
                            <th>Item</th>
                            <th>Win rate</th>
                            <th>vs hero avg</th>
                            <th>Buy rate</th>
                            <th>Median buy</th>
                        </tr>
                    </thead>
                    <tbody>
                        {shownItems.map((item) => (
                            <tr key={item.itemId}>
                                <td>
                                    <span className="item-cell">
                                        <ItemIcon url={item.imageUrl} name={item.itemName} size={36} />
                                        {item.itemName}
                                    </span>
                                </td>
                                <td>{item.winRate.toFixed(1)}%</td>
                                <td>{formatDelta(item.winRateDelta)}</td>
                                <td>{item.buyRate.toFixed(1)}%</td>
                                <td>{formatTime(item.medianBuySeconds)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </section>
        </main>
    )
}