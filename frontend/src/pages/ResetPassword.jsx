import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import TextField from '../components/TextField'
import { extractErrorMessage } from '../services/api'
import { authService } from '../services/authService'
import PublicHeader from '../components/PublicHeader'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    token: searchParams.get('token') || '',
    newPassword: '',
    confirmPassword: '',
  })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
    setErrors((previous) => ({ ...previous, [name]: undefined }))
    setServerError('')
  }

  function validate() {
    const next = {}
    if (!form.token.trim()) next.token = 'Reset token is required'
    if (!form.newPassword) next.newPassword = 'New password is required'
    else if (form.newPassword.length < 8) next.newPassword = 'Password must be at least 8 characters'
    if (form.confirmPassword !== form.newPassword) next.confirmPassword = 'Passwords do not match'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setSubmitting(true)
    setServerError('')
    try {
      await authService.resetPassword({ token: form.token.trim(), newPassword: form.newPassword })
      navigate('/login', {
        replace: true,
        state: { notice: 'Password reset. Please sign in with your new password.' },
      })
    } catch (err) {
      setServerError(extractErrorMessage(err, 'Could not reset your password.'))
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
          <h1 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">Choose a new password</h1>
          <p className="mt-1 text-sm text-slate-500">Paste the reset token if it was not included in the link.</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-5 rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
          {serverError && (
            <div role="alert" className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {serverError}
            </div>
          )}
          <TextField
            id="token"
            label="Reset token"
            value={form.token}
            onChange={handleChange}
            error={errors.token}
            autoComplete="off"
          />
          <TextField
            id="newPassword"
            label="New password"
            type="password"
            value={form.newPassword}
            onChange={handleChange}
            error={errors.newPassword}
            placeholder="At least 8 characters"
            autoComplete="new-password"
          />
          <TextField
            id="confirmPassword"
            label="Confirm new password"
            type="password"
            value={form.confirmPassword}
            onChange={handleChange}
            error={errors.confirmPassword}
            autoComplete="new-password"
          />
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Resetting…' : 'Reset password'}
          </button>
        </form>
      </div>
      </main>
    </div>
  )
}
