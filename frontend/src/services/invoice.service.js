import api from './api'

// ── Invoice endpoints ──────────────────────────────────────────
export const invoiceService = {
  upload: (file, onProgress) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/invoices/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: e => onProgress?.(Math.round((e.loaded * 100) / e.total))
    })
  },

  create: (data) => api.post('/invoices', data),

  getAll: (params) => api.get('/invoices', { params }),

  getMy: (params) => api.get('/invoices/my', { params }),

  getById: (id) => api.get(`/invoices/${id}`),

  update: (id, data) => api.put(`/invoices/${id}`, data),

  validate: (id, data) => api.post(`/invoices/${id}/validate`, data),

  approve: (id) => api.post(`/invoices/${id}/approve`),

  reject: (id, reason) => api.post(`/invoices/${id}/reject`, { reason }),

  delete: (id) => api.delete(`/invoices/${id}`),

  exportExcel: (params) => api.get('/invoices/export/excel', {
    params,
    responseType: 'blob'
  }),

  exportPdf: (id) => api.get(`/invoices/${id}/pdf`, { responseType: 'blob' }),
}

// ── Dashboard endpoints ────────────────────────────────────────
export const dashboardService = {
  getStats: () => api.get('/dashboard/stats')
}

// ── Vendor endpoints ───────────────────────────────────────────
export const vendorService = {
  getAll: (params) => api.get('/vendors', { params }),
  getById: (id) => api.get(`/vendors/${id}`),
  create: (data) => api.post('/vendors', data),
  update: (id, data) => api.put(`/vendors/${id}`, data),
  delete: (id) => api.delete(`/vendors/${id}`)
}

// ── Helpers ────────────────────────────────────────────────────
export const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
