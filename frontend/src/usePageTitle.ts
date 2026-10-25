import { useEffect } from 'react'

const SITE = 'Deadlock Meta Companion'

export function usePageTitle(title?: string) {
    useEffect(() => {
        document.title = title ? `${title} · ${SITE}` : SITE
    }, [title])
}