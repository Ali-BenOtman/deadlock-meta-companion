import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import type { BuildStat, ItemStat } from './types'

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
    const response = await fetch(url)
    if (!response.ok) {
        throw new Error(`API returned ${response.status}`)
    }
    return (await response.json()) as T
}

export default function HeroPage() {
    const { heroId } = useParams()
    const [items, setItems] = useState<ItemStat[]>([])
    const [builds, setBuilds] = useState<BuildStat[]>([])
    const [minBuyRate, setMinBuyRate] = useState(5)
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {

        Promise.all([
            getJson<ItemStat[]>(`/api/heroes/${heroId}/items`),
            getJson<BuildStat[]>(`/api/heroes/${heroId}/builds`),
        ])
            .then(([itemData, buildData]) => {
                setItems(itemData)
                setBuilds(buildData)
            })
            .catch((e: Error) => setError(e.message))
            .finally(() => setLoading(false))
    }, [heroId])

    if (loading) return <p className="status">Loading...</p>
    if (error) return <p className="status">Failed to load: {error}</p>

    const shownItems = items.filter((item) => item.buyRate >= minBuyRate).slice(0, 20)

    return (
        <main className="page">
            <Link to="/">← Back to tier list</Link>
            <h1>Hero {heroId}</h1>
            <p className="subtitle">
                Item win rates are correlational: players who are ahead buy items sooner and win more.
            </p>

            <h2>Best items</h2>
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
                            <td>{item.itemName}</td>
                            <td>{item.winRate.toFixed(1)}%</td>
                            <td>{formatDelta(item.winRateDelta)}</td>
                            <td>{item.buyRate.toFixed(1)}%</td>
                            <td>{formatTime(item.medianBuySeconds)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>

            <h2>Top builds</h2>
            <ul className="build-list">
                {builds.map((build) => (
                    <li key={build.itemIds.join('-')}>
                        <strong>{build.winRate.toFixed(1)}%</strong> ({formatDelta(build.winRateDelta)}) ·{' '}
                        {build.games.toLocaleString()} games
                        <div>{build.itemNames.join(' → ')}</div>
                    </li>
                ))}
            </ul>
        </main>
    )
}