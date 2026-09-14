import api from "./api";

export const trackingService = {
  // Public tracking lookup by tracking number
  lookup: (trackingNumber) =>
    api
      .get(
        `/tracking/${encodeURIComponent(
          trackingNumber.trim()
        )}`
      )
      .then((response) => {
        const data = response.data || {};

        return {
          ...(data.shipment || {}),
          events: Array.isArray(data.events)
            ? data.events
            : [],
        };
      }),
};