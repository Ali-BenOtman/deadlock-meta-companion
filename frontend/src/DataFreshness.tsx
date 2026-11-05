import { useEffect, useState } from 'react'
import { apiUrl } from './api'
import type { DataStatus } from './types'

interface Freshness {
    text: string
    title: string
}

function ago(from: Date): string {
    const minutes = Math.max(0, Math.round((Date.now() - from.getTime()) / 60000))
    if (minutes < 1) return 'just now'
    if (minutes < 60) return `${minutes} min ago`
    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours} ${hours === 1 ? 'hour' : 'hours'} ago`
    const days = Math.floor(hours / 24)
    return `${days} ${days === 1 ? 'day' : 'days'} ago`
}

function describe(status: DataStatus): Freshness | null {
    if (!status.refreshedAt) return null
    const refreshed = new Date(status.refreshedAt)
    let title = `Refreshed ${refreshed.toLocaleString()}.`
    if (status.windowStart && status.windowEnd) {
        const from = new Date(status.windowStart).toLocaleDateString()
        const to = new Date(status.windowEnd).toLocaleDateString()
        title += ` Covers ${from} to ${to}.`
    }
    return { text: `Data last refreshed ${ago(refreshed)}`, title }
}

export default function DataFreshness() {
    const [freshness, setFreshness] = useState<Freshness | null>(null)

    useEffect(() => {
        let cancelled = false
        fetch(apiUrl('/api/status'))
            .then((response) => (response.ok ? (response.json() as Promise<DataStatus>) : null))
            .then((data) => {
                if (!cancelled && data) setFreshness(describe(data))
            })
            .catch(() => {
                // The line is an extra: if the call fails, show nothing.
            })
        return () => {
            cancelled = true
        }
    }, [])

    if (!freshness) return null
    return (
        <p className="tier-note" title={freshness.title}>
            {freshness.text}
        </p>
    )
}