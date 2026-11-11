import ItemIcon from './ItemIcon'
import type { ItemStat, Phase, PhaseItemStat } from './types'

// An item counts as "core" when at least this share of the hero's players buy it.
const CORE_MIN_BUY_RATE = 50
const ITEMS_PER_PHASE = 8

const PHASES: { key: Phase; label: string; range: string }[] = [
    { key: 'EARLY', label: 'Early', range: '0-10 min' },
    { key: 'MID', label: 'Mid', range: '10-20 min' },
    { key: 'LATE', label: 'Late', range: '20+ min' },
]

function formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60)
    const rest = seconds % 60
    return `${minutes}:${rest.toString().padStart(2, '0')}`
}

export default function QuickAnswers({
    items,
    phaseItems,
}: {
    items: ItemStat[]
    phaseItems: PhaseItemStat[] | null
}) {
    const core = items
        .filter((item) => item.buyRate >= CORE_MIN_BUY_RATE && item.medianBuySeconds !== null)
        .sort((a, b) => (a.medianBuySeconds ?? 0) - (b.medianBuySeconds ?? 0))

    const phases = PHASES.map((phase) => ({
        ...phase,
        top: (phaseItems ?? [])
            .filter((item) => item.phase === phase.key)
            .sort((a, b) => b.buyRate - a.buyRate)
            .slice(0, ITEMS_PER_PHASE),
    })).filter((phase) => phase.top.length > 0)

    if (core.length < 3 && phases.length === 0) return null

    return (
        <section className="hero-section">
            <h2>Quick answers</h2>
            <p className="subtitle">
                What most players buy, from real ranked matches. Popular is not the same as best: the table
                below shows win rates.
            </p>

            {core.length >= 3 && (
                <div className="qa-block">
                    <h3>
                        Core build
                        <span className="qa-hint">
                            bought by more than half of players, in the order they usually buy them
                        </span>
                    </h3>
                    <ol className="qa-row">
                        {core.map((item) => (
                            <li
                                key={item.itemId}
                                className="qa-tile"
                                title={`${item.itemName}: bought by ${item.buyRate.toFixed(0)}% of players, usually around ${formatTime(item.medianBuySeconds ?? 0)}`}
                            >
                                <ItemIcon url={item.imageUrl} name={item.itemName} size={48} />
                                <span className="qa-name">{item.itemName}</span>
                                <span className="qa-sub">~{formatTime(item.medianBuySeconds ?? 0)}</span>
                                <span className="qa-pct">{item.buyRate.toFixed(0)}%</span>
                            </li>
                        ))}
                    </ol>
                </div>
            )}

            {phases.map((phase) => (
                <div className="qa-block" key={phase.key}>
                    <h3>
                        {phase.label}
                        <span className="qa-hint">
                            share of players who first buy the item at {phase.range}
                        </span>
                    </h3>
                    <ul className="qa-row">
                        {phase.top.map((item) => (
                            <li
                                key={item.itemId}
                                className="qa-tile"
                                title={`${item.itemName}: ${item.buyRate.toFixed(0)}% of players first buy it in this phase`}
                            >
                                <ItemIcon url={item.imageUrl} name={item.itemName} size={48} />
                                <span className="qa-name">{item.itemName}</span>
                                <span className="qa-pct">{item.buyRate.toFixed(0)}%</span>
                            </li>
                        ))}
                    </ul>
                </div>
            ))}
        </section>
    )
}