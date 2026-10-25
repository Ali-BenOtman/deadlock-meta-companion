import { useState } from 'react'

export default function ItemIcon({
    url,
    name,
    size = 28,
}: {
    url?: string | null
    name: string
    size?: number
}) {
    const [failed, setFailed] = useState(false)

    if (!url || failed) {
        return (
            <span
                className="item-icon item-icon-fallback"
                style={{ width: size, height: size }}
                aria-hidden="true"
            >
                {name.charAt(0)}
            </span>
        )
    }

    return (
        <img
            className="item-icon"
            src={url}
            alt=""
            width={size}
            height={size}
            loading="lazy"
            onError={() => setFailed(true)}
        />
    )
}