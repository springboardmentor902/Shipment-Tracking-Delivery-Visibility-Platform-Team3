import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppLayout from '../components/AppLayout'
import TextField from '../components/TextField'
import { extractErrorMessage } from '../services/api'
import { shipmentService } from '../services/shipmentService'

const EMPTY_PACKAGE = {
  description: '',
  weightKg: '',
  lengthCm: '',
  widthCm: '',
  heightCm: '',
  quantity: '1',
  declaredValue: '',
  fragile: false,
}

const INITIAL = {
  senderName: '',
  senderPhone: '',
  senderAddress: '',
  receiverName: '',
  receiverPhone: '',
  receiverEmail: '',
  receiverAddress: '',
  pickupAddress: '',
  deliveryAddress: '',
  priority: 'STANDARD',
}

export default function ShipmentCreate() {
  const navigate = useNavigate()
  const [form, setForm] = useState(INITIAL)
  const [packages, setPackages] = useState([{ ...EMPTY_PACKAGE }])
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: undefined }))
    setServerError('')
  }

  function handlePackageChange(index, field, value) {
    setPackages((prev) => prev.map((item, i) => (i === index ? { ...item, [field]: value } : item)))
    setErrors((prev) => ({ ...prev, packages: undefined }))
  }

  function copySenderToPickup() {
    setForm((prev) => ({ ...prev, pickupAddress: prev.senderAddress }))
  }

  function copyReceiverToDelivery() {
    setForm((prev) => ({ ...prev, deliveryAddress: prev.receiverAddress }))
  }

  function validate() {
    const next = {}
    const phoneRule = /^[0-9+\-\s()]{7,20}$/

    if (!form.senderName.trim()) next.senderName = 'Required'
    if (!phoneRule.test(form.senderPhone)) next.senderPhone = 'Enter a valid phone number'
    if (!form.senderAddress.trim()) next.senderAddress = 'Required'

    if (!form.receiverName.trim()) next.receiverName = 'Required'
    if (!phoneRule.test(form.receiverPhone)) next.receiverPhone = 'Enter a valid phone number'
    if (form.receiverEmail && !/^\S+@\S+\.\S+$/.test(form.receiverEmail))
      next.receiverEmail = 'Enter a valid email'
    if (!form.receiverAddress.trim()) next.receiverAddress = 'Required'

    if (!form.pickupAddress.trim()) next.pickupAddress = 'Required'
    if (!form.deliveryAddress.trim()) next.deliveryAddress = 'Required'

    const badPackage = packages.some(
      (item) => !item.description.trim() || !Number(item.weightKg) || !Number(item.quantity)
    )
    if (badPackage) next.packages = 'Every package needs a description, a weight above zero, and a quantity.'

    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validate()) return

    setSubmitting(true)
    setServerError('')
    try {
      const created = await shipmentService.create({
        ...form,
        packages: packages.map((item) => ({
          description: item.description.trim(),
          weightKg: Number(item.weightKg),
          lengthCm: item.lengthCm ? Number(item.lengthCm) : null,
          widthCm: item.widthCm ? Number(item.widthCm) : null,
          heightCm: item.heightCm ? Number(item.heightCm) : null,
          quantity: Number(item.quantity),
          declaredValue: item.declaredValue ? Number(item.declaredValue) : null,
          fragile: Boolean(item.fragile),
        })),
      })
      navigate(`/shipments/${created.id}`, { replace: true })
    } catch (err) {
      if (err.response?.status === 403) {
        setServerError('Your role cannot create shipments. Only business clients and logistics operators can.')
      } else {
        setServerError(extractErrorMessage(err))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppLayout>
      <div className="mb-6">
        <Link to="/shipments" className="text-sm text-brand-600 hover:text-brand-700">
          ← Back to shipments
        </Link>
        <h1 className="mt-2 text-xl font-semibold text-slate-900">New shipment</h1>
      </div>

      <form onSubmit={handleSubmit} noValidate className="space-y-6">
        {serverError && (
          <div role="alert" className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
            {serverError}
          </div>
        )}

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Sender</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <TextField id="senderName" label="Name" value={form.senderName} onChange={handleChange} error={errors.senderName} />
            <TextField id="senderPhone" label="Phone" value={form.senderPhone} onChange={handleChange} error={errors.senderPhone} placeholder="9876543210" />
          </div>
          <div className="mt-5">
            <TextField id="senderAddress" label="Address" value={form.senderAddress} onChange={handleChange} error={errors.senderAddress} placeholder="Plot 14, Gachibowli, Hyderabad" />
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Receiver</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <TextField id="receiverName" label="Name" value={form.receiverName} onChange={handleChange} error={errors.receiverName} />
            <TextField id="receiverPhone" label="Phone" value={form.receiverPhone} onChange={handleChange} error={errors.receiverPhone} placeholder="9876500000" />
            <TextField id="receiverEmail" label="Email (optional)" type="email" value={form.receiverEmail} onChange={handleChange} error={errors.receiverEmail} />
          </div>
          <div className="mt-5">
            <TextField id="receiverAddress" label="Address" value={form.receiverAddress} onChange={handleChange} error={errors.receiverAddress} placeholder="Flat 302, Kondapur, Hyderabad" />
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">Route</h2>
          <div className="space-y-5">
            <div>
              <TextField id="pickupAddress" label="Pickup address" value={form.pickupAddress} onChange={handleChange} error={errors.pickupAddress} />
              <button type="button" onClick={copySenderToPickup} className="mt-1.5 text-xs font-medium text-brand-600 hover:text-brand-700">
                Same as sender address
              </button>
            </div>
            <div>
              <TextField id="deliveryAddress" label="Delivery address" value={form.deliveryAddress} onChange={handleChange} error={errors.deliveryAddress} />
              <button type="button" onClick={copyReceiverToDelivery} className="mt-1.5 text-xs font-medium text-brand-600 hover:text-brand-700">
                Same as receiver address
              </button>
            </div>
            <div>
              <label htmlFor="priority" className="mb-1.5 block text-sm font-medium text-slate-700">
                Priority
              </label>
              <select
                id="priority"
                name="priority"
                value={form.priority}
                onChange={handleChange}
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none
                           focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 sm:w-64"
              >
                <option value="STANDARD">Standard — 5 day estimate</option>
                <option value="EXPRESS">Express — 2 day estimate</option>
              </select>
            </div>
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Packages</h2>
            <button
              type="button"
              onClick={() => setPackages((prev) => [...prev, { ...EMPTY_PACKAGE }])}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Add package
            </button>
          </div>

          {errors.packages && <p className="mb-4 text-xs text-red-600">{errors.packages}</p>}

          <div className="space-y-4">
            {packages.map((item, index) => (
              <div key={index} className="rounded-lg border border-slate-200 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-500">Package {index + 1}</span>
                  {packages.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setPackages((prev) => prev.filter((_, i) => i !== index))}
                      className="text-xs font-medium text-red-600 hover:text-red-700"
                    >
                      Remove
                    </button>
                  )}
                </div>

                <div className="grid gap-4 sm:grid-cols-3">
                  <div className="sm:col-span-3">
                    <label className="mb-1.5 block text-sm font-medium text-slate-700">Description</label>
                    <input
                      value={item.description}
                      onChange={(e) => handlePackageChange(index, 'description', e.target.value)}
                      placeholder="Laptop"
                      className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none
                                 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                    />
                  </div>

                  {[
                    ['weightKg', 'Weight (kg)', '2.5'],
                    ['quantity', 'Quantity', '1'],
                    ['declaredValue', 'Declared value', '75000'],
                    ['lengthCm', 'Length (cm)', '40'],
                    ['widthCm', 'Width (cm)', '30'],
                    ['heightCm', 'Height (cm)', '10'],
                  ].map(([field, label, placeholder]) => (
                    <div key={field}>
                      <label className="mb-1.5 block text-sm font-medium text-slate-700">{label}</label>
                      <input
                        type="number"
                        step="any"
                        min="0"
                        value={item[field]}
                        onChange={(e) => handlePackageChange(index, field, e.target.value)}
                        placeholder={placeholder}
                        className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none
                                   focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                      />
                    </div>
                  ))}

                  <label className="flex items-center gap-2 text-sm text-slate-700 sm:col-span-3">
                    <input
                      type="checkbox"
                      checked={item.fragile}
                      onChange={(e) => handlePackageChange(index, 'fragile', e.target.checked)}
                      className="h-4 w-4 rounded border-slate-300"
                    />
                    Fragile — handle with care
                  </label>
                </div>
              </div>
            ))}
          </div>
        </section>

        <div className="flex items-center gap-3">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-brand-600 px-5 py-2.5 text-sm font-medium text-white
                       hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Creating…' : 'Create shipment'}
          </button>
          <Link to="/shipments" className="text-sm font-medium text-slate-600 hover:text-slate-800">
            Cancel
          </Link>
        </div>
      </form>
    </AppLayout>
  )
}
