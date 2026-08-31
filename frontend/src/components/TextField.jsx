export default function TextField({
  id,
  // defaults to id; pass it when the form state key differs from the DOM id
  name,
  label,
  type = 'text',
  value,
  onChange,
  error,
  placeholder,
  autoComplete,
  trailing,
}) {
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          name={name || id}
          type={type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          autoComplete={autoComplete}
          aria-invalid={Boolean(error)}
          className={`w-full rounded-lg border px-3.5 py-2.5 text-sm outline-none transition
            focus:ring-2 focus:ring-brand-500/30
            ${trailing ? 'pr-20' : ''}
            ${
              error
                ? 'border-red-400 focus:border-red-500'
                : 'border-slate-300 focus:border-brand-500'
            }`}
        />
        {trailing}
      </div>
      {error && <p className="mt-1.5 text-xs text-red-600">{error}</p>}
    </div>
  )
}
