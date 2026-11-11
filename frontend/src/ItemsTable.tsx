import { useState } from 'react'
import ItemIcon from './ItemIcon'

export interface ItemRow {
    itemId: number
    itemName: string
    imageUrl?: string | null
    buyers: number
    buyRate: number
    winRate: number
    winRateDelta: number
    medianBuySeconds?: number | null
}

type SortKey = 'winRate' | 'winRateDelta' | 'buyRate' | 'buyers' | 'median'

const COLLAPSED_ROWS = 20
// Rows with fewer buyers than this are greyed out: their win rate is less reliable.
const SMALL_SAMPLE = 500
// The API hides items with fewer buyers than this (MIN_BUYERS in ItemStatsService).
const MIN_BUYERS = 100

function formatDelta(delta: number): string {
    const sign = delta > 0 ? '+' : ''
    return `${sign}${delta.toFixed(1)}`
}

function formatTime(seconds: number | null): string {
    if (seconds === null) return 'n/a'
    const minutes = Math.floor(seconds / 60)
    const rest = seconds % 60
    return `${minutes}:${rest.toString().padStart(2, '0')}`
}

export default function ItemsTable({ rows, showMedian }: { rows: ItemRow[]; showMedian: boolean }) {
    const [sortKey, setSortKey] = useState<SortKey>('buyers')
    const [descending, setDescending] = useState(true)
    const [showAll, setShowAll] = useState(false)

    if (rows.length === 0) {
        return <p className="tier-note">No items with enough data here.</p>
    }

    // The bars are scaled to the largest value in the table, so they stay readable in every view.
    const maxBuyRate = Math.max(...rows.map((r) => r.buyRate))
    const maxWinRate = Math.max(...rows.map((r) => r.winRate))
    const winBar = (winRate: number) =>
        maxWinRate > 40 ? Math.min(100, Math.max(0, ((winRate - 40) / (maxWinRate - 40)) * 100)) : 0
    const buyBar = (rate: number) => (maxBuyRate > 0 ? (rate / maxBuyRate) * 100 : 0)

    const value = (row: ItemRow, key: SortKey): number =>
        key === 'median' ? (row.medianBuySeconds ?? -1) : row[key]

    const sorted = [...rows].sort((a, b) =>
        descending ? value(b, sortKey) - value(a, sortKey) : value(a, sortKey) - value(b, sortKey),
    )
    const visible = showAll ? sorted : sorted.slice(0, COLLAPSED_ROWS)

    const sortBy = (key: SortKey) => {
        if (key === sortKey) {
            setDescending(!descending)
        } else {
            setSortKey(key)
            setDescending(true)
        }
    }

    const header = (key: SortKey, label: string) => (
        <th aria-sort={sortKey === key ? (descending ? 'descending' : 'ascending') : 'none'}>
            <button
                type="button"
                className={sortKey === key ? 'sort-button active' : 'sort-button'}
                onClick={() => sortBy(key)}
            >
                {label}
                {sortKey === key && <span aria-hidden="true">{descending ? ' ▼' : ' ▲'}</span>}
            </button>
        </th>
    )

    return (
        <>
            <table className="stats-table items-table">
                <thead>
                    <tr>
                        <th>Item</th>
                        {header('winRate', 'Win rate')}
                        {header('winRateDelta', 'vs hero avg')}
                        {header('buyRate', 'Buy rate')}
                        {header('buyers', 'Buyers')}
                        {showMedian && header('median', 'Median buy')}
                    </tr>
                </thead>
                <tbody>
                    {visible.map((row) => (
                        <tr
                            key={row.itemId}
                            className={row.buyers < SMALL_SAMPLE ? 'low-sample' : undefined}
                            title={row.buyers < SMALL_SAMPLE ? `Small sample: fewer than ${SMALL_SAMPLE} buyers` : undefined}
                        >
                            <td>
                                <span className="item-cell">
                                    <ItemIcon url={row.imageUrl} name={row.itemName} size={36} />
                                    {row.itemName}
                                </span>
                            </td>
                            <td>
                                <div>{row.winRate.toFixed(1)}%</div>
                                <div className="bar-track">
                                    <div className="bar" style={{ width: `${winBar(row.winRate)}%` }} />
                                </div>
                            </td>
                            <td>{formatDelta(row.winRateDelta)}</td>
                            <td>
                                <div>{row.buyRate.toFixed(1)}%</div>
                                <div className="bar-track">
                                    <div className="bar" style={{ width: `${buyBar(row.buyRate)}%` }} />
                                </div>
                            </td>
                            <td>{row.buyers.toLocaleString()}</td>
                            {showMedian && <td>{formatTime(row.medianBuySeconds ?? null)}</td>}
                        </tr>
                    ))}
                </tbody>
            </table>
            {rows.length > COLLAPSED_ROWS && (
                <button type="button" className="show-all" onClick={() => setShowAll(!showAll)}>
                    {showAll ? 'Show fewer' : `Show all ${rows.length} items`}
                </button>
            )}
            <p className="tier-note">
                Items with fewer than {MIN_BUYERS} buyers are not shown. Grey rows have fewer than {SMALL_SAMPLE}{' '}
                buyers, so their win rate is less reliable. Click a heading to sort.
            </p>
        </>
    )
}