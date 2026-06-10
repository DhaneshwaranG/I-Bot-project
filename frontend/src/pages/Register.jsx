import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', department: '', phone: '' })
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})

  const validate = () => {
    const e = {}
    if (!form.name.trim()) e.name = 'Name is required'
    if (!form.email) e.email = 'Email is required'
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Invalid email'
    if (!form.password) e.password = 'Password is required'
    else if (form.password.length < 8) e.password = 'Minimum 8 characters'
    setErrors(e)
    return !Object.keys(e).length
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      await register(form)
      toast.success('Account created! Welcome to I-Bot.')
      navigate('/dashboard')
    } catch (err) {
      toast.error(err.response?.data?.error || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  const field = (key, label, type = 'text', placeholder = '', required = false) => (
    <div>
      <label className="input-label">{label}{required && ' *'}</label>
      <input
        type={type}
        className={`input-field ${errors[key] ? 'border-error' : ''}`}
        placeholder={placeholder}
        value={form[key]}
        onChange={e => setForm(p => ({ ...p, [key]: e.target.value }))}
        autoComplete={key}
      />
      {errors[key] && <p className="text-label-md text-error mt-xs">{errors[key]}</p>}
    </div>
  )

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-md">
      <div className="w-full max-w-md">
        <div className="text-center mb-xl">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary mb-md">
            <span className="material-symbols-outlined text-white text-3xl">smart_toy</span>
          </div>
          <h1 className="text-headline-lg font-bold text-primary">I-Bot</h1>
          <p className="text-body-md text-on-surface-variant mt-xs">Create your account</p>
        </div>

        <div className="card p-xl">
          <h2 className="text-headline-md text-on-surface mb-lg">Get started</h2>
          <form onSubmit={handleSubmit} className="space-y-md">
            {field('name', 'Full Name', 'text', 'Rajesh Kumar', true)}
            {field('email', 'Work Email', 'email', 'rajesh@company.com', true)}
            {field('password', 'Password', 'password', 'Min. 8 characters', true)}
            <div className="grid grid-cols-2 gap-md">
              {field('department', 'Department', 'text', 'Finance')}
              {field('phone', 'Phone', 'tel', '+91 9876543210')}
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full justify-center py-sm">
              {loading ? (
                <><span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Creating account…</>
              ) : (
                <><span className="material-symbols-outlined text-xl">person_add</span>Create account</>
              )}
            </button>
          </form>
          <p className="text-center text-body-md text-on-surface-variant mt-lg">
            Already have an account?{' '}
            <Link to="/login" className="text-primary font-medium hover:underline">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
