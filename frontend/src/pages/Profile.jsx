import { useEffect, useState } from 'react'
import AppLayout from '../components/AppLayout'
import TextField from '../components/TextField'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../services/api'
import { userService } from '../services/userService'

export default function Profile() {
  const { user, updateCurrentUser } = useAuth()

  const [form, setForm] = useState({
    fullName: '',
    phone: '',
  })

  const [loading, setLoading] = useState(true)
  const [editingProfile, setEditingProfile] = useState(false)
  const [savingProfile, setSavingProfile] = useState(false)

  const [profileError, setProfileError] = useState('')
  const [profileNotice, setProfileNotice] = useState('')

  // =========================================================
  // LOAD PROFILE
  // =========================================================

  useEffect(() => {
    if (!user?.id) {
      setLoading(false)
      return
    }

    async function loadProfile() {
      try {
        setLoading(true)
        setProfileError('')

        const currentUser = await userService.getMe(user.id)

        updateCurrentUser(currentUser)

        setForm({
          fullName: currentUser.fullName || '',
          phone: currentUser.phone || '',
        })
      } catch (err) {
        setProfileError(
          extractErrorMessage(
            err,
            'Could not load your profile.'
          )
        )
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [user?.id, updateCurrentUser])

  // =========================================================
  // HANDLE INPUT
  // =========================================================

  function handleProfileChange(event) {
    const { name, value } = event.target

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }))

    setProfileError('')
    setProfileNotice('')
  }

  // =========================================================
  // START EDITING
  // =========================================================

  function handleEdit() {
    setEditingProfile(true)
    setProfileError('')
    setProfileNotice('')
  }

  // =========================================================
  // CANCEL EDITING
  // =========================================================

  function handleCancel() {
    setForm({
      fullName: user?.fullName || '',
      phone: user?.phone || '',
    })

    setEditingProfile(false)
    setProfileError('')
    setProfileNotice('')
  }

  // =========================================================
  // SAVE PROFILE
  // =========================================================

  async function handleProfileSubmit(event) {
  event.preventDefault()

  if (!user?.id) return

  setSavingProfile(true)
  setProfileError('')
  setProfileNotice('')

  try {
    const updatedUser = await userService.updateMe(user.id, {
      fullName: form.fullName.trim(),
      phone: form.phone.trim(),
    })

    updateCurrentUser(updatedUser)
    setEditingProfile(false)
    setProfileNotice('Profile saved successfully.')
  } catch (error) {
    setProfileError(
      extractErrorMessage(error, 'Unable to save profile.')
    )
  } finally {
    setSavingProfile(false)
  }
}
  

  // =========================================================
  // RENDER
  // =========================================================

  return (
    <AppLayout>

      {/* PAGE HEADER */}
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-900">
          Profile
        </h1>

        <p className="mt-1 text-sm text-slate-500">
          Manage your account details and review your account information.
        </p>
      </div>

      {/* MAIN GRID */}
      <div className="grid gap-6 lg:grid-cols-2">

        {/* ===================================================
            ACCOUNT DETAILS
        =================================================== */}

        <section className="rounded-xl border border-slate-200 bg-white p-6">

          <div className="mb-4 flex items-center justify-between">

            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Account Details
            </h2>

            {!loading && !editingProfile && (
              <button
                type="button"
                onClick={handleEdit}
                className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
              >
                Edit Profile
              </button>
            )}

          </div>

          {/* ERROR */}
          {profileError && (
            <div
              role="alert"
              className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700"
            >
              {profileError}
            </div>
          )}

          {/* SUCCESS */}
          {profileNotice && (
            <div className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">
              {profileNotice}
            </div>
          )}

          {/* LOADING */}
          {loading ? (
            <p className="text-sm text-slate-500">
              Loading profile...
            </p>
          ) : (

            <form
              onSubmit={handleProfileSubmit}
              noValidate
              className="space-y-5"
            >

              {/* FULL NAME */}
              <TextField
                id="fullName"
                label="Full name"
                value={form.fullName}
                onChange={handleProfileChange}
                disabled={!editingProfile}
              />

              {/* PHONE */}
              <TextField
                id="phone"
                label="Phone"
                value={form.phone}
                onChange={handleProfileChange}
                autoComplete="tel"
                disabled={!editingProfile}
              />

              {/* EMAIL - READ ONLY */}
              <TextField
                id="email"
                label="Email"
                value={user?.email || ''}
                disabled
              />

              {/* ROLE */}
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Role
                </label>

                <input
                  type="text"
                  value={
                    user?.role
                      ? user.role.replaceAll('_', ' ')
                      : ''
                  }
                  disabled
                  className="w-full rounded-lg border border-slate-300 bg-slate-100 px-3 py-2.5 text-sm text-slate-500"
                />
              </div>

              {/* STATUS */}
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Account status
                </label>

                <input
                  type="text"
                  value={user?.status || 'ACTIVE'}
                  disabled
                  className="w-full rounded-lg border border-slate-300 bg-slate-100 px-3 py-2.5 text-sm text-slate-500"
                />
              </div>

              {/* EDIT MODE BUTTONS */}
              {editingProfile && (
                <div className="flex gap-3">

                  <button
                    type="submit"
                    disabled={savingProfile}
                    className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {savingProfile
                      ? 'Saving...'
                      : 'Save Changes'}
                  </button>

                  <button
                    type="button"
                    onClick={handleCancel}
                    disabled={savingProfile}
                    className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                  >
                    Cancel
                  </button>

                </div>
              )}

            </form>
          )}

        </section>


        {/* ===================================================
            ACCOUNT INFORMATION
        =================================================== */}

        <section className="rounded-xl border border-slate-200 bg-white p-6">

          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Account Information
          </h2>

          <div className="space-y-4">

            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                Customer ID
              </p>

              <p className="mt-1 text-sm text-slate-700">
                {user?.id || '—'}
              </p>
            </div>

            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                Email
              </p>

              <p className="mt-1 text-sm text-slate-700">
                {user?.email || '—'}
              </p>
            </div>

            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                Role
              </p>

              <p className="mt-1 text-sm text-slate-700">
                {user?.role
                  ? user.role.replaceAll('_', ' ')
                  : '—'}
              </p>
            </div>

            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">
                Account Status
              </p>

              <p className="mt-1 text-sm text-slate-700">
                {user?.status || 'ACTIVE'}
              </p>
            </div>

          </div>

        </section>

      </div>

    </AppLayout>
  )
}