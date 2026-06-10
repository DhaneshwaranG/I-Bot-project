import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { invoiceService } from '../services/invoice.service'
import { formatFileSize } from '../utils/formatters'

const STEPS = ['Uploading', 'OCR Processing', 'Data Extraction', 'Ready for Review']

export default function UploadProcess() {
  const navigate = useNavigate()
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [step, setStep] = useState(-1)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  const onDrop = useCallback((accepted) => {
    if (accepted.length) { setFile(accepted[0]); setError(null); setResult(null) }
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/pdf': ['.pdf'], 'image/png': ['.png'], 'image/jpeg': ['.jpg', '.jpeg'] },
    maxSize: 20 * 1024 * 1024,
    multiple: false,
    onDropRejected: (f) => setError(f[0]?.errors[0]?.message || 'Invalid file')
  })

  const handleUpload = async () => {
    if (!file) return
    setUploading(true); setStep(0); setProgress(0); setError(null)
    try {
      const response = await invoiceService.upload(file, (p) => {
        setProgress(p)
        if (p === 100) setStep(1)
      })
      setStep(2)
      await new Promise(r => setTimeout(r, 400))
      setStep(3)
      const invoice = response.data.data
      setResult(invoice)
      toast.success('Invoice processed! Review extracted data.')
    } catch (err) {
      setError(err.response?.data?.error || 'Upload failed. Please try again.')
      toast.error('Upload failed')
      setStep(-1)
    } finally {
      setUploading(false)
    }
  }

  const handleReset = () => {
    setFile(null); setStep(-1); setProgress(0); setResult(null); setError(null)
  }

  return (
    <div className="p-lg max-w-2xl mx-auto space-y-lg">
      <div>
        <h1 className="text-headline-lg text-on-surface">Upload Invoice</h1>
        <p className="text-body-md text-on-surface-variant mt-xs">Upload a PDF or image — I-Bot will extract all fields automatically.</p>
      </div>

      {/* Drop zone */}
      {!uploading && !result && (
        <div
          {...getRootProps()}
          className={`border-2 border-dashed rounded-xl p-xl flex flex-col items-center gap-md cursor-pointer transition-all ${
            isDragActive ? 'border-primary bg-primary/5' : 'border-outline-variant hover:border-primary/50 hover:bg-surface-container-low'
          }`}
        >
          <input {...getInputProps()} />
          <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
            <span className="material-symbols-outlined text-primary text-4xl">cloud_upload</span>
          </div>
          <div className="text-center">
            <p className="text-headline-md text-primary">
              {isDragActive ? 'Drop it here!' : file ? file.name : 'Drag & drop or click to upload'}
            </p>
            <p className="text-body-md text-on-surface-variant mt-xs">
              {file ? formatFileSize(file.size) : 'PDF, PNG, JPG — max 20 MB'}
            </p>
          </div>
          <div className="flex gap-sm">
            {['PDF', 'PNG', 'JPG'].map(t => (
              <span key={t} className="bg-surface-container text-on-surface-variant px-sm py-xs rounded text-label-md">{t}</span>
            ))}
          </div>
        </div>
      )}

      {error && (
        <div className="flex items-center gap-sm bg-error-container text-on-error-container rounded-xl p-md">
          <span className="material-symbols-outlined">error</span>
          <p className="text-body-md">{error}</p>
        </div>
      )}

      {/* Upload button */}
      {file && !uploading && !result && (
        <div className="flex gap-md">
          <button onClick={handleUpload} className="btn-primary flex-1 justify-center">
            <span className="material-symbols-outlined">smart_toy</span>
            Process with I-Bot OCR
          </button>
          <button onClick={handleReset} className="btn-ghost">
            <span className="material-symbols-outlined">close</span>Clear
          </button>
        </div>
      )}

      {/* Processing steps */}
      {uploading && (
        <div className="card p-lg space-y-lg">
          <div className="flex items-center justify-between">
            <h3 className="text-headline-md text-on-surface">Processing Invoice</h3>
            <span className="chip-info animate-pulse">In Progress</span>
          </div>

          {/* Progress bar */}
          <div>
            <div className="flex justify-between text-label-md text-on-surface-variant mb-xs">
              <span>Upload progress</span>
              <span>{progress}%</span>
            </div>
            <div className="h-2 bg-surface-container rounded-full overflow-hidden">
              <div className="h-full bg-primary rounded-full transition-all duration-300" style={{ width: `${progress}%` }} />
            </div>
          </div>

          <div className="space-y-md">
            {STEPS.map((s, i) => (
              <div key={i} className="flex items-center gap-md">
                <div className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 transition-all ${
                  i < step ? 'bg-primary' : i === step ? 'bg-primary/20 border-2 border-primary' : 'bg-surface-container-high'
                }`}>
                  {i < step
                    ? <span className="material-symbols-outlined text-white text-sm">check</span>
                    : i === step
                    ? <span className="w-3 h-3 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                    : <span className="w-2 h-2 rounded-full bg-outline-variant" />
                  }
                </div>
                <div>
                  <p className={`text-body-md font-medium ${i <= step ? 'text-on-surface' : 'text-on-surface-variant'}`}>{s}</p>
                  {i === step && <p className="text-label-md text-on-surface-variant">In progress…</p>}
                  {i < step && <p className="text-label-md text-primary">Complete</p>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Success result */}
      {result && (
        <div className="card p-lg space-y-md">
          <div className="flex items-center gap-sm">
            <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center">
              <span className="material-symbols-outlined text-emerald-600">check_circle</span>
            </div>
            <div>
              <h3 className="text-headline-md text-on-surface">Processing Complete</h3>
              <p className="text-body-md text-on-surface-variant">Invoice ID #{result.id}</p>
            </div>
            <span className="ml-auto chip-success">
              {result.ocrConfidence ? `${result.ocrConfidence}% confidence` : result.status}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-md">
            {[
              ['Invoice #', result.invoiceNumber],
              ['Vendor', result.vendorName],
              ['Date', result.invoiceDate],
              ['Total', result.totalAmount ? `₹${Number(result.totalAmount).toLocaleString('en-IN')}` : null],
              ['GST', result.gstAmount ? `₹${Number(result.gstAmount).toLocaleString('en-IN')}` : null],
              ['Status', result.status],
            ].map(([label, value]) => value ? (
              <div key={label} className="bg-surface-container-low rounded-lg p-sm">
                <p className="text-label-md text-on-surface-variant">{label}</p>
                <p className="text-body-md font-medium text-on-surface mt-xs">{value}</p>
              </div>
            ) : null)}
          </div>

          {result.isDuplicate && (
            <div className="flex items-center gap-sm bg-amber-50 text-amber-800 rounded-lg p-md">
              <span className="material-symbols-outlined">warning</span>
              <p className="text-body-md">Possible duplicate of Invoice #{result.duplicateOfId}</p>
            </div>
          )}

          <div className="flex gap-md">
            <button onClick={() => navigate(`/validate/${result.id}`)} className="btn-primary flex-1 justify-center">
              <span className="material-symbols-outlined">fact_check</span>
              Review & Validate
            </button>
            <button onClick={handleReset} className="btn-ghost">
              <span className="material-symbols-outlined">add</span>Upload Another
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
