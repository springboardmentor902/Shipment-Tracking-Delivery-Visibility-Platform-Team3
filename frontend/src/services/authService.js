import api from './api'

export const ROLE_OPTIONS = [
  { value: 'CUSTOMER', label: 'Customer', hint: 'Track and view your own shipments' },
  { value: 'BUSINESS_CLIENT', label: 'Business Client', hint: 'Book and manage shipments' },
  { value: 'LOGISTICS_OPERATOR', label: 'Logistics Operator', hint: 'Move shipments and update status' },
  { value: 'SUPPORT_AGENT', label: 'Support Agent', hint: 'Assist customers with queries' },
  // ADMINISTRATOR is intentionally absent — the backend rejects admin self-signup with 403.
]

export const authService = {
  register: (payload) => api.post('/auth/register', payload).then((res) => res.data),
  login: (credentials) => api.post('/auth/login', credentials).then((res) => res.data),
}
