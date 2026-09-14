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
  const [packages, setPackages] = useState([
    { ...EMPTY_PACKAGE },
  ])

  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }))

    setErrors((previous) => ({
      ...previous,
      [name]: undefined,
    }))

    setServerError('')
  }

  function handlePackageChange(index, field, value) {
    setPackages((previous) =>
      previous.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              [field]: value,
            }
          : item
      )
    )

    setErrors((previous) => ({
      ...previous,
      packages: undefined,
    }))

    setServerError('')
  }

  function copySenderToPickup() {
    setForm((previous) => ({
      ...previous,
      pickupAddress: previous.senderAddress,
    }))
  }

  function copyReceiverToDelivery() {
    setForm((previous) => ({
      ...previous,
      deliveryAddress: previous.receiverAddress,
    }))
  }

  function validate() {
    const next = {}
    const phoneRule = /^[0-9+\-\s()]{7,20}$/
    const emailRule = /^\S+@\S+\.\S+$/

    // Sender
    if (!form.senderName.trim()) {
      next.senderName = 'Required'
    }

    if (!form.senderPhone.trim()) {
      next.senderPhone = 'Required'
    } else if (!phoneRule.test(form.senderPhone.trim())) {
      next.senderPhone = 'Enter a valid phone number'
    }

    if (!form.senderAddress.trim()) {
      next.senderAddress = 'Required'
    }

    // Receiver
    if (!form.receiverName.trim()) {
      next.receiverName = 'Required'
    }

    if (!form.receiverPhone.trim()) {
      next.receiverPhone = 'Required'
    } else if (!phoneRule.test(form.receiverPhone.trim())) {
      next.receiverPhone = 'Enter a valid phone number'
    }

    if (!form.receiverEmail.trim()) {
      next.receiverEmail = 'Required'
    } else if (!emailRule.test(form.receiverEmail.trim())) {
      next.receiverEmail = 'Enter a valid email'
    }

    if (!form.receiverAddress.trim()) {
      next.receiverAddress = 'Required'
    }

    // Route
    if (!form.pickupAddress.trim()) {
      next.pickupAddress = 'Required'
    }

    if (!form.deliveryAddress.trim()) {
      next.deliveryAddress = 'Required'
    }

    // Package
    const badPackage = packages.some(
      (item) =>
        !item.description.trim() ||
        Number(item.weightKg) <= 0 ||
        Number(item.quantity) <= 0
    )

    if (badPackage) {
      next.packages =
        'Every package needs a description, a weight above zero, and a quantity.'
    }

    setErrors(next)

    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()

    if (!validate()) {
      return
    }

    setSubmitting(true)
    setServerError('')

    try {
      const firstPackage = packages[0]

      const created = await shipmentService.create({
        senderName: form.senderName.trim(),
        senderPhone: form.senderPhone.trim(),
        senderAddress: form.senderAddress.trim(),

        receiverName: form.receiverName.trim(),
        receiverEmail: form.receiverEmail.trim(),
        receiverAddress: form.receiverAddress.trim(),
        receiverPhone: form.receiverPhone.trim(),

        packageDescription:
          firstPackage.description.trim(),

        weightKg: Number(firstPackage.weightKg),
      })

      navigate(
        `/shipments/${created.trackingNumber}`,
        { replace: true }
      )
    } catch (err) {
      if (err.response?.status === 403) {
        setServerError(
          'Your role cannot create shipments. Only business clients and logistics operators can.'
        )
      } else {
        setServerError(
          extractErrorMessage(err)
        )
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppLayout>
      <div className="mb-6">
        <Link
          to="/shipments"
          className="text-sm text-brand-600 hover:text-brand-700"
        >
          ← Back to shipments
        </Link>

        <h1 className="mt-2 text-xl font-semibold text-slate-900">
          New shipment
        </h1>
      </div>

      <form
        onSubmit={handleSubmit}
        noValidate
        className="space-y-6"
      >
        {serverError && (
          <div
            role="alert"
            className="rounded-lg bg-red-50 px-3.5 py-2.5 text-sm text-red-700"
          >
            {serverError}
          </div>
        )}

        {/* SENDER */}
        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Sender
          </h2>

          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              id="senderName"
              name="senderName"
              label="Name"
              value={form.senderName}
              onChange={handleChange}
              error={errors.senderName}
            />

            <TextField
              id="senderPhone"
              name="senderPhone"
              label="Phone"
              type="tel"
              value={form.senderPhone}
              onChange={handleChange}
              error={errors.senderPhone}
              placeholder="9876543210"
            />
          </div>

          <div className="mt-5">
            <TextField
              id="senderAddress"
              name="senderAddress"
              label="Address"
              value={form.senderAddress}
              onChange={handleChange}
              error={errors.senderAddress}
              placeholder="Plot 14, Gachibowli, Hyderabad"
            />
          </div>
        </section>

        {/* RECEIVER */}
        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Receiver
          </h2>

          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              id="receiverName"
              name="receiverName"
              label="Name"
              value={form.receiverName}
              onChange={handleChange}
              error={errors.receiverName}
            />

            <TextField
              id="receiverPhone"
              name="receiverPhone"
              label="Phone"
              type="tel"
              value={form.receiverPhone}
              onChange={handleChange}
              error={errors.receiverPhone}
              placeholder="9876500000"
            />

            <TextField
              id="receiverEmail"
              name="receiverEmail"
              label="Email"
              type="email"
              value={form.receiverEmail}
              onChange={handleChange}
              error={errors.receiverEmail}
              placeholder="receiver@example.com"
            />
          </div>

          <div className="mt-5">
            <TextField
              id="receiverAddress"
              name="receiverAddress"
              label="Address"
              value={form.receiverAddress}
              onChange={handleChange}
              error={errors.receiverAddress}
              placeholder="Flat 302, Kondapur, Hyderabad"
            />
          </div>
        </section>

        {/* ROUTE */}
        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Route
          </h2>

          <div className="space-y-5">
            <div>
              <TextField
                id="pickupAddress"
                name="pickupAddress"
                label="Pickup address"
                value={form.pickupAddress}
                onChange={handleChange}
                error={errors.pickupAddress}
                placeholder="Vijayawada, Andhra Pradesh"
              />

              <button
                type="button"
                onClick={copySenderToPickup}
                className="mt-1.5 text-xs font-medium text-brand-600 hover:text-brand-700"
              >
                Same as sender address
              </button>
            </div>

            <div>
              <TextField
                id="deliveryAddress"
                name="deliveryAddress"
                label="Delivery address"
                value={form.deliveryAddress}
                onChange={handleChange}
                error={errors.deliveryAddress}
                placeholder="Hyderabad, Telangana"
              />

              <button
                type="button"
                onClick={copyReceiverToDelivery}
                className="mt-1.5 text-xs font-medium text-brand-600 hover:text-brand-700"
              >
                Same as receiver address
              </button>
            </div>

            <div>
              <label
                htmlFor="priority"
                className="mb-1.5 block text-sm font-medium text-slate-700"
              >
                Priority
              </label>

              <select
                id="priority"
                name="priority"
                value={form.priority}
                onChange={handleChange}
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 sm:w-64"
              >
                <option value="STANDARD">
                  Standard — 5 day estimate
                </option>

                <option value="EXPRESS">
                  Express — 2 day estimate
                </option>
              </select>
            </div>
          </div>
        </section>

        {/* PACKAGES */}
        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Packages
            </h2>

            <button
              type="button"
              onClick={() =>
                setPackages((previous) => [
                  ...previous,
                  { ...EMPTY_PACKAGE },
                ])
              }
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Add package
            </button>
          </div>

          {errors.packages && (
            <p className="mb-4 text-xs text-red-600">
              {errors.packages}
            </p>
          )}

          <div className="space-y-4">
            {packages.map((item, index) => (
              <div
                key={index}
                className="rounded-lg border border-slate-200 p-4"
              >
                <div className="mb-3 flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-500">
                    Package {index + 1}
                  </span>

                  {packages.length > 1 && (
                    <button
                      type="button"
                      onClick={() =>
                        setPackages((previous) =>
                          previous.filter(
                            (_, itemIndex) =>
                              itemIndex !== index
                          )
                        )
                      }
                      className="text-xs font-medium text-red-600 hover:text-red-700"
                    >
                      Remove
                    </button>
                  )}
                </div>

                <div className="grid gap-4 sm:grid-cols-3">
                  <div className="sm:col-span-3">
                    <label className="mb-1.5 block text-sm font-medium text-slate-700">
                      Description
                    </label>

                    <input
                      value={item.description}
                      onChange={(event) =>
                        handlePackageChange(
                          index,
                          'description',
                          event.target.value
                        )
                      }
                      placeholder="Laptop"
                      className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                    />
                  </div>

                  {[
                    [
                      'weightKg',
                      'Weight (kg)',
                      '2.5',
                    ],
                    [
                      'quantity',
                      'Quantity',
                      '1',
                    ],
                    [
                      'declaredValue',
                      'Declared value',
                      '75000',
                    ],
                    [
                      'lengthCm',
                      'Length (cm)',
                      '40',
                    ],
                    [
                      'widthCm',
                      'Width (cm)',
                      '30',
                    ],
                    [
                      'heightCm',
                      'Height (cm)',
                      '10',
                    ],
                  ].map(
                    ([field, label, placeholder]) => (
                      <div key={field}>
                        <label className="mb-1.5 block text-sm font-medium text-slate-700">
                          {label}
                        </label>

                        <input
                          type="number"
                          step="any"
                          min="0"
                          value={item[field]}
                          onChange={(event) =>
                            handlePackageChange(
                              index,
                              field,
                              event.target.value
                            )
                          }
                          placeholder={placeholder}
                          className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                        />
                      </div>
                    )
                  )}

                  <label className="flex items-center gap-2 text-sm text-slate-700 sm:col-span-3">
                    <input
                      type="checkbox"
                      checked={item.fragile}
                      onChange={(event) =>
                        handlePackageChange(
                          index,
                          'fragile',
                          event.target.checked
                        )
                      }
                      className="h-4 w-4 rounded border-slate-300"
                    />

                    Fragile — handle with care
                  </label>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* ACTIONS */}
        <div className="flex items-center gap-3">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-brand-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting
              ? 'Creating…'
              : 'Create shipment'}
          </button>

          <Link
            to="/shipments"
            className="text-sm font-medium text-slate-600 hover:text-slate-800"
          >
            Cancel
          </Link>
        </div>
      </form>
    </AppLayout>
  )
}