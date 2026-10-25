import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiUrl } from './api'
import type { HeroMeta } from './types'

const FEATURES = [
    {
        title: 'Tier list',
        text: 'Every hero ranked S to D by win rate, with weekly trend arrows.',
        to: '/tier-list',
    },
    {
        title: 'Hero pages',
        text: 'Win rate, pick rate and games for each hero, plus the best items and when players buy them.',
        to: '/tier-list',
    },
    {
        title: 'Starting builds',
        text: 'The most-played six-item builds for each hero, with win rate against the hero average.',
        to: '/tier-list',
    },
    {
        title: 'Updated every 3 hours',
        text: 'New ranked matches are collected automatically and every number is recalculated.',
        to: '/tier-list',
    },
]

const STEPS = [
    { title: 'Collect', text: 'New ranked matches are pulled from the community data lake every few hours.' },
    { title: 'Store', text: 'Matches, players and item purchases go into a database that keeps about three weeks.' },
    { title: 'Calculate', text: 'Win rates, tiers, trends and builds are precomputed so pages load instantly.' },
    { title: 'Serve', text: 'A cached API feeds this site, so a busy page never slows the numbers down.' },
]

const LIMITS = [
    {
        title: 'A sample, not every match',
        text: 'Matches are collected by the community Deadlock API, not by Valve, so the sample is not guaranteed to represent every player.',
    },
    {
        title: 'Not split by region',
        text: 'Stats are not broken down by server or region, so a hero that is strong in one region and weak in another looks average here.',
    },
    {
        title: 'Item win rates are correlational',
        text: 'Players who are ahead buy items sooner and win more, so a high item win rate is not proof the item is strong.',
    },
    {
        title: 'New heroes lag in tiers',
        text: 'Tiers use 14 days of games, so a hero that just released or got reworked takes a while to settle.',
    },
    {
        title: 'Trends need volume',
        text: 'An arrow needs 500 games in each of the last two weeks. Otherwise it shows a question mark.',
    },
    {
        title: 'Small samples are smoothed',
        text: 'Win rates are pulled toward 50% for heroes with few games, so tiers do not swing on a lucky streak.',
    },
]

export default function LandingPage() {
    const [heroes, setHeroes] = useState<HeroMeta[]>([])

    useEffect(() => {
        fetch(apiUrl('/api/meta'))
            .then((response) => (response.ok ? (response.json() as Promise<HeroMeta[]>) : []))
            .then(setHeroes)
            .catch(() => setHeroes([]))
    }, [])

    const topHeroes = heroes.filter((hero) => hero.tier === 'S').slice(0, 3)

    const stats = [
        { value: heroes.length > 0 ? String(heroes.length) : '–', label: 'heroes with a tier today' },
        { value: '14 days', label: 'rolling window of ranked matches' },
        { value: '3 hours', label: 'between automatic data updates' },
        { value: 'Ranked only', label: 'every match in the stats is a ranked game' },
    ]

    return (
        <main className="landing">
            <section className="landing-hero">
                <div className="landing-hero-text">
                    <p className="eyebrow">Ranked match stats, refreshed every 3 hours</p>
                    <h1 className="landing-title">Know what is winning in Deadlock, today.</h1>
                    <p className="landing-lead">
                        A hero tier list built from a large sample of real ranked matches, with the items and
                        builds that win for every hero.
                    </p>
                    <div className="landing-actions">
                        <Link to="/tier-list" className="button button-primary">
                            Open the tier list
                        </Link>
                    </div>
                </div>

                {topHeroes.length > 0 && (
                    <Link to="/tier-list" className="landing-preview">
                        <div className="preview-head">
                            <span className="tier-label tier-S preview-badge">S</span>
                            <span className="preview-title">Top tier heroes</span>
                        </div>
                        <div className="preview-grid">
                            {topHeroes.map((hero) => (
                                <div key={hero.heroId} className="preview-hero">
                                    {hero.imageUrl ? (
                                        <img src={hero.imageUrl} alt={hero.heroName} width={64} height={64} />
                                    ) : (
                                        <div className="tile-fallback" aria-hidden="true">
                                            {hero.heroName.charAt(0)}
                                        </div>
                                    )}
                                    <span className="preview-name">{hero.heroName}</span>
                                    <span className="preview-rate">{hero.winRate.toFixed(1)}% win rate</span>
                                </div>
                            ))}
                        </div>
                        <p className="preview-link">See the full tier list →</p>
                    </Link>
                )}
            </section>

            <section className="landing-stats" aria-label="Key numbers">
                {stats.map((stat) => (
                    <div key={stat.label} className="stat-card">
                        <div className="stat-value">{stat.value}</div>
                        <div className="stat-label">{stat.label}</div>
                    </div>
                ))}
            </section>

            <section className="landing-section" aria-label="What you get">
                <h2>What you get</h2>
                <div className="card-grid">
                    {FEATURES.map((feature) => (
                        <Link key={feature.title} to={feature.to} className="info-card">
                            <h3>{feature.title}</h3>
                            <p>{feature.text}</p>
                        </Link>
                    ))}
                </div>
            </section>

            <section className="landing-section" aria-label="How the numbers are made">
                <h2>How the numbers are made</h2>
                <div className="step-grid">
                    {STEPS.map((step, index) => (
                        <div key={step.title} className="step-card">
                            <div className="step-number">{index + 1}</div>
                            <h3>{step.title}</h3>
                            <p>{step.text}</p>
                        </div>
                    ))}
                </div>
            </section>

            <section id="about" className="landing-section" aria-label="About the data">
                <h2>Honest about the data</h2>
                <p className="landing-lead">
                    Stats come from the community Deadlock API's match data, not from Valve, and cover a sample of
                    ranked matches rather than every match played. A few things to keep in mind when you read them.
                </p>
                <div className="card-grid">
                    {LIMITS.map((limit) => (
                        <div key={limit.title} className="info-card static">
                            <h3>{limit.title}</h3>
                            <p>{limit.text}</p>
                        </div>
                    ))}
                </div>
            </section>

            <footer className="landing-footer">
                Not affiliated with Valve. Match data from the community Deadlock API data lake.
            </footer>
        </main>
    )
}