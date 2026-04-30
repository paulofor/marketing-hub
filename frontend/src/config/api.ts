const envApiUrl = import.meta.env.VITE_API_URL?.trim();

// Default to same-origin so browser requests go through the current host
// (and reverse proxy), avoiding CORS issues when frontend runs on :5173.
export const apiBaseUrl = envApiUrl && envApiUrl.length > 0 ? envApiUrl : "";
