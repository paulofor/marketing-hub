export type AccessType = 'FREE' | 'PREMIUM'
export type Role = 'ANON' | 'LEAD' | 'CLIENTE' | 'ADMIN'

export type ContentCard = {
  id: string
  title: string
  description: string
  accessType: AccessType
  locked: boolean
  coverImageUrl: string
  planId: string | null
}

export type HealthResponse = {
  status: string
  version: string
}
