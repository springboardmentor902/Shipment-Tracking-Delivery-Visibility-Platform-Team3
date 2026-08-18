import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../services/api'
import TextField from '../components/TextField'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const notice = location.state?.notice

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: undefined }))
    setServerError('')
  }

  function validate() {
    const next = {}
    if (!form.email.trim()) next.email = 'Email is required'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) next.email = 'Enter a valid email address'
    if (!form.password) next.password = 'Password is required'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setSubmitting(true)
    setServerError('')
    try {
      await login({ email: form.email.trim(), password: form.password })
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true })
    } catch (error) {
      const status = error.response?.status
      if (status === 401) setServerError('Incorrect email or password.')
      else if (status === 403) setServerError('Your account is not active. Contact an administrator.')
      else setServerError(extractErrorMessage(error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">ShipTrack Pro</h1>
          <p className="mt-1 text-sm text-slate-500">Sign in to your account</p>
        </div>

        <form
          onSubmit={handleSubmit}
          noValidate
          className="space-y-5 rounded-2xl border border-slate-200 bg-white p-7 shadow-sm"
        >
          {notice && (
            <div className="rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">
              {notice}
            </div>
          )}

          {serverError && (
            <div role="alert" className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {serverError}
            </div>
          )}

          <TextField
            id="email"
            label="Email"
            type="email"
            value={form.email}
            onChange={handleChange}
            error={errors.email}
            placeholder="you@company.com"
            autoComplete="email"
          />

          <TextField
            id="password"
            label="Password"
            type={showPassword ? 'text' : 'password'}
            value={form.password}
            onChange={handleChange}
            error={errors.password}
            placeholder="••••••••"
            autoComplete="current-password"
            trailing={
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-500 hover:text-slate-700"
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            }
          />

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white
                       transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>

          <p className="text-center text-sm text-slate-500">
            New here?{' '}
            <Link to="/register" className="font-medium text-brand-600 hover:text-brand-700">
              Create an account
            </Link>
            <span className="mx-2 text-slate-300">·</span>
            <Link to="/track" className="font-medium text-brand-600 hover:text-brand-700">
              Track a shipment
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}
