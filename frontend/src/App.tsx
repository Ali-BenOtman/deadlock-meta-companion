import { BrowserRouter, Route, Routes, useParams } from 'react-router-dom'
import HeroPage from './HeroPage'
import LandingPage from './LandingPage'
import Nav from './Nav'
import ScrollToHash from './ScrollToHash'
import TierListPage from './TierListPage'
import './App.css'
import { useRank } from './rank'

function HeroRoute() {
  const { heroId } = useParams()
  const { minTier } = useRank()
  return <HeroPage key={`${heroId}-${minTier}`} />
}

function App() {
  return (
    <BrowserRouter>
      <ScrollToHash />
      <Nav />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/tier-list" element={<TierListPage />} />
        <Route path="/hero/:heroId" element={<HeroRoute />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App