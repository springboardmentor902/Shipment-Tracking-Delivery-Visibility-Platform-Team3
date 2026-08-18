import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ROLE_OPTIONS } from '../services/authService'
import { extractErrorMessage } from '../services/api'
import TextField from '../components/TextField'

const INITIAL = {
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  role: 'CUSTOMER',
}

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState(INITIAL)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const selectedRole = ROLE_OPTIONS.find((option) => option.value === form.role)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: undefined }))
    setServerError('')
  }

  function validate() {
    const next = {}

    if (!form.fullName.trim()) next.fullName = 'Full name is required'
    else if (form.fullName.trim().length < 3) next.fullName = 'Full name must be at least 3 characters'

    if (!form.email.trim()) next.email = 'Email is required'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) next.email = 'Enter a valid email address'

    if (form.phone && !/^[0-9+\-\s()]{7,20}$/.test(form.phone))
      next.phone = 'Enter a valid phone number'

    if (!form.password) next.password = 'Password is required'
    else if (form.password.length < 8) next.password = 'Password must be at least 8 characters'

    if (form.confirmPassword !== form.password) next.confirmPassword = 'Passwords do not match'

    if (!form.role) next.role = 'Select a role'

    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setSubmitting(true)
    setServerError('')
    try {
      // confirmPassword is a UI-only field — the backend never sees it.
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        password: form.password,
        role: form.role,
      })
      navigate('/login', {
        replace: true,
        state: { notice: 'Account created. Please sign in.' },
      })
    } catch (error) {
      const status = error.response?.status
      if (status === 409) setErrors((prev) => ({ ...prev, email: 'This email is already registered' }))
      else if (status === 403) setServerError('That role cannot be self-registered.')
      else setServerError(extractErrorMessage(error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Create your account</h1>
          <p className="mt-1 text-sm text-slate-500">Join ShipTrack Pro in a few seconds</p>
        </div>

        <form
          onSubmit={handleSubmit}
          noValidate
          className="space-y-5 rounded-2xl border border-slate-200 bg-white p-7 shadow-sm"
        >
          {serverError && (
            <div role="alert" className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {serverError}
            </div>
          )}

          <TextField
            id="fullName"
            label="Full name"
            value={form.fullName}
            onChange={handleChange}
            error={errors.fullName}
            placeholder="Jayesh Jaiswal"
            autoComplete="name"
          />

          <div className="grid gap-5 sm:grid-cols-2">
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
              id="phone"
              label="Phone"
              value={form.phone}
              onChange={handleChange}
              error={errors.phone}
              placeholder="9876543210"
              autoComplete="tel"
            />
          </div>

          <div>
            <label htmlFor="role" className="mb-1.5 block text-sm font-medium text-slate-700">
              I am a
            </label>
            <select
              id="role"
              name="role"
              value={form.role}
              onChange={handleChange}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm
                         outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
            >
              {ROLE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {selectedRole && <p className="mt-1.5 text-xs text-slate-500">{selectedRole.hint}</p>}
            {errors.role && <p className="mt-1.5 text-xs text-red-600">{errors.role}</p>}
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              id="password"
              label="Password"
              type={showPassword ? 'text' : 'password'}
              value={form.password}
              onChange={handleChange}
              error={errors.password}
              placeholder="At least 8 characters"
              autoComplete="new-password"
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
            <TextField
              id="confirmPassword"
              label="Confirm password"
              type={showPassword ? 'text' : 'password'}
              value={form.confirmPassword}
              onChange={handleChange}
              error={errors.confirmPassword}
              placeholder="Re-enter password"
              autoComplete="new-password"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white
                       transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Creating account…' : 'Create account'}
          </button>

          <p className="text-center text-sm text-slate-500">
            Already registered?{' '}
            <Link to="/login" className="font-medium text-brand-600 hover:text-brand-700">
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}
