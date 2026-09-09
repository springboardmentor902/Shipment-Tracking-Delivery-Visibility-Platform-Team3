export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "shiptrack_token";
const EMAIL_KEY = "shiptrack_email";
const ROLE_KEY = "shiptrack_role";

export function storeAuth(token: string, email: string, role: string) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(EMAIL_KEY, email);
  localStorage.setItem(ROLE_KEY, role);
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EMAIL_KEY);
  localStorage.removeItem(ROLE_KEY);
}

// Returns undefined during SSR, null when logged out, the token when logged in.
export function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredEmail(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(EMAIL_KEY);
}

export function getStoredRole(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(ROLE_KEY);
}

// Authenticated GET returning parsed JSON. Throws with the backend's
// error message (if any) so callers can show it directly.
export async function authGet<T>(path: string): Promise<T> {
  const token = getStoredToken();
  if (!token) throw new Error("Not logged in.");

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` },
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message || `Request failed (status ${response.status})`);
  }

  return data as T;
}

// Authenticated GET that downloads the response as a file (used for
// PDF/Excel report exports) and triggers the browser's save dialog.
export async function authDownload(path: string, fallbackFilename: string): Promise<void> {
  const token = getStoredToken();
  if (!token) throw new Error("Not logged in.");

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new Error(data?.message || `Download failed (status ${response.status})`);
  }

  const disposition = response.headers.get("Content-Disposition");
  const match = disposition?.match(/filename="?([^"]+)"?/);
  const filename = match?.[1] || fallbackFilename;

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
