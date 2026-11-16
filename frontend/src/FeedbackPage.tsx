import { useState } from 'react'
import type { FormEvent } from 'react'
import { apiUrl } from './api'
import { usePageTitle } from './usePageTitle'

const MAX_LENGTH = 1000

type Status = 'idle' | 'sending' | 'done' | 'limit' | 'invalid' | 'error'

export default function FeedbackPage() {
    usePageTitle('Feedback')
    const [rating, setRating] = useState<number | null>(null)
    const [message, setMessage] = useState('')
    const [website, setWebsite] = useState('')
    const [status, setStatus] = useState<Status>('idle')

    const submit = async (event: FormEvent) => {
        event.preventDefault()
        if (status === 'sending' || message.trim() === '') return
        setStatus('sending')
        try {
            const response = await fetch(apiUrl('/api/feedback'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rating, message, website }),
            })
            if (response.status === 201) setStatus('done')
            else if (response.status === 429) setStatus('limit')
            else if (response.status === 400) setStatus('invalid')
            else setStatus('error')
        } catch {
            setStatus('error')
        }
    }

    return (
        <main className="page">
            <div className="feedback-wrap">
                <h1>Feedback</h1>
                <p className="subtitle">
                    Tell me what works, what is confusing or what you would like to see. Messages are private:
                    only I can read them, and I use them to improve the site.
                </p>

                {status === 'done' ? (
                    <div className="feedback-done" role="status">
                        <h2>Thank you</h2>
                        <p>Your message was sent.</p>
                    </div>
                ) : (
                    <form className="feedback-form" onSubmit={submit}>
                        <fieldset className="feedback-rating">
                            <legend>
                                How useful is the site? <span className="feedback-optional">(optional)</span>
                            </legend>
                            <div className="rank-chips" role="group" aria-label="Rating from 1 to 5">
                                {[1, 2, 3, 4, 5].map((n) => (
                                    <button
                                        key={n}
                                        type="button"
                                        className={rating === n ? 'rank-chip active' : 'rank-chip'}
                                        aria-pressed={rating === n}
                                        aria-label={`${n} out of 5`}
                                        onClick={() => setRating(rating === n ? null : n)}
                                    >
                                        {n}
                                    </button>
                                ))}
                            </div>
                            <p className="feedback-hint">1 is not useful, 5 is very useful. Click again to clear.</p>
                        </fieldset>

                        <div>
                            <label htmlFor="feedback-message" className="feedback-label">
                                Your message
                            </label>
                            <textarea
                                id="feedback-message"
                                className="feedback-textarea"
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                maxLength={MAX_LENGTH}
                                rows={7}
                                required
                            />
                            <div className="feedback-count">
                                {message.length}/{MAX_LENGTH}
                            </div>
                        </div>

                        {/* People never see this field; a bot that fills it is ignored by the server. */}
                        <div className="feedback-hp" aria-hidden="true">
                            <label>
                                Website
                                <input
                                    type="text"
                                    name="website"
                                    tabIndex={-1}
                                    autoComplete="off"
                                    value={website}
                                    onChange={(e) => setWebsite(e.target.value)}
                                />
                            </label>
                        </div>

                        {status === 'limit' && (
                            <p className="feedback-error" role="alert">
                                You have sent several messages recently. Please try again later.
                            </p>
                        )}
                        {status === 'invalid' && (
                            <p className="feedback-error" role="alert">
                                Please write a message of up to {MAX_LENGTH} characters.
                            </p>
                        )}
                        {status === 'error' && (
                            <p className="feedback-error" role="alert">
                                Something went wrong and your message was not sent. Please try again later.
                            </p>
                        )}

                        <div>
                            <button
                                type="submit"
                                className="button button-primary"
                                disabled={status === 'sending' || message.trim() === ''}
                            >
                                {status === 'sending' ? 'Sending...' : 'Send feedback'}
                            </button>
                        </div>

                        <p className="feedback-hint">
                            Please do not include personal details such as your name or email. Messages are stored
                            without your address and are part of the database backups. I cannot reply, because no
                            contact details are collected.
                        </p>
                    </form>
                )}
            </div>
        </main>
    )
}