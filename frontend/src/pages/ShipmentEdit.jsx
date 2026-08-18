import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import TextField from '../components/TextField'
import { extractErrorMessage } from '../services/api'
import { shipmentService } from '../services/shipmentService'

const EMPTY_FORM = {
  receiverName: '',
  receiverPhone: '',
  receiverEmail: '',
  receiverAddress: '',
  pickupAddress: '',
  deliveryAddress: '',
  priority: 'STANDARD',
}

export default function ShipmentEdit() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY_FORM)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [errors, setErrors] = useState({})

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const shipment = await shipmentService.getById(id)
      setForm({
        receiverName: shipment.receiverName || '',
        receiverPhone: shipment.receiverPhone || '',
        receiverEmail: shipment.receiverEmail || '',
        receiverAddress: shipment.receiverAddress || '',
        pickupAddress: shipment.pickupAddress || '',
        deliveryAddress: shipment.deliveryAddress || '',
        priority: shipment.priority || 'STANDARD',
      })
    } catch (err) {
      if (err.response?.status === 404) setError('Shipment not found.')
      else if (err.response?.status === 403) setError('You do not have permission to edit this shipment.')
      else setError(extractErrorMessage(err, 'Could not load this shipment.'))
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
    setErrors((previous) => ({ ...previous, [name]: undefined }))
    setError('')
  }

  function validate() {
    const next = {}
    const phoneRule = /^[0-9+\-\s()]{7,20}$/
    if (!form.receiverName.trim()) next.receiverName = 'Required'
    if (!phoneRule.test(form.receiverPhone)) next.receiverPhone = 'Enter a valid phone number'
    if (form.receiverEmail && !/^\S+@\S+\.\S+$/.test(form.receiverEmail)) {
      next.receiverEmail = 'Enter a valid email'
    }
    if (!form.receiverAddress.trim()) next.receiverAddress = 'Required'
    if (!form.pickupAddress.trim()) next.pickupAddress = 'Required'
    if (!form.deliveryAddress.trim()) next.deliveryAddress = 'Required'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setSubmitting(true)
    setError('')
    try {
      const updated = await shipmentService.update(id, {
        ...form,
        receiverName: form.receiverName.trim(),
        receiverPhone: form.receiverPhone.trim(),
        receiverEmail: form.receiverEmail.trim() || null,
        receiverAddress: form.receiverAddress.trim(),
        pickupAddress: form.pickupAddress.trim(),
        deliveryAddress: form.deliveryAddress.trim(),
      })
      navigate(`/shipments/${updated.id}`, { replace: true })
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not save shipment changes.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <AppLayout>
        <p className="text-slate-500">Loading…</p>
      </AppLayout>
    )
  }

  if (error && !form.receiverName) {
    return (
      <AppLayout>
        <div role="alert" className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
          {error}
        </div>
        <Link to="/shipments" className="mt-4 inline-block text-sm text-brand-600 hover:text-brand-700">
          ← Back to shipments
        </Link>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <div className="mb-6">
        <Link to={`/shipments/${id}`} className="text-sm text-brand-600 hover:text-brand-700">
          ← Back to shipment
        </Link>
        <h1 className="mt-2 text-xl font-semibold text-slate-900">Edit shipment</h1>
        <p className="mt-1 text-sm text-slate-500">Update the receiver and delivery details.</p>
      </div>

      <form onSubmit={handleSubmit} noValidate className="space-y-6">
        {error && (
          <div role="alert" className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
            {error}
          </div>
        )}

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Receiver</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <TextField id="receiverName" label="Name" value={form.receiverName} onChange={handleChange} error={errors.receiverName} />
            <TextField id="receiverPhone" label="Phone" value={form.receiverPhone} onChange={handleChange} error={errors.receiverPhone} />
            <TextField id="receiverEmail" label="Email (optional)" type="email" value={form.receiverEmail} onChange={handleChange} error={errors.receiverEmail} />
          </div>
          <div className="mt-5">
            <TextField id="receiverAddress" label="Address" value={form.receiverAddress} onChange={handleChange} error={errors.receiverAddress} />
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Route</h2>
          <div className="space-y-5">
            <TextField id="pickupAddress" label="Pickup address" value={form.pickupAddress} onChange={handleChange} error={errors.pickupAddress} />
            <TextField id="deliveryAddress" label="Delivery address" value={form.deliveryAddress} onChange={handleChange} error={errors.deliveryAddress} />
            <div>
              <label htmlFor="priority" className="mb-1.5 block text-sm font-medium text-slate-700">Priority</label>
              <select
                id="priority"
                name="priority"
                value={form.priority}
                onChange={handleChange}
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 sm:w-64"
              >
                <option value="STANDARD">Standard — 5 day estimate</option>
                <option value="EXPRESS">Express — 2 day estimate</option>
              </select>
            </div>
          </div>
        </section>

        <div className="flex items-center gap-3">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-brand-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Saving…' : 'Save changes'}
          </button>
          <Link to={`/shipments/${id}`} className="text-sm font-medium text-slate-600 hover:text-slate-800">
            Cancel
          </Link>
        </div>
      </form>
    </AppLayout>
  )
}
