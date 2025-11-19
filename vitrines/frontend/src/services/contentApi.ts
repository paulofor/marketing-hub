import { environment } from '../config/environment'
import { ContentCard, HealthResponse, Role } from '../types/content'

const buildUrl = (path: string) => `${environment.apiBaseUrl}${path}`

export const fetchHealth = async (): Promise<HealthResponse> => {
  const response = await fetch(buildUrl('/health'))
  if (!response.ok) {
    throw new Error('Falha ao consultar o backend das vitrines')
  }

  return response.json()
}

export const fetchContentCards = async (role: Role): Promise<ContentCard[]> => {
  const response = await fetch(buildUrl(`/conteudos?role=${role}`))
  if (!response.ok) {
    throw new Error('Não foi possível carregar as vitrines')
  }

  return response.json()
}
