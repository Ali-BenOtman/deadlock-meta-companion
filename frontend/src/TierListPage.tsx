import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { apiUrl } from './api'
import HeroTile from './HeroTile'
import RankFilter from './RankFilter'
import { rankNote, tierLabel, useRank } from './rank'
import { SORTS, biggestMovers, sortHeroes, useTierTools } from './tierTools'
import type { SortKey } from './tierTools'
import type { HeroMeta, Tier } from './types'

const TIER_ORDER: Tier[] = ['S', 'A', 'B', 'C', 'D', 'NOT_ENOUGH_DATA']

function Movers({ heroes }: { heroes: HeroMeta[] }) {
  const { search } = useLocation()
  const movers = biggestMovers(heroes)
  if (movers.length === 0) return null

  return (
    <section className="movers" aria-label="Biggest movers">
      <h2>Biggest movers this week</h2>
      <div className="movers-grid">
        {movers.map((hero) => {
          const change = hero.winRateChange ?? 0
          const rising = change > 0
          return (
            <Link
              key={hero.heroId}
              className="mover"
              to={{ pathname: `/hero/${hero.heroId}`, search }}
            >
              {hero.imageUrl ? (
                <img src={hero.imageUrl} alt={hero.heroName} width={44} height={44} />
              ) : (
                <div className="tile-fallback" aria-hidden="true">
                  {hero.heroName.charAt(0)}
                </div>
              )}
              <div className="mover-text">
                <span className="mover-name">{hero.heroName}</span>
                <span className={rising ? 'mover-change trend-rising' : 'mover-change trend-falling'}>
                  {rising ? '▲ +' : '▼ '}
                  {change.toFixed(1)} pts
                </span>
              </div>
            </Link>
          )
        })}
      </div>
    </section>
  )
}

function TierRows({ minTier, q, sort }: { minTier: number; q: string; sort: SortKey }) {
  const [heroes, setHeroes] = useState<HeroMeta[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    fetch(apiUrl(`/api/meta?minTier=${minTier}`))
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
  const needle = q.trim().toLowerCase()
  const visible = needle
    ? heroes.filter((hero) => hero.heroName.toLowerCase().includes(needle))
    : heroes
  const sortLabel = SORTS.find((s) => s.key === sort)?.label ?? ''

  return (
    <>
      <p className="tier-note">
        Based on about {matches.toLocaleString()} ranked matches · {rankNote(minTier)}
      </p>
      {matches < 5000 && (
        <p className="tier-note">Small sample: tiers at this rank can change a lot from day to day.</p>
      )}

      {!needle && <Movers heroes={heroes} />}

      {visible.length === 0 && <p className="status">No heroes match "{q}".</p>}

      {sort === 'tier' ? (
        TIER_ORDER.map((tier) => {
          const inTier = visible.filter((hero) => hero.tier === tier)
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
        })
      ) : (
        visible.length > 0 && (
          <>
            <p className="tier-note">Sorted by {sortLabel.toLowerCase()}</p>
            <div className="tier-heroes tier-flat">
              {sortHeroes(visible, sort).map((hero) => (
                <HeroTile key={hero.heroId} hero={hero} />
              ))}
            </div>
          </>
        )
      )}

      {hasUnranked && sort === 'tier' && (
        <p className="tier-note">Heroes marked ? have fewer than 500 games at this rank, so they have no tier.</p>
      )}
    </>
  )
}

export default function TierListPage() {
  const { minTier } = useRank()
  const { q, sort, setQ, setSort } = useTierTools()

  return (
    <main className="page">
      <h1>Hero tier list</h1>
      <p className="subtitle">Hero tiers from the last 14 days of ranked matches</p>
      <div className="tier-controls">
        <RankFilter variant="select" />
        <label className="search-box">
          <input
            type="search"
            value={q}
            placeholder="Search heroes"
            aria-label="Search heroes"
            onChange={(e) => setQ(e.target.value)}
          />
        </label>
        <label className="sort-select">
          Sort
          <select value={sort} onChange={(e) => setSort(e.target.value as SortKey)}>
            {SORTS.map((s) => (
              <option key={s.key} value={s.key}>
                {s.label}
              </option>
            ))}
          </select>
        </label>
      </div>
      <TierRows key={minTier} minTier={minTier} q={q} sort={sort} />
    </main>
  )
}