import { BrowserRouter, Route, Routes, useParams } from 'react-router-dom'
import HeroPage from './HeroPage'
import TierListPage from './TierListPage'
import './App.css'

function HeroRoute() {
  const { heroId } = useParams()
  return <HeroPage key={heroId} />
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<TierListPage />} />
        <Route path="/hero/:heroId" element={<HeroRoute />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App