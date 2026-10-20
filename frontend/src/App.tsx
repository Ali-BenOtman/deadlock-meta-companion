import { useEffect, useState } from 'react'
import type { HeroMeta } from './types'

function formatChange(change: number | null): string {
  if (change === null) return 'n/a'
  const sign = change > 0 ? '+' : ''
  return `${sign}${change.toFixed(2)}`
}

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

  if (loading) return <p>Loading...</p>
  if (error) return <p>Failed to load: {error}</p>

  return (
    <main>
      <h1>Deadlock Meta</h1>
      <ul>
        {heroes.map((hero) => (
          <li key={hero.heroId}>
            {hero.tier} | {hero.heroName} | {hero.smoothedWinRate.toFixed(2)}% |{' '}
            {hero.trend} | {formatChange(hero.winRateChange)}
          </li>
        ))}
      </ul>
    </main>
  )
}

export default App