const defaultApiBaseUrl = `${window.location.protocol}//${window.location.hostname}`;
const envApiUrl = import.meta.env.VITE_API_URL?.trim();

export const apiBaseUrl = envApiUrl && envApiUrl.length > 0 ? envApiUrl : defaultApiBaseUrl;
