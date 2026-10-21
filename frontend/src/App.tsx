import { BrowserRouter, Route, Routes, useParams } from 'react-router-dom'
import HeroPage from './HeroPage'
import LandingPage from './LandingPage'
import Nav from './Nav'
import ScrollToHash from './ScrollToHash'
import TierListPage from './TierListPage'
import './App.css'

function HeroRoute() {
  const { heroId } = useParams()
  return <HeroPage key={heroId} />
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