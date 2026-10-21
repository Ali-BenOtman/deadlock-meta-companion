import { Link, NavLink } from 'react-router-dom'

export default function Nav() {
    return (
        <nav className="nav">
            <Link to="/" className="nav-brand">
                <span className="nav-logo" aria-hidden="true" />
                <span className="nav-title">Deadlock Meta</span>
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