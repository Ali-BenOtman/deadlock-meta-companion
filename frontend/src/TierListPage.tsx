import { useEffect, useState } from 'react'
import HeroTile from './HeroTile'
import RankFilter from './RankFilter'
import { rankNote, tierLabel, useRank } from './rank'
import type { HeroMeta, Tier } from './types'

const TIER_ORDER: Tier[] = ['S', 'A', 'B', 'C', 'D', 'NOT_ENOUGH_DATA']

function TierRows({ minTier }: { minTier: number }) {
  const [heroes, setHeroes] = useState<HeroMeta[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    fetch(`/api/meta?minTier=${minTier}`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`API returned ${response.status}`)
        }
        return response.json() as Promise<HeroMeta[]>
      })
      .then((data) => {
        if (!cancelled) setHeroes(data)
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
  }, [minTier])

  if (loading) return <p className="status">Loading...</p>
  if (error) return <p className="status">Failed to load: {error}</p>

  const matches = Math.round(heroes.reduce((sum, hero) => sum + hero.games, 0) / 12)
  const hasUnranked = heroes.some((hero) => hero.tier === 'NOT_ENOUGH_DATA')

  return (
    <>
      <p className="tier-note">
        Based on about {matches.toLocaleString()} ranked matches · {rankNote(minTier)}
      </p>
      {matches < 5000 && (
        <p className="tier-note">Small sample: tiers at this rank can change a lot from day to day.</p>
      )}
      {TIER_ORDER.map((tier) => {
        const inTier = heroes.filter((hero) => hero.tier === tier)
        if (inTier.length === 0) return null
        return (
          <section key={tier} className="tier-row">
            <div className={`tier-label tier-${tier}`}>{tierLabel(tier)}</div>
            <div className="tier-heroes">
              {inTier.map((hero) => (
                <HeroTile key={hero.heroId} hero={hero} />
              ))}
            </div>
          </section>
        )
      })}
      {hasUnranked && (
        <p className="tier-note">Heroes marked ? have fewer than 500 games at this rank, so they have no tier.</p>
      )}
    </>
  )
}

export default function TierListPage() {
  const { minTier } = useRank()

  return (
    <main className="page">
      <h1>Hero tier list</h1>
      <p className="subtitle">Hero tiers from the last 14 days of ranked matches</p>
      <RankFilter variant="select" />
      <TierRows key={minTier} minTier={minTier} />
    </main>
  )
}