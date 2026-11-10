import { useState } from 'react'
import type { DailyStat } from './types'

const WIDTH = 640
const HEIGHT = 240
const PAD = { left: 44, right: 14, top: 14, bottom: 28 }
const MIN_GAMES = 500
const TIP_W = 136
const TIP_H = 36

function formatDay(day: string): string {
    return new Date(`${day}T00:00:00Z`).toLocaleDateString('en-GB', {
        day: 'numeric',
        month: 'short',
        timeZone: 'UTC',
    })
}

export default function WinRateChart({ days }: { days: DailyStat[] }) {
    const [hover, setHover] = useState<number | null>(null)

    if (days.length < 2) return null

    const rates = days.map((d) => d.winRate)
    // The range always includes 50%, the break-even win rate, with a little room around the data.
    const lo = Math.floor(Math.min(50, ...rates) - 1)
    const hi = Math.ceil(Math.max(50, ...rates) + 1)
    const step = hi - lo <= 6 ? 1 : hi - lo <= 12 ? 2 : 5
    const ticks: number[] = []
    for (let t = Math.ceil(lo / step) * step; t <= hi; t += step) ticks.push(t)

    const x = (i: number) => PAD.left + (i * (WIDTH - PAD.left - PAD.right)) / (days.length - 1)
    const y = (v: number) => PAD.top + ((hi - v) * (HEIGHT - PAD.top - PAD.bottom)) / (hi - lo)
    // The first day starts partway into the window and the last is still filling up.
    const isFaint = (i: number) => i === 0 || i === days.length - 1 || days[i].games < MIN_GAMES

    const labelIdx = Array.from(
        new Set([0, 1, 2, 3, 4].map((k) => Math.round((k * (days.length - 1)) / 4))),
    )
    const summary = `Daily win rate for the last ${days.length} days, between ${Math.min(...rates).toFixed(1)}% and ${Math.max(...rates).toFixed(1)}%.`

    const active = hover !== null && hover < days.length ? hover : null
    const tip = active !== null ? days[active] : null
    let tipX = 0
    let tipY = 0
    if (tip && active !== null) {
        tipX = Math.min(Math.max(x(active) - TIP_W / 2, 2), WIDTH - TIP_W - 2)
        const above = y(tip.winRate) - TIP_H - 10
        tipY = above >= 0 ? above : y(tip.winRate) + 12
    }

    return (
        <>
            <svg className="win-chart" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={summary}>
                {ticks.map((t) => (
                    <g key={t}>
                        <line
                            className={t === 50 ? 'even' : 'grid'}
                            x1={PAD.left}
                            x2={WIDTH - PAD.right}
                            y1={y(t)}
                            y2={y(t)}
                        />
                        <text x={PAD.left - 6} y={y(t) + 4} textAnchor="end">
                            {t}%
                        </text>
                    </g>
                ))}

                {days.slice(1).map((d, k) => {
                    const i = k + 1
                    const faint = isFaint(i - 1) || isFaint(i)
                    return (
                        <line
                            key={d.day}
                            className={faint ? 'line faint' : 'line'}
                            x1={x(i - 1)}
                            y1={y(days[i - 1].winRate)}
                            x2={x(i)}
                            y2={y(d.winRate)}
                        />
                    )
                })}

                {days.map((d, i) => (
                    <g key={d.day}>
                        <circle
                            className={isFaint(i) ? 'dot faint' : 'dot'}
                            cx={x(i)}
                            cy={y(d.winRate)}
                            r={active === i ? 5 : 3.5}
                        />
                        <circle
                            className="hit"
                            cx={x(i)}
                            cy={y(d.winRate)}
                            r={12}
                            fill="transparent"
                            tabIndex={0}
                            aria-label={`${formatDay(d.day)}: ${d.winRate.toFixed(1)}% win rate, ${d.games.toLocaleString()} games`}
                            onMouseEnter={() => setHover(i)}
                            onMouseLeave={() => setHover(null)}
                            onFocus={() => setHover(i)}
                            onBlur={() => setHover(null)}
                            onClick={() => setHover(i)}
                        />
                    </g>
                ))}

                {labelIdx.map((i) => (
                    <text
                        key={days[i].day}
                        x={x(i)}
                        y={HEIGHT - 8}
                        textAnchor={i === 0 ? 'start' : i === days.length - 1 ? 'end' : 'middle'}
                    >
                        {formatDay(days[i].day)}
                    </text>
                ))}

                {tip && (
                    <g className="tip" transform={`translate(${tipX} ${tipY})`}>
                        <rect className="tip-box" width={TIP_W} height={TIP_H} rx={6} />
                        <text className="tip-text" x={8} y={14}>
                            {formatDay(tip.day)}
                        </text>
                        <text className="tip-text" x={8} y={28}>
                            {`${tip.winRate.toFixed(1)}% · ${tip.games.toLocaleString()} games`}
                        </text>
                    </g>
                )}
            </svg>
            <p className="tier-note">
                Hollow dots are partial days (the first and last) or days with fewer than {MIN_GAMES} games. The
                grey dashed line is 50%, the break-even win rate.
            </p>
        </>
    )
}