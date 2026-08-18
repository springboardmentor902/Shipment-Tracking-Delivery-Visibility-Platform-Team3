import { useCallback, useEffect, useState } from 'react'
import AppLayout from '../components/AppLayout'
import TextField from '../components/TextField'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../services/api'
import { userService } from '../services/userService'

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16)
  return date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}

export default function Profile() {
  const { user, updateCurrentUser } = useAuth()
  const [form, setForm] = useState({ fullName: '', phone: '', profileImageUrl: '' })
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [profileNotice, setProfileNotice] = useState('')
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [passwordError, setPasswordError] = useState('')
  const [passwordNotice, setPasswordNotice] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)
  const [activity, setActivity] = useState(null)
  const [activityLoading, setActivityLoading] = useState(true)
  const [activityError, setActivityError] = useState('')
  const [activityPage, setActivityPage] = useState(0)

  const loadActivity = useCallback(async () => {
    setActivityLoading(true)
    setActivityError('')
    try {
      setActivity(await userService.activity({ page: activityPage, size: 10 }))
    } catch (err) {
      setActivityError(extractErrorMessage(err, 'Could not load account activity.'))
    } finally {
      setActivityLoading(false)
    }
  }, [activityPage])

  useEffect(() => {
    async function loadProfile() {
      setLoading(true)
      setProfileError('')
      try {
        const currentUser = await userService.getMe()
        updateCurrentUser(currentUser)
        setForm({
          fullName: currentUser.fullName || '',
          phone: currentUser.phone || '',
          profileImageUrl: currentUser.profileImageUrl || '',
        })
      } catch (err) {
        setProfileError(extractErrorMessage(err, 'Could not load your profile.'))
      } finally {
        setLoading(false)
      }
    }
    loadProfile()
  }, [updateCurrentUser])

  useEffect(() => {
    loadActivity()
  }, [loadActivity])

  function handleProfileChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
    setProfileError('')
    setProfileNotice('')
  }

  async function handleProfileSubmit(event) {
    event.preventDefault()
    if (!form.fullName.trim()) {
      setProfileError('Full name is required.')
      return
    }

    setSavingProfile(true)
    setProfileError('')
    setProfileNotice('')
    try {
      const updated = await userService.updateMe({
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        profileImageUrl: form.profileImageUrl.trim() || null,
      })
      updateCurrentUser(updated)
      setProfileNotice('Profile saved.')
    } catch (err) {
      setProfileError(extractErrorMessage(err, 'Could not save your profile.'))
    } finally {
      setSavingProfile(false)
    }
  }

  function handlePasswordChange(event) {
    const { name, value } = event.target
    setPasswordForm((previous) => ({ ...previous, [name]: value }))
    setPasswordError('')
    setPasswordNotice('')
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    if (!passwordForm.currentPassword || !passwordForm.newPassword) {
      setPasswordError('Current and new password are required.')
      return
    }
    if (passwordForm.newPassword.length < 8) {
      setPasswordError('New password must be at least 8 characters.')
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('New passwords do not match.')
      return
    }

    setSavingPassword(true)
    setPasswordError('')
    setPasswordNotice('')
    try {
      await userService.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      })
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setPasswordNotice('Password changed.')
    } catch (err) {
      setPasswordError(extractErrorMessage(err, 'Could not change your password.'))
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <AppLayout>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-900">Profile</h1>
        <p className="mt-1 text-sm text-slate-500">Manage your account details and review recent activity.</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Account details</h2>
          {profileError && (
            <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {profileError}
            </div>
          )}
          {profileNotice && (
            <div className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">{profileNotice}</div>
          )}
          {loading ? (
            <p className="text-sm text-slate-500">Loading profile…</p>
          ) : (
            <form onSubmit={handleProfileSubmit} noValidate className="space-y-5">
              <TextField id="fullName" label="Full name" value={form.fullName} onChange={handleProfileChange} />
              <TextField id="phone" label="Phone" value={form.phone} onChange={handleProfileChange} autoComplete="tel" />
              <TextField
                id="profileImageUrl"
                label="Profile image URL"
                type="url"
                value={form.profileImageUrl}
                onChange={handleProfileChange}
                placeholder="https://example.com/photo.jpg"
              />
              {form.profileImageUrl && (
                <img
                  src={form.profileImageUrl}
                  alt="Profile preview"
                  onError={(event) => {
                    event.currentTarget.style.display = 'none'
                  }}
                  className="h-16 w-16 rounded-full border border-slate-200 object-cover"
                />
              )}
              <p className="text-sm text-slate-500">
                {user?.email} · {user?.role?.replaceAll('_', ' ')}
              </p>
              <button
                type="submit"
                disabled={savingProfile}
                className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {savingProfile ? 'Saving…' : 'Save profile'}
              </button>
            </form>
          )}
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Change password</h2>
          {passwordError && (
            <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {passwordError}
            </div>
          )}
          {passwordNotice && (
            <div className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">{passwordNotice}</div>
          )}
          <form onSubmit={handlePasswordSubmit} noValidate className="space-y-5">
            <TextField
              id="currentPassword"
              label="Current password"
              type="password"
              value={passwordForm.currentPassword}
              onChange={handlePasswordChange}
              autoComplete="current-password"
            />
            <TextField
              id="newPassword"
              label="New password"
              type="password"
              value={passwordForm.newPassword}
              onChange={handlePasswordChange}
              autoComplete="new-password"
            />
            <TextField
              id="confirmPassword"
              label="Confirm new password"
              type="password"
              value={passwordForm.confirmPassword}
              onChange={handlePasswordChange}
              autoComplete="new-password"
            />
            <button
              type="submit"
              disabled={savingPassword}
              className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {savingPassword ? 'Changing…' : 'Change password'}
            </button>
          </form>
        </section>
      </div>

      <section className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Recent activity</h2>
        {activityError && (
          <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
            {activityError}
          </div>
        )}
        {activityLoading ? (
          <p className="text-sm text-slate-500">Loading activity…</p>
        ) : (
          <>
            <div className="divide-y divide-slate-100">
              {(activity?.content || []).map((item) => (
                <article key={item.id} className="py-3 text-sm">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="font-medium text-slate-900">{item.action?.replaceAll('_', ' ')}</p>
                    <time className="text-xs text-slate-500" dateTime={item.createdAt}>
                      {formatDateTime(item.createdAt)}
                    </time>
                  </div>
                  <p className="mt-1 text-slate-500">{item.detail || 'No additional details.'}</p>
                </article>
              ))}
              {!activity?.content?.length && <p className="py-6 text-sm text-slate-500">No account activity yet.</p>}
            </div>
            {activity && activity.totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
                <span>
                  Page {activity.number + 1} of {activity.totalPages}
                </span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setActivityPage((page) => Math.max(0, page - 1))}
                    disabled={activity.number === 0}
                    className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    onClick={() => setActivityPage((page) => page + 1)}
                    disabled={activity.number + 1 >= activity.totalPages}
                    className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </section>
    </AppLayout>
  )
}
