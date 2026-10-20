import { useEffect, useState } from 'react'
import HeroTile from './HeroTile'
import type { HeroMeta, Tier } from './types'
import './App.css'

const TIER_ORDER: Tier[] = ['S', 'A', 'B', 'C', 'D']

function App() {
  const [heroes, setHeroes] = useState<HeroMeta[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('/api/meta')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`API returned ${response.status}`)
        }
        return response.json() as Promise<HeroMeta[]>
      })
      .then(setHeroes)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="status">Loading...</p>
  if (error) return <p className="status">Failed to load: {error}</p>

  return (
    <main className="page">
      <h1>Deadlock Meta</h1>
      <p className="subtitle">Hero tiers from the last 14 days of ranked matches</p>
      {TIER_ORDER.map((tier) => {
        const inTier = heroes.filter((hero) => hero.tier === tier)
        if (inTier.length === 0) return null
        return (
          <section key={tier} className="tier-row">
            <div className={`tier-label tier-${tier}`}>{tier}</div>
            <div className="tier-heroes">
              {inTier.map((hero) => (
                <HeroTile key={hero.heroId} hero={hero} />
              ))}
            </div>
          </section>
        )
      })}
    </main>
  )
}

export default App