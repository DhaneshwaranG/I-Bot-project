import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { invoiceService } from '../services/invoice.service'
import { formatCurrency, formatDate, confidenceColor } from '../utils/formatters'

export default function ValidateOcr() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({})
  const [saving, setSaving] = useState(false)

  const { data: invoice, isLoading } = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => invoiceService.getById(id).then(r => r.data.data),
    enabled: !!id
  })

  useEffect(() => {
    if (invoice) setForm({
      invoiceNumber: invoice.invoiceNumber || '',
      vendorName: invoice.vendorName || '',
      invoiceDate: invoice.invoiceDate || '',
      dueDate: invoice.dueDate || '',
      subtotal: invoice.subtotal || '',
      gstAmount: invoice.gstAmount || '',
      gstRate: invoice.gstRate || '',
      totalAmount: invoice.totalAmount || '',
      currency: invoice.currency || 'INR',
      poNumber: invoice.poNumber || '',
      paymentTerms: invoice.paymentTerms || '',
      notes: invoice.notes || ''
    })
  }, [invoice])

  const handleChange = (e) => setForm(p => ({ ...p, [e.target.name]: e.target.value }))

  const handleValidate = async () => {
    setSaving(true)
    try {
      await invoiceService.validate(id, { ...form, invoiceDate: form.invoiceDate || null, dueDate: form.dueDate || null })
      toast.success('Invoice validated successfully!')
      navigate('/history')
    } catch (err) {
      toast.error(err.response?.data?.error || 'Validation failed')
    } finally { setSaving(false) }
  }

  const handleApprove = async () => {
    setSaving(true)
    try {
      await invoiceService.approve(id)
      toast.success('Invoice approved!')
      navigate('/history')
    } catch { toast.error('Approval failed') } finally { setSaving(false) }
  }

  const handleReject = async () => {
    const reason = prompt('Reason for rejection?')
    if (!reason) return
    setSaving(true)
    try {
      await invoiceService.reject(id, reason)
      toast.success('Invoice rejected')
      navigate('/history')
    } catch { toast.error('Rejection failed') } finally { setSaving(false) }
  }

  if (isLoading) return (
    <div className="p-xl flex items-center justify-center min-h-64">
      <div className="w-10 h-10 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
    </div>
  )

  if (!invoice) return (
    <div className="p-xl text-center">
      <p className="text-body-md text-on-surface-variant">Invoice not found</p>
    </div>
  )

  const conf = Number(invoice.ocrConfidence || 0)

  const inputField = (name, label, type = 'text') => (
    <div className="bg-white border border-outline-variant rounded-xl p-md shadow-sm">
      <label className="input-label">{label}</label>
      <input
        type={type}
        name={name}
        value={form[name] || ''}
        onChange={handleChange}
        className="w-full bg-surface-container border-none rounded-lg text-body-lg py-sm px-md focus:ring-2 focus:ring-primary outline-none"
      />
    </div>
  )

  return (
    <div className="p-lg max-w-3xl mx-auto space-y-lg">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-headline-lg text-on-surface">Validate OCR Data</h1>
          <p className="text-body-md text-on-surface-variant mt-xs">
            {invoice.fileName} • Invoice #{invoice.id}
          </p>
        </div>
        <div className="flex items-center gap-sm">
          <span className="text-label-md text-on-surface-variant">OCR Confidence</span>
          <span className={`text-headline-md font-bold ${confidenceColor(conf)}`}>{conf.toFixed(1)}%</span>
        </div>
      </div>

      {/* OCR confidence bar */}
      <div className="card p-md">
        <div className="flex items-center justify-between mb-sm">
          <p className="text-body-md font-medium text-on-surface">Extraction Quality</p>
          {conf < 70 && (
            <span className="chip-warning">
              <span className="material-symbols-outlined text-sm">warning</span>
              Low confidence — review carefully
            </span>
          )}
          {conf >= 70 && conf < 85 && <span className="chip-warning">Review recommended</span>}
          {conf >= 85 && <span className="chip-success">High confidence</span>}
        </div>
        <div className="h-2.5 bg-surface-container rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all ${conf >= 85 ? 'bg-emerald-500' : conf >= 65 ? 'bg-amber-400' : 'bg-error'}`}
            style={{ width: `${Math.min(conf, 100)}%` }}
          />
        </div>
      </div>

      {invoice.isDuplicate && (
        <div className="flex items-center gap-sm bg-amber-50 border border-amber-200 text-amber-800 rounded-xl p-md">
          <span className="material-symbols-outlined">content_copy</span>
          <p className="text-body-md">Possible duplicate of Invoice #{invoice.duplicateOfId}. Verify before approving.</p>
        </div>
      )}

      {/* Form fields */}
      <div className="space-y-md">
        <h2 className="text-headline-md text-on-surface">Extracted Fields</h2>
        <p className="text-body-md text-on-surface-variant">Review and correct any errors before validating.</p>

        <div className="grid grid-cols-2 gap-md">
          {inputField('invoiceNumber', 'Invoice Number')}
          {inputField('vendorName', 'Vendor Name')}
          {inputField('invoiceDate', 'Invoice Date', 'date')}
          {inputField('dueDate', 'Due Date', 'date')}
          {inputField('subtotal', 'Subtotal (₹)', 'number')}
          {inputField('gstAmount', 'GST Amount (₹)', 'number')}
          {inputField('gstRate', 'GST Rate (%)', 'number')}
          {inputField('totalAmount', 'Total Amount (₹)', 'number')}
          {inputField('poNumber', 'PO Number')}
          {inputField('paymentTerms', 'Payment Terms')}
        </div>

        <div className="bg-white border border-outline-variant rounded-xl p-md shadow-sm">
          <label className="input-label">Notes</label>
          <textarea
            name="notes"
            value={form.notes || ''}
            onChange={handleChange}
            rows={3}
            className="w-full bg-surface-container border-none rounded-lg text-body-lg py-sm px-md focus:ring-2 focus:ring-primary outline-none resize-none"
            placeholder="Add validation notes…"
          />
        </div>
      </div>

      {/* Actions */}
      <div className="flex flex-wrap gap-md">
        <button onClick={handleValidate} disabled={saving} className="btn-primary">
          <span className="material-symbols-outlined">fact_check</span>
          Validate Invoice
        </button>
        <button onClick={handleApprove} disabled={saving}
          className="flex items-center gap-sm bg-emerald-600 text-white px-md py-sm rounded-lg font-medium hover:bg-emerald-700 transition-colors disabled:opacity-50">
          <span className="material-symbols-outlined">thumb_up</span>
          Approve
        </button>
        <button onClick={handleReject} disabled={saving} className="btn-danger">
          <span className="material-symbols-outlined">thumb_down</span>
          Reject
        </button>
        <button onClick={() => navigate('/history')} className="btn-ghost ml-auto">
          <span className="material-symbols-outlined">arrow_back</span>
          Back to History
        </button>
      </div>
    </div>
  )
}
