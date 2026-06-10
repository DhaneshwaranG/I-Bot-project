import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import toast from 'react-hot-toast'

export default function Profile() {
  const { user, updateUser } = useAuth()
  const [form, setForm] = useState({ name: user?.name || '', phone: user?.phone || '', department: user?.department || '' })
  const [pwd, setPwd] = useState({ current: '', newPwd: '', confirm: '' })
  const [saving, setSaving] = useState(false)

  const handleSaveProfile = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await api.put('/users/me', form)
      updateUser(form)
      toast.success('Profile updated!')
    } catch { toast.error('Update failed') } finally { setSaving(false) }
  }

  const infoItems = [
    { icon: 'email', label: 'Email', value: user?.email },
    { icon: 'badge', label: 'Role', value: user?.role?.replace('ROLE_', '') },
    { icon: 'fingerprint', label: 'User ID', value: `#${user?.id}` },
  ]

  return (
    <div className="p-lg max-w-2xl mx-auto space-y-lg">
      <h1 className="text-headline-lg text-on-surface">Profile</h1>

      {/* Avatar card */}
      <div className="card p-lg flex items-center gap-lg">
        <div className="w-20 h-20 rounded-full bg-primary-container flex items-center justify-center flex-shrink-0">
          <span className="text-primary text-3xl font-bold">{user?.name?.charAt(0)?.toUpperCase()}</span>
        </div>
        <div>
          <h2 className="text-headline-md text-on-surface">{user?.name}</h2>
          <p className="text-body-md text-on-surface-variant">{user?.email}</p>
          <p className="text-label-md text-primary mt-xs">{user?.department || 'No department'}</p>
        </div>
      </div>

      {/* Account info */}
      <div className="card overflow-hidden">
        <div className="p-md border-b border-outline-variant">
          <h3 className="text-headline-md text-on-surface">Account Information</h3>
        </div>
        <div className="divide-y divide-outline-variant">
          {infoItems.map(item => (
            <div key={item.label} className="flex items-center gap-md p-md">
              <span className="material-symbols-outlined text-on-surface-variant">{item.icon}</span>
              <div>
                <p className="text-label-md text-on-surface-variant uppercase tracking-wider">{item.label}</p>
                <p className="text-body-md font-medium text-on-surface">{item.value || '—'}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Edit profile */}
      <div className="card p-lg">
        <h3 className="text-headline-md text-on-surface mb-lg">Edit Profile</h3>
        <form onSubmit={handleSaveProfile} className="space-y-md">
          {[['name', 'Full Name', 'text'], ['phone', 'Phone Number', 'tel'], ['department', 'Department', 'text']].map(([key, label, type]) => (
            <div key={key}>
              <label className="input-label">{label}</label>
              <input
                type={type}
                className="input-field"
                value={form[key]}
                onChange={e => setForm(p => ({ ...p, [key]: e.target.value }))}
              />
            </div>
          ))}
          <button type="submit" disabled={saving} className="btn-primary">
            {saving ? <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : <span className="material-symbols-outlined">save</span>}
            Save Changes
          </button>
        </form>
      </div>
    </div>
  )
}
