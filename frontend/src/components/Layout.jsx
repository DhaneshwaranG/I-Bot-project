import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'

const NAV = [
  { to: '/dashboard', icon: 'dashboard', label: 'Dashboard' },
  { to: '/upload', icon: 'upload_file', label: 'Upload Invoice' },
  { to: '/history', icon: 'receipt_long', label: 'Invoice History' },
  { to: '/profile', icon: 'person', label: 'Profile' },
]

export default function Layout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const handleLogout = () => {
    logout()
    toast.success('Logged out successfully')
    navigate('/login')
  }

  const navItemClass = ({ isActive }) =>
    `flex items-center gap-md px-md py-sm rounded-lg text-body-md font-medium transition-all cursor-pointer ${
      isActive
        ? 'bg-primary/10 text-primary'
        : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
    }`

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      {/* Sidebar overlay on mobile */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/30 z-30 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`fixed lg:relative inset-y-0 left-0 z-40 flex flex-col w-[280px] bg-surface-container-lowest border-r border-outline-variant transition-transform duration-300 ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
      }`}>
        {/* Logo */}
        <div className="flex items-center gap-md px-lg py-lg border-b border-outline-variant">
          <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center">
            <span className="material-symbols-outlined text-white text-xl">smart_toy</span>
          </div>
          <div>
            <h1 className="text-headline-md font-bold text-primary">I-Bot</h1>
            <p className="text-label-md text-on-surface-variant">Invoice Processing</p>
          </div>
        </div>

        {/* Nav links */}
        <nav className="flex-1 p-md space-y-xs overflow-y-auto scrollbar-thin">
          <p className="text-label-md text-on-surface-variant uppercase tracking-wider px-md mb-sm">Main Menu</p>
          {NAV.map(item => (
            <NavLink key={item.to} to={item.to} className={navItemClass} onClick={() => setSidebarOpen(false)}>
              <span className="material-symbols-outlined text-xl">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* User card */}
        <div className="p-md border-t border-outline-variant">
          <div className="flex items-center gap-sm p-sm rounded-lg hover:bg-surface-container-high transition-colors">
            <div className="w-9 h-9 rounded-full bg-primary-container flex items-center justify-center flex-shrink-0">
              <span className="text-primary font-semibold text-sm">
                {user?.name?.charAt(0)?.toUpperCase() || 'U'}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-body-md font-medium text-on-surface truncate">{user?.name}</p>
              <p className="text-label-md text-on-surface-variant truncate">{user?.email}</p>
            </div>
            <button onClick={handleLogout} className="btn-icon text-on-surface-variant hover:text-error" title="Logout">
              <span className="material-symbols-outlined text-xl">logout</span>
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top bar */}
        <header className="bg-surface-container-lowest border-b border-outline-variant flex items-center gap-md px-lg py-md z-20">
          <button className="btn-icon lg:hidden" onClick={() => setSidebarOpen(true)}>
            <span className="material-symbols-outlined">menu</span>
          </button>
          <div className="flex-1" />
          <button className="btn-icon relative">
            <span className="material-symbols-outlined">notifications</span>
            <span className="absolute top-1 right-1 w-2 h-2 bg-primary rounded-full" />
          </button>
          <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center">
            <span className="text-primary font-semibold text-sm">
              {user?.name?.charAt(0)?.toUpperCase() || 'U'}
            </span>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto scrollbar-thin">
          {children}
        </main>
      </div>
    </div>
  )
}
