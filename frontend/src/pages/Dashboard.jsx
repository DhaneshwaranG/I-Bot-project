import { useQuery } from '@tanstack/react-query'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line } from 'recharts'
import { dashboardService } from '../services/invoice.service'
import { formatCurrency } from '../utils/formatters'
import { useAuth } from '../context/AuthContext'

function StatCard({ icon, label, value, sub, color = 'bg-primary/10', iconColor = 'text-primary' }) {
  return (
    <div className="card p-md flex items-center gap-md">
      <div className={`w-12 h-12 rounded-xl ${color} flex items-center justify-center flex-shrink-0`}>
        <span className={`material-symbols-outlined text-2xl ${iconColor}`}>{icon}</span>
      </div>
      <div>
        <p className="text-label-md text-on-surface-variant uppercase tracking-wider">{label}</p>
        <p className="text-display-lg font-bold text-on-surface leading-tight">{value}</p>
        {sub && <p className="text-label-md text-on-surface-variant mt-xs">{sub}</p>}
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: () => dashboardService.getStats().then(r => r.data.data),
    refetchInterval: 60000
  })

  const s = data || {}

  const chartData = s.monthlyStats?.map(m => ({
    name: m.monthName,
    invoices: m.count,
    amount: Number(m.totalAmount || 0)
  })) || []

  const accuracy = s.averageOcrConfidence ? s.averageOcrConfidence.toFixed(1) : '—'

  if (isLoading) return (
    <div className="p-xl flex items-center justify-center min-h-64">
      <div className="w-10 h-10 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
    </div>
  )

  return (
    <div className="p-lg space-y-lg">
      {/* Header */}
      <div>
        <h1 className="text-headline-lg text-on-surface">Good morning, {user?.name?.split(' ')[0]} 👋</h1>
        <p className="text-body-md text-on-surface-variant mt-xs">Here's your invoice processing overview</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-md">
        <StatCard icon="receipt_long" label="Total Invoices" value={s.totalInvoices ?? 0}
          sub={`+${s.invoicesToday ?? 0} today`} />
        <StatCard icon="check_circle" label="Processed" value={s.processedInvoices ?? 0}
          color="bg-emerald-50" iconColor="text-emerald-600"
          sub={`${s.approvedInvoices ?? 0} approved`} />
        <StatCard icon="pending" label="Pending Review" value={s.pendingInvoices ?? 0}
          color="bg-amber-50" iconColor="text-amber-600"
          sub="Needs attention" />
        <StatCard icon="error" label="Flagged" value={s.flaggedInvoices ?? 0}
          color="bg-red-50" iconColor="text-error"
          sub={`${s.duplicateInvoices ?? 0} duplicates`} />
      </div>

      {/* Amount + OCR row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-md">
        <div className="card p-md sm:col-span-2">
          <p className="text-label-md text-on-surface-variant uppercase tracking-wider mb-xs">Total Amount Processed</p>
          <p className="text-display-lg font-bold text-primary">{formatCurrency(s.totalAmountProcessed)}</p>
          <p className="text-body-md text-on-surface-variant mt-xs">
            {formatCurrency(s.pendingAmount)} pending
          </p>
        </div>
        <div className="card p-md flex flex-col justify-between">
          <p className="text-label-md text-on-surface-variant uppercase tracking-wider">OCR Accuracy</p>
          <div>
            <p className="text-display-lg font-bold text-emerald-600">{accuracy}%</p>
            <div className="mt-sm h-2 bg-surface-container rounded-full overflow-hidden">
              <div className="h-full bg-emerald-500 rounded-full transition-all"
                style={{ width: `${Math.min(accuracy, 100)}%` }} />
            </div>
          </div>
        </div>
      </div>

      {/* Charts */}
      {chartData.length > 0 && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-md">
          <div className="card p-md">
            <h3 className="text-headline-md text-on-surface mb-md">Monthly Invoice Volume</h3>
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5eeff" />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#454652' }} />
                <YAxis tick={{ fontSize: 12, fill: '#454652' }} />
                <Tooltip formatter={(v) => [v, 'Invoices']}
                  contentStyle={{ background: '#213145', border: 'none', color: '#eaf1ff', borderRadius: 8 }} />
                <Bar dataKey="invoices" fill="#24389c" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="card p-md">
            <h3 className="text-headline-md text-on-surface mb-md">Monthly Amount Trend</h3>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5eeff" />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#454652' }} />
                <YAxis tick={{ fontSize: 12, fill: '#454652' }}
                  tickFormatter={v => v >= 1000 ? `${(v / 1000).toFixed(0)}K` : v} />
                <Tooltip formatter={v => [formatCurrency(v), 'Amount']}
                  contentStyle={{ background: '#213145', border: 'none', color: '#eaf1ff', borderRadius: 8 }} />
                <Line type="monotone" dataKey="amount" stroke="#4edea3" strokeWidth={2} dot={{ fill: '#4edea3' }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Top vendors */}
      {s.topVendors?.length > 0 && (
        <div className="card overflow-hidden">
          <div className="p-md border-b border-outline-variant">
            <h3 className="text-headline-md text-on-surface">Top Vendors</h3>
          </div>
          <table className="w-full">
            <thead className="bg-surface-container-low">
              <tr>
                <th className="table-header">Vendor</th>
                <th className="table-header text-right">Invoices</th>
                <th className="table-header text-right">Total Amount</th>
              </tr>
            </thead>
            <tbody>
              {s.topVendors.map((v, i) => (
                <tr key={i} className="table-row">
                  <td className="table-cell font-medium">{v.vendorName || '—'}</td>
                  <td className="table-cell text-right font-mono text-primary">{v.invoiceCount}</td>
                  <td className="table-cell text-right font-mono font-semibold">{formatCurrency(v.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
