import { Link, NavLink } from 'react-router-dom'
import Logo from './Logo'

export default function Nav() {
    return (
        <nav className="nav">
            <Link to="/" className="nav-brand">
                <Logo size={30} />
                <span className="nav-title">Deadlock Meta Companion</span>
            </Link>
            <div className="nav-links">
                <NavLink to="/" end>
                    Home
                </NavLink>
                <NavLink to="/tier-list">Tier list</NavLink>
                <Link to={{ pathname: '/', hash: '#about' }}>About the data</Link>
            </div>
        </nav>
    )
}