const defaultApiBaseUrl = 'http://localhost:8085/vitrines/api'

export const environment = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? defaultApiBaseUrl
}
