import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { invoiceService, downloadBlob } from '../services/invoice.service'
import { formatCurrency, formatDate, StatusChip, formatFileSize } from '../utils/formatters'

const FILTER_CHIPS = [
  { label: 'All', value: '' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Extracted', value: 'EXTRACTED' },
  { label: 'Validated', value: 'VALIDATED' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Flagged', value: 'FLAGGED' },
  { label: 'Rejected', value: 'REJECTED' },
]

export default function InvoiceHistory() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [exporting, setExporting] = useState(false)

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['invoices', status, page, search],
    queryFn: () => invoiceService.getAll({
      status: status || undefined,
      vendorName: search || undefined,
      page,
      size: 20
    }).then(r => r.data.data),
    keepPreviousData: true
  })

  const invoices = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  const handleDelete = async (id, e) => {
    e.stopPropagation()
    if (!confirm('Delete this invoice?')) return
    try {
      await invoiceService.delete(id)
      toast.success('Invoice deleted')
      refetch()
    } catch { toast.error('Delete failed') }
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const res = await invoiceService.exportExcel({ status: status || undefined })
      downloadBlob(res.data, `invoices_${Date.now()}.xlsx`)
      toast.success('Export downloaded!')
    } catch { toast.error('Export failed') } finally { setExporting(false) }
  }

  return (
    <div className="p-lg space-y-lg">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-headline-lg text-on-surface">Invoice History</h1>
          <p className="text-body-md text-on-surface-variant mt-xs">{totalElements} total invoices</p>
        </div>
        <div className="flex gap-sm">
          <button onClick={handleExport} disabled={exporting} className="btn-secondary">
            {exporting
              ? <span className="w-4 h-4 border-2 border-secondary/30 border-t-secondary rounded-full animate-spin" />
              : <span className="material-symbols-outlined text-xl">download</span>}
            Export Excel
          </button>
          <button onClick={() => navigate('/upload')} className="btn-primary">
            <span className="material-symbols-outlined">upload_file</span>
            Upload
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="relative">
        <span className="material-symbols-outlined absolute left-md top-1/2 -translate-y-1/2 text-outline">search</span>
        <input
          className="input-field pl-10"
          placeholder="Search vendor or invoice number…"
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(0) }}
        />
      </div>

      {/* Filter chips */}
      <div className="flex gap-sm overflow-x-auto hide-scrollbar pb-xs">
        {FILTER_CHIPS.map(f => (
          <button
            key={f.value}
            onClick={() => { setStatus(f.value); setPage(0) }}
            className={`flex-shrink-0 px-md py-xs rounded-full text-label-md font-medium transition-all ${
              status === f.value
                ? 'bg-primary text-white'
                : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Table (desktop) / Cards (mobile) */}
      {isLoading ? (
        <div className="flex justify-center py-xl">
          <div className="w-10 h-10 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
        </div>
      ) : invoices.length === 0 ? (
        <div className="card p-xl text-center">
          <span className="material-symbols-outlined text-5xl text-on-surface-variant mb-md block">receipt_long</span>
          <p className="text-headline-md text-on-surface">No invoices found</p>
          <p className="text-body-md text-on-surface-variant mt-xs mb-lg">
            {search || status ? 'Try adjusting your filters' : 'Upload your first invoice to get started'}
          </p>
          <button onClick={() => navigate('/upload')} className="btn-primary mx-auto">
            <span className="material-symbols-outlined">upload_file</span>Upload Invoice
          </button>
        </div>
      ) : (
        <>
          {/* Desktop table */}
          <div className="card overflow-hidden hidden md:block">
            <table className="w-full">
              <thead className="bg-surface-container-low">
                <tr>
                  <th className="table-header">Invoice #</th>
                  <th className="table-header">Vendor</th>
                  <th className="table-header">Date</th>
                  <th className="table-header text-right">Amount</th>
                  <th className="table-header">Status</th>
                  <th className="table-header">Confidence</th>
                  <th className="table-header text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {invoices.map(inv => (
                  <tr key={inv.id} className="table-row cursor-pointer" onClick={() => navigate(`/validate/${inv.id}`)}>
                    <td className="table-cell">
                      <span className="font-mono font-medium text-primary">
                        {inv.invoiceNumber || `#${inv.id}`}
                      </span>
                    </td>
                    <td className="table-cell">
                      <span className="font-medium">{inv.vendorName || '—'}</span>
                      {inv.fileName && <p className="text-label-md text-on-surface-variant">{inv.fileName}</p>}
                    </td>
                    <td className="table-cell">{formatDate(inv.invoiceDate)}</td>
                    <td className="table-cell text-right font-mono font-semibold">
                      {formatCurrency(inv.totalAmount)}
                    </td>
                    <td className="table-cell"><StatusChip status={inv.status} /></td>
                    <td className="table-cell">
                      {inv.ocrConfidence != null ? (
                        <div className="flex items-center gap-sm">
                          <div className="flex-1 h-1.5 bg-surface-container rounded-full overflow-hidden w-16">
                            <div
                              className={`h-full rounded-full ${Number(inv.ocrConfidence) >= 85 ? 'bg-emerald-500' : Number(inv.ocrConfidence) >= 65 ? 'bg-amber-400' : 'bg-error'}`}
                              style={{ width: `${Math.min(Number(inv.ocrConfidence), 100)}%` }}
                            />
                          </div>
                          <span className="text-label-md">{Number(inv.ocrConfidence).toFixed(0)}%</span>
                        </div>
                      ) : <span className="text-on-surface-variant">—</span>}
                    </td>
                    <td className="table-cell text-right" onClick={e => e.stopPropagation()}>
                      <div className="flex items-center justify-end gap-xs">
                        <button
                          onClick={() => navigate(`/validate/${inv.id}`)}
                          className="btn-icon text-primary"
                          title="Validate"
                        >
                          <span className="material-symbols-outlined text-lg">edit</span>
                        </button>
                        <button
                          onClick={(e) => handleDelete(inv.id, e)}
                          className="btn-icon text-error"
                          title="Delete"
                        >
                          <span className="material-symbols-outlined text-lg">delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile cards */}
          <div className="space-y-sm md:hidden">
            {invoices.map(inv => (
              <div key={inv.id} className="card-hover p-md" onClick={() => navigate(`/validate/${inv.id}`)}>
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-semibold text-on-surface">{inv.vendorName || '—'}</p>
                    <p className="text-label-md text-on-surface-variant mt-xs flex items-center gap-xs">
                      <span className="material-symbols-outlined text-sm">calendar_today</span>
                      {formatDate(inv.invoiceDate)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold text-primary">{formatCurrency(inv.totalAmount)}</p>
                    <div className="mt-xs"><StatusChip status={inv.status} /></div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-sm">
              <button onClick={() => setPage(p => p - 1)} disabled={page === 0} className="btn-icon disabled:opacity-40">
                <span className="material-symbols-outlined">chevron_left</span>
              </button>
              <span className="text-body-md text-on-surface-variant">Page {page + 1} of {totalPages}</span>
              <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1} className="btn-icon disabled:opacity-40">
                <span className="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
