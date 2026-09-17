import axios from "axios";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  "https://shiptrack-backend-rv62.onrender.com";


export const TOKEN_KEY = "shiptrack_token";
export const EMAIL_KEY = "shiptrack_email";
export const ROLE_KEY = "shiptrack_role";

export function storeAuth(token, email, role) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(EMAIL_KEY, email);
  localStorage.setItem(ROLE_KEY, role);
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EMAIL_KEY);
  localStorage.removeItem(ROLE_KEY);
}

export function getStoredToken() {
  if (typeof window === "undefined") {
    return null;
  }

  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredEmail() {
  if (typeof window === "undefined") {
    return null;
  }

  return localStorage.getItem(EMAIL_KEY);
}

export function getStoredRole() {
  if (typeof window === "undefined") {
    return null;
  }

  return localStorage.getItem(ROLE_KEY);
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem(TOKEN_KEY);

    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      console.error("Unauthorized request.");
    }

    if (status === 403) {
      console.error("Forbidden request. Check user role.");
    }

    if (status === 404) {
      console.error(
        "API endpoint not found:",
        error.config?.url
      );
    }

    if (!error.response) {
      console.error(
        "Backend server cannot be reached."
      );
    }

    return Promise.reject(error);
  }
);

export default api;
