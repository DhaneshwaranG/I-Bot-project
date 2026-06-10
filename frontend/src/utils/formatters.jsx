import { format, parseISO, isValid } from 'date-fns'

export const formatCurrency = (amount, currency = 'INR') => {
  if (amount == null) return '—'
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

export const formatDate = (date, fmt = 'dd MMM yyyy') => {
  if (!date) return '—'
  try {
    const d = typeof date === 'string' ? parseISO(date) : date
    return isValid(d) ? format(d, fmt) : '—'
  } catch { return '—' }
}

export const formatDateTime = (date) => formatDate(date, 'dd MMM yyyy, HH:mm')

export const formatFileSize = (bytes) => {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export const formatConfidence = (val) => {
  if (val == null) return '—'
  return `${Number(val).toFixed(1)}%`
}

export const STATUS_META = {
  PENDING:    { label: 'Pending',    cls: 'chip-pending',  dot: 'bg-gray-400' },
  PROCESSING: { label: 'Processing', cls: 'chip-info',     dot: 'bg-blue-400' },
  EXTRACTED:  { label: 'Extracted',  cls: 'chip-info',     dot: 'bg-blue-400' },
  VALIDATED:  { label: 'Validated',  cls: 'chip-success',  dot: 'bg-emerald-500' },
  APPROVED:   { label: 'Approved',   cls: 'chip-success',  dot: 'bg-emerald-500' },
  REJECTED:   { label: 'Rejected',   cls: 'chip-error',    dot: 'bg-red-500' },
  FLAGGED:    { label: 'Flagged',    cls: 'chip-warning',  dot: 'bg-amber-500' },
  DUPLICATE:  { label: 'Duplicate',  cls: 'chip-warning',  dot: 'bg-amber-500' },
}

export const StatusChip = ({ status }) => {
  const meta = STATUS_META[status] || STATUS_META.PENDING
  return (
    <span className={meta.cls}>
      <span className={`w-1.5 h-1.5 rounded-full ${meta.dot}`} />
      {meta.label}
    </span>
  )
}

export const confidenceColor = (score) => {
  if (score >= 85) return 'text-emerald-600'
  if (score >= 65) return 'text-amber-600'
  return 'text-red-600'
}
