import api from './api'

export const userService = {
  getMe: (userId) =>
    api.get(`/users/${userId}/profile`).then((res) => res.data),

 updateMe: (userId, payload) =>
  api.put(`/users/${userId}/profile`, payload).then((res) => res.data),

  changePassword: (userId, payload) =>
    api.post(`/users/${userId}/password`, payload).then((res) => res.data),

  activity: ({ page = 0, size = 10 } = {}) =>
    Promise.resolve({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: page,
      size,
    }),
}