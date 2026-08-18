import { useState } from 'react'
import { Link } from 'react-router-dom'
import TextField from '../components/TextField'
import { extractErrorMessage } from '../services/api'
import { authService } from '../services/authService'
import PublicHeader from '../components/PublicHeader'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [response, setResponse] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    if (!email.trim()) {
      setError('Email is required')
      return
    }

    setSubmitting(true)
    setError('')
    setResponse(null)
    try {
      setResponse(await authService.forgotPassword({ email: email.trim() }))
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not start the password reset.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen">
      <PublicHeader />
      <main className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Link to="/login" className="text-sm font-medium text-brand-600 hover:text-brand-700">
            ShipTrack Pro
          </Link>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">Reset your password</h1>
          <p className="mt-1 text-sm text-slate-500">Enter your email to receive a reset token.</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-5 rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
          {error && (
            <div role="alert" className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {error}
            </div>
          )}
          {response && (
            <div className="rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">
              <p>{response.message || 'Password reset token created.'}</p>
              {response.token && (
                <p className="mt-2 break-all text-xs">
                  Reset token: <span className="font-mono">{response.token}</span>
                </p>
              )}
            </div>
          )}

          <TextField
            id="email"
            label="Email"
            type="email"
            value={email}
            onChange={(event) => {
              setEmail(event.target.value)
              setError('')
            }}
            placeholder="you@company.com"
            autoComplete="email"
          />

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Creating token…' : 'Get reset token'}
          </button>

          <p className="text-center text-sm text-slate-500">
            Have a token?{' '}
            <Link to="/reset-password" className="font-medium text-brand-600 hover:text-brand-700">
              Reset password
            </Link>
          </p>
        </form>
      </div>
      </main>
    </div>
  )
}
