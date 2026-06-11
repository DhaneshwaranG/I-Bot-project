import axios from 'axios'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || '/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// Request interceptor – attach JWT
api.interceptors.request.use(config => {
    const token = localStorage.getItem('ibot_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
}, Promise.reject)

// Response interceptor – handle 401, auto-refresh
api.interceptors.response.use(
    res => res,
    async error => {
        const orig = error.config
        if (error.response?.status === 401 && !orig._retry) {
            orig._retry = true
            const refresh = localStorage.getItem('ibot_refresh')
            if (refresh) {
                try {
                    const {
                        data
                    } = await axios.post(
                        `${import.meta.env.VITE_API_URL}/api/auth/refresh`, {
                            refreshToken: refresh
                        }
                    )
                    const token = data.data.token
                    localStorage.setItem('ibot_token', token)
                    if (data.data.refreshToken) localStorage.setItem('ibot_refresh', data.data.refreshToken)
                    orig.headers.Authorization = `Bearer ${token}`
                    return api(orig)
                } catch {
                    localStorage.clear()
                    window.location.href = '/login'
                }
            } else {
                localStorage.clear()
                window.location.href = '/login'
            }
        }
        return Promise.reject(error)
    }
)

export default api