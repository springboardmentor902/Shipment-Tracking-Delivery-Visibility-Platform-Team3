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
  if (!value) {
    return '—'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return String(value).replace('T', ' ').slice(0, 16)
  }

  return date.toLocaleString([], {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

function getValue(item, ...keys) {
  for (const key of keys) {
    const value = key
      .split('.')
      .reduce((current, part) => current?.[part], item)

    if (value !== undefined && value !== null && value !== '') {
      return value
    }
  }

  return '—'
}

function getAccountOwner(item) {
  return getValue(
    item,
    'createdBy',
    'createdByName',
    'ownerName',
    'owner.name',
    'owner.email',
    'userName',
    'email'
  )
}

function getAccountStatus(item) {
  return getValue(item, 'status', 'accountStatus')
}

function getAccountAddress(item) {
  return getValue(
    item,
    'billingAddress',
    'businessAddress',
    'address'
  )
}

function getAccountCreatedDate(item) {
  return getValue(
    item,
    'createdAt',
    'created_at',
    'createdDate'
  )
}

function getAccountUpdatedDate(item) {
  return getValue(
    item,
    'updatedAt',
    'updated_at',
    'updatedDate'
  )
}

export default function BusinessAccount() {
  const { user } = useAuth()
  useEffect(() => {
  if (!user) return

  setForm((previous) => ({
    ...previous,
    contactPerson: user.fullName || '',
    contactPhone: user.phone || '',
  }))
}, [user])

  const isBusinessClient = user?.role === 'BUSINESS_CLIENT'
  const isAdmin =
    user?.role === 'ADMINISTRATOR' ||
    user?.role === 'ADMIN'

  const [form, setForm] = useState(EMPTY_FORM)
  const [account, setAccount] = useState(null)
  const [accounts, setAccounts] = useState([])

  const [loading, setLoading] = useState(isBusinessClient)
  const [accountsLoading, setAccountsLoading] = useState(isAdmin)

  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const loadOwnAccount = useCallback(async () => {
    if (!isBusinessClient) {
      return
    }

    setLoading(true)
    setError('')

    try {
      const currentAccount =
        await businessAccountService.getMine()

      setAccount(currentAccount)

      setForm({
        companyName: currentAccount?.companyName || '',
        gstNumber: currentAccount?.gstNumber || '',
        contactPerson: currentAccount?.contactPerson || '',
        contactPhone: currentAccount?.contactPhone || '',
        billingAddress:
          currentAccount?.billingAddress ||
          currentAccount?.businessAddress ||
          '',
      })
    } catch (err) {
      if (err.response?.status === 404) {
        setAccount(null)
        setForm(EMPTY_FORM)
      } else {
        setError(
          extractErrorMessage(
            err,
            'Could not load your business account.'
          )
        )
      }
    } finally {
      setLoading(false)
    }
  }, [isBusinessClient])

  const loadAccounts = useCallback(async () => {
    if (!isAdmin) {
      return
    }

    setAccountsLoading(true)
    setError('')

    try {
      const accountList =
        await businessAccountService.list()

      console.log(
        'FINAL BUSINESS ACCOUNT LIST:',
        accountList
      )

      setAccounts(
        Array.isArray(accountList)
          ? accountList
          : []
      )
    } catch (err) {
      console.error(
        'BUSINESS ACCOUNTS LOAD ERROR:',
        err
      )

      setAccounts([])

      setError(
        extractErrorMessage(
          err,
          'Could not load business accounts.'
        )
      )
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

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }))

    setError('')
    setNotice('')
  }

  function validate() {
  if (
    !form.companyName.trim() ||
    !form.gstNumber.trim() ||
    !form.billingAddress.trim()
  ) {
    setError(
      'Company name, GST number, and billing address are required.'
    )

    return false
  }

  return true
}

  async function handleSubmit(event) {
    event.preventDefault()

    if (!validate()) {
      return
    }

    setSubmitting(true)
    setError('')
    setNotice('')

   const payload = {
  companyName: form.companyName.trim(),
  gstNumber: form.gstNumber.trim(),
  billingAddress: form.billingAddress.trim(),
}

    try {
      let savedAccount

      if (account) {
        savedAccount =
          await businessAccountService.updateMine(
            payload
          )

        setNotice(
          'Business account updated successfully.'
        )
      } else {
        savedAccount =
          await businessAccountService.create(
            payload
          )

        setNotice(
          'Business account created successfully.'
        )
      }

      setAccount(savedAccount)

      setForm({
        companyName:
          savedAccount?.companyName ||
          payload.companyName,

        gstNumber:
          savedAccount?.gstNumber ||
          payload.gstNumber,

        contactPerson:
          savedAccount?.contactPerson ||
          payload.contactPerson,

        contactPhone:
          savedAccount?.contactPhone ||
          payload.contactPhone,

        billingAddress:
          savedAccount?.billingAddress ||
          payload.billingAddress,
      })

      if (isAdmin) {
        await loadAccounts()
      }
    } catch (err) {
      console.error(
        'BUSINESS ACCOUNT SAVE ERROR:',
        err
      )

      setError(
        extractErrorMessage(
          err,
          'Could not save the business account.'
        )
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppLayout>
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-900">
          Business Account
        </h1>

        <p className="mt-1 text-sm text-slate-500">
          {isBusinessClient
            ? 'Keep your company billing details current.'
            : 'Review all registered business accounts.'}
        </p>
      </div>

      {error && (
        <div
          role="alert"
          className="mb-4 rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700"
        >
          {error}
        </div>
      )}

      {notice && (
        <div
          role="status"
          className="mb-4 rounded-lg bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-700"
        >
          {notice}
        </div>
      )}

      {isBusinessClient && (
        <section className="mb-8 max-w-2xl rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
            {account
              ? 'Company Details'
              : 'Create Business Account'}
          </h2>

          {loading ? (
            <p className="text-sm text-slate-500">
              Loading business account…
            </p>
          ) : (
            <form
              onSubmit={handleSubmit}
              noValidate
              className="space-y-5"
            >
              <TextField
  id="contactPerson"
  name="contactPerson"
  label="Contact Person"
  value={form.contactPerson}
  readOnly
/>

              <TextField
                id="gstNumber"
                name="gstNumber"
                label="GST Number"
                value={form.gstNumber}
                onChange={handleChange}
              />

              <div className="grid gap-5 sm:grid-cols-2">
                <TextField
  id="contactPerson"
  name="contactPerson"
  label="Contact Person"
  value={form.contactPerson}
  readOnly
/>
                <TextField
                  id="contactPhone"
                  name="contactPhone"
                  label="Contact Phone"
                  value={form.contactPhone}
                  onChange={handleChange}
                />
              </div>

              <TextField
                id="billingAddress"
                name="billingAddress"
                label="Billing Address"
                value={form.billingAddress}
                onChange={handleChange}
              />

              <button
                type="submit"
                disabled={submitting}
                className="rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {submitting
                  ? 'Saving…'
                  : account
                    ? 'Save Changes'
                    : 'Create Business Account'}
              </button>
            </form>
          )}
        </section>
      )}

      {isAdmin && (
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
          <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                All Business Accounts
              </h2>

              <p className="mt-1 text-xs text-slate-400">
                Complete registered account information
              </p>
            </div>

            <button
              type="button"
              onClick={loadAccounts}
              disabled={accountsLoading}
              className="rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            >
              {accountsLoading
                ? 'Refreshing…'
                : 'Refresh'}
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-medium">
                    ID
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Company
                  </th>

                  <th className="px-4 py-3 font-medium">
                    GST Number
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Contact Person
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Phone
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Billing Address
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Created By
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Status
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Created
                  </th>

                  <th className="px-4 py-3 font-medium">
                    Updated
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {accountsLoading && (
                  <tr>
                    <td
                      colSpan={10}
                      className="px-4 py-10 text-center text-slate-500"
                    >
                      Loading business accounts…
                    </td>
                  </tr>
                )}

                {!accountsLoading &&
                  accounts.length === 0 && (
                    <tr>
                      <td
                        colSpan={10}
                        className="px-4 py-10 text-center text-slate-500"
                      >
                        No business accounts have been created yet.
                      </td>
                    </tr>
                  )}

                {!accountsLoading &&
                  accounts.map((item, index) => (
                    <tr
                      key={
                        item.id ||
                        item.accountId ||
                        index
                      }
                      className="hover:bg-slate-50"
                    >
                      <td className="whitespace-nowrap px-4 py-3 text-slate-500">
                        {getValue(
                          item,
                          'id',
                          'accountId'
                        )}
                      </td>

                      <td className="px-4 py-3 font-medium text-slate-900">
                        {getValue(
                          item,
                          'companyName',
                          'company_name'
                        )}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-slate-500">
                        {getValue(
                          item,
                          'gstNumber',
                          'gst_number'
                        )}
                      </td>

                      <td className="px-4 py-3 text-slate-700">
                        {getValue(
                          item,
                          'contactPerson',
                          'contact_person'
                        )}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">
                        {getValue(
                          item,
                          'contactPhone',
                          'contact_phone'
                        )}
                      </td>

                      <td className="min-w-64 max-w-sm px-4 py-3 text-slate-500">
                        {getAccountAddress(item)}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-slate-500">
                        {getAccountOwner(item)}
                      </td>

                      <td className="px-4 py-3">
                        <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700">
                          {getAccountStatus(item)}
                        </span>
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-slate-500">
                        {formatDateTime(
                          getAccountCreatedDate(item)
                        )}
                      </td>

                      <td className="whitespace-nowrap px-4 py-3 text-slate-500">
                        {formatDateTime(
                          getAccountUpdatedDate(item)
                        )}
                      </td>
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