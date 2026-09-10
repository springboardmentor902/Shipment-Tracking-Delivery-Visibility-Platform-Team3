import { useCallback, useEffect, useState } from 'react'
import AppLayout from '../components/AppLayout'
import TextField from '../components/TextField'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../services/api'
import { businessAccountService } from '../services/businessAccountService'

const EMPTY_FORM = {
  companyName: '',
  gstNumber: '',
  contactPerson: '',
  contactPhone: '',
  billingAddress: '',
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16)
  return date.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}

export default function BusinessAccount() {
  const { user } = useAuth()
  const isBusinessClient = user?.role === 'BUSINESS_CLIENT'
  const isAdmin = user?.role === 'ADMINISTRATOR'
  const [form, setForm] = useState(EMPTY_FORM)
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(isBusinessClient)
  const [accounts, setAccounts] = useState([])
  const [accountsLoading, setAccountsLoading] = useState(isAdmin)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const loadOwnAccount = useCallback(async () => {
    if (!isBusinessClient) return
    setLoading(true)
    setError('')
    try {
      const currentAccount = await businessAccountService.getMine()
      setAccount(currentAccount)
      setForm({
        companyName: currentAccount.companyName || '',
        gstNumber: currentAccount.gstNumber || '',
        contactPerson: currentAccount.contactPerson || '',
        contactPhone: currentAccount.contactPhone || '',
        billingAddress: currentAccount.billingAddress || '',
      })
    } catch (err) {
      if (err.response?.status === 404) {
        setAccount(null)
        setForm(EMPTY_FORM)
      } else {
        setError(extractErrorMessage(err, 'Could not load your business account.'))
      }
    } finally {
      setLoading(false)
    }
  }, [isBusinessClient])

  const loadAccounts = useCallback(async () => {
    if (!isAdmin) return
    setAccountsLoading(true)
    try {
      setAccounts(await businessAccountService.list())
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not load business accounts.'))
    } finally {
      setAccountsLoading(false)
    }
  }, [isAdmin])

  useEffect(() => {
    loadOwnAccount()
  }, [loadOwnAccount])

  useEffect(() => {
    loadAccounts()
  }, [loadAccounts])

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
    setError('')
    setNotice('')
  }

  function validate() {
    if (!form.companyName.trim() || !form.contactPerson.trim() || !form.contactPhone.trim() || !form.billingAddress.trim()) {
      setError('Company name, contact person, phone, and billing address are required.')
      return false
    }
    return true
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setSubmitting(true)
    setError('')
    setNotice('')
    const payload = {
      companyName: form.companyName.trim(),
      gstNumber: form.gstNumber.trim(),
      contactPerson: form.contactPerson.trim(),
      contactPhone: form.contactPhone.trim(),
      billingAddress: form.billingAddress.trim(),
    }
    try {
      const saved = account
        ? await businessAccountService.updateMine(payload)
        : await businessAccountService.create(payload)
      setAccount(saved)
      setNotice(account ? 'Business account updated.' : 'Business account created.')
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not save the business account.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppLayout>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-900">Business account</h1>
        <p className="mt-1 text-sm text-slate-500">
          {isBusinessClient ? 'Keep your company billing details current.' : 'Review registered business accounts.'}
        </p>
      </div>

      {error && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
          {error}
        </div>
      )}
      {notice && (
        <div className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700">{notice}</div>
      )}

      {isBusinessClient && (
        <section className="max-w-2xl rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
            {account ? 'Company details' : 'Create business account'}
          </h2>
          {loading ? (
            <p className="text-sm text-slate-500">Loading business account…</p>
          ) : (
            <form onSubmit={handleSubmit} noValidate className="space-y-5">
              <TextField id="companyName" label="Company name" value={form.companyName} onChange={handleChange} />
              <TextField id="gstNumber" label="GST number" value={form.gstNumber} onChange={handleChange} />
              <div className="grid gap-5 sm:grid-cols-2">
                <TextField id="contactPerson" label="Contact person" value={form.contactPerson} onChange={handleChange} />
                <TextField id="contactPhone" label="Contact phone" value={form.contactPhone} onChange={handleChange} />
              </div>
              <TextField id="billingAddress" label="Billing address" value={form.billingAddress} onChange={handleChange} />
              <button
                type="submit"
                disabled={submitting}
                className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {submitting ? 'Saving…' : account ? 'Save changes' : 'Create business account'}
              </button>
            </form>
          )}
        </section>
      )}

      {isAdmin && (
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
          <div className="border-b border-slate-200 px-6 py-4">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">All business accounts</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-medium">Company</th>
                  <th className="px-4 py-3 font-medium">GST number</th>
                  <th className="px-4 py-3 font-medium">Contact</th>
                  <th className="px-4 py-3 font-medium">Owner</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {accountsLoading && (
                  <tr>
                    <td colSpan={6} className="px-4 py-10 text-center text-slate-500">
                      Loading business accounts…
                    </td>
                  </tr>
                )}
                {!accountsLoading && accounts.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-10 text-center text-slate-500">
                      No business accounts have been created yet.
                    </td>
                  </tr>
                )}
                {!accountsLoading &&
                  accounts.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-medium text-slate-900">{item.companyName}</td>
                      <td className="px-4 py-3 text-slate-500">{item.gstNumber || '—'}</td>
                      <td className="px-4 py-3 text-slate-700">
                        <p>{item.contactPerson}</p>
                        <p className="mt-1 text-xs text-slate-500">{item.contactPhone}</p>
                      </td>
                      <td className="px-4 py-3 text-slate-500">{item.ownerName}</td>
                      <td className="px-4 py-3">
                        <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700">
                          {item.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-500">{formatDateTime(item.createdAt)}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </AppLayout>
  )
}
