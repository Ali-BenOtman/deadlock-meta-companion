import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

export default function ScrollToHash() {
    const { hash, pathname } = useLocation()

    useEffect(() => {
        if (hash) {
            document.getElementById(hash.slice(1))?.scrollIntoView()
        } else {
            window.scrollTo(0, 0)
        }
    }, [hash, pathname])

    return null
}