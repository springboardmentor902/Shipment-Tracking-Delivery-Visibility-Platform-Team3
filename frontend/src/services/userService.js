import api from './api'

export const userService = {
  getMe: () => api.get('/users/me').then((res) => res.data),
  updateMe: (payload) => api.put('/users/me', payload).then((res) => res.data),
  changePassword: (payload) => api.post('/users/me/password', payload).then((res) => res.data),
  activity: ({ page = 0, size = 10 } = {}) =>
    api.get('/users/me/activity', { params: { page, size } }).then((res) => res.data),
}
