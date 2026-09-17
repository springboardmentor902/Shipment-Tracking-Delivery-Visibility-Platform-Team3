import axios from "axios";

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
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
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredEmail() {
  return localStorage.getItem(EMAIL_KEY);
}

export function getStoredRole() {
  return localStorage.getItem(ROLE_KEY);
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = getStoredToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  console.log("API REQUEST:", {
    method: config.method,
    url: `${config.baseURL}${config.url}`,
  });

  return config;
});

api.interceptors.response.use(
  (response) => {
    console.log("API RESPONSE:", response.status, response.config.url);
    return response;
  },
  (error) => {
    console.error("AXIOS ERROR:", error);
    console.error("REQUEST URL:", error.config?.url);
    console.error("BASE URL:", error.config?.baseURL);
    console.error("ERROR MESSAGE:", error.message);
    console.error("ERROR RESPONSE:", error.response);
    return Promise.reject(error);
  }
);

export default api;