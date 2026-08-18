import api from './api'

export const trackingService = {
  lookup: (trackingNumber) =>
    api.get(`/tracking/${encodeURIComponent(trackingNumber)}`).then((res) => res.data),
}
