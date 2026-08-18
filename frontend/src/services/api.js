import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api',
  headers: { 'Content-Type': 'application/json' },
})

export const TOKEN_KEY = 'shiptrack_token'
export const USER_KEY = 'shiptrack_user'

// Attach the token to every outgoing request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Any 401 means the token is gone or expired — clear it and bounce to login.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const isAuthCall = error.config?.url?.includes('/auth/')

    const isPublicTrackingCall = error.config?.url?.includes('/tracking/')

    if (status === 401 && !isAuthCall && !isPublicTrackingCall) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

/** Turns any backend error into a single readable string for the UI. */
export function extractErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error.response?.data
  if (!data) {
    return error.request ? 'Cannot reach the server. Is the backend running on port 8081?' : fallback
  }
  // Bean validation errors come back as a field -> message map.
  if (data.errors && typeof data.errors === 'object') {
    return Object.values(data.errors).join(' ')
  }
  return data.message || data.error || fallback
}

export default api
