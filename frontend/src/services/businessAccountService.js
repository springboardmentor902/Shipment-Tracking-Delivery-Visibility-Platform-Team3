import api from './api'

export const businessAccountService = {
  getMine: () => api.get('/business-accounts/me').then((res) => res.data),
  create: (payload) => api.post('/business-accounts', payload).then((res) => res.data),
  updateMine: (payload) => api.put('/business-accounts/me', payload).then((res) => res.data),
  list: () => api.get('/business-accounts').then((res) => res.data),
}
