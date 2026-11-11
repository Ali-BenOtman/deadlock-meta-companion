import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { apiUrl } from './api'
import { usePageTitle } from './usePageTitle'
import ItemIcon from './ItemIcon'
import ItemsTable from './ItemsTable'
import QuickAnswers from './QuickAnswers'
import DataFreshness from './DataFreshness'
import WinRateChart from './WinRateChart'
import RankFilter from './RankFilter'
import { tierLabel, useRank } from './rank'
import type { BuildStat, DailyStat, HeroMeta, HeroStat, ItemStat, PhaseItemStat } from './types'

const SECTIONS = [
    { id: 'builds', label: 'Builds' },
    { id: 'items', label: 'Items' },
]

const VIEWS = [
    { key: 'ALL', label: 'All' },
    { key: 'EARLY', label: 'Early (0-10 min)' },
    { key: 'MID', label: 'Mid (10-20 min)' },
    { key: 'LATE', label: 'Late (20+ min)' },
] as const

type View = (typeof VIEWS)[number]['key']

const COLLAPSED_BUILDS = 3

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
    const [wins, setWins] = useState<number | null>(null)
    const [daily, setDaily] = useState<DailyStat[]>([])
    const [phaseItems, setPhaseItems] = useState<PhaseItemStat[] | null>(null)
    const [view, setView] = useState<View>('ALL')
    const [showAllBuilds, setShowAllBuilds] = useState(false)
    const [active, setActive] = useState('builds')
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    usePageTitle(hero?.heroName)

    useEffect(() => {
        let cancelled = false
        getJson<HeroMeta[]>(`/api/meta?minTier=${minTier}`)
            .then(async (heroes) => {
                const found = heroes.find((h) => String(h.heroId) === heroId)
                if (!found) return
                const [itemData, buildData, statData, dailyData, phaseData] = await Promise.all([
                    getJson<ItemStat[]>(`/api/heroes/${heroId}/items?minTier=${minTier}`),
                    getJson<BuildStat[]>(`/api/heroes/${heroId}/builds?minTier=${minTier}`),
                    getJson<HeroStat[]>(`/api/heroes/stats?minTier=${minTier}`).catch(() => null),
                    getJson<DailyStat[]>(`/api/heroes/${heroId}/daily?minTier=${minTier}`).catch(() => []),
                    getJson<PhaseItemStat[]>(`/api/heroes/${heroId}/items/phases?minTier=${minTier}`).catch(
                        () => null,
                    ),
                ])
                if (cancelled) return
                setHero(found)
                setItems(itemData)
                setBuilds(buildData)
                setWins(statData?.find((s) => String(s.heroId) === heroId)?.wins ?? null)
                setDaily(dailyData)
                setPhaseItems(phaseData)
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

    // The switch only exists when the phase endpoint answered with data; otherwise the page
    // shows the overall table exactly as before.
    const hasPhases = phaseItems !== null && phaseItems.length > 0
    const activeView: View = hasPhases ? view : 'ALL'
    const rows = activeView === 'ALL' ? items : (phaseItems ?? []).filter((item) => item.phase === activeView)
    const shownBuilds = showAllBuilds ? builds : builds.slice(0, COLLAPSED_BUILDS)

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
                        {wins !== null && (
                            <span className="hero-record">
                                ({wins.toLocaleString()} wins, {(hero.games - wins).toLocaleString()} losses)
                            </span>
                        )}
                    </p>
                    <DataFreshness />
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

            <QuickAnswers items={items} phaseItems={phaseItems} />

            {daily.length >= 2 && (
                <section className="hero-section">
                    <h2>Win rate over time</h2>
                    <WinRateChart days={daily} />
                </section>
            )}

            <section id="builds" className="hero-section">
                <h2>Starting builds</h2>
                <p className="subtitle">
                    The most-played first six items. Many of these differ by a single item, and the order they were
                    bought in is not tracked.
                </p>
                <ul className="build-list">
                    {shownBuilds.map((build) => (
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
                {builds.length > COLLAPSED_BUILDS && (
                    <button type="button" className="show-all" onClick={() => setShowAllBuilds(!showAllBuilds)}>
                        {showAllBuilds ? 'Show fewer' : `Show all ${builds.length} builds`}
                    </button>
                )}
            </section>

            <section id="items" className="hero-section">
                <h2>Items</h2>
                <p className="subtitle">
                    Item win rates are correlational: players who are ahead buy items sooner and win more.
                </p>
                {hasPhases && (
                    <div className="rank-chips" role="group" aria-label="Game phase">
                        <span className="rank-label">First bought</span>
                        {VIEWS.map((option) => (
                            <button
                                key={option.key}
                                type="button"
                                className={option.key === activeView ? 'rank-chip active' : 'rank-chip'}
                                aria-pressed={option.key === activeView}
                                onClick={() => setView(option.key)}
                            >
                                {option.label}
                            </button>
                        ))}
                    </div>
                )}
                {activeView !== 'ALL' && (
                    <p className="subtitle">
                        Items grouped by when players first bought them. Buy rate is the share of this hero's
                        players who first bought the item in that phase.
                    </p>
                )}
                <ItemsTable key={`${activeView}-${minTier}`} rows={rows} showMedian={activeView === 'ALL'} />
            </section>
        </main>
    )
}