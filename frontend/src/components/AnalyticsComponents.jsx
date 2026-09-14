import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts'

export function StatCard({ label, value, hint }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
        {label}
      </p>

      <p className="mt-2 text-2xl font-bold text-slate-900">
        {value}
      </p>

      {hint && (
        <p className="mt-1 text-xs text-slate-400">
          {hint}
        </p>
      )}
    </div>
  )
}

export function SectionCard({ title, children }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
        {title}
      </h2>

      {children}
    </div>
  )
}

export function StatusBarChart({ data }) {
  const chartData = Object.entries(data || {}).map(
    ([status, count]) => ({
      status: status.replace(/_/g, ' '),
      count,
    })
  )

  if (chartData.length === 0) {
    return (
      <p className="text-sm text-slate-400">
        No data yet.
      </p>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" />

        <XAxis
          dataKey="status"
          tick={{ fontSize: 11 }}
          interval={0}
          angle={-15}
          textAnchor="end"
          height={60}
        />

        <YAxis
          allowDecimals={false}
          tick={{ fontSize: 11 }}
        />

        <Tooltip />

        <Bar
          dataKey="count"
          fill="#2563eb"
          radius={[4, 4, 0, 0]}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}

export function LoadingState() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="text-slate-500">
        Loading dashboard...
      </p>
    </div>
  )
}