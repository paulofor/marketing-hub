import { useQuery } from '@tanstack/react-query'
import { getLeadPortalFlows } from '../api/getLeadPortalFlows'

const getStatusFromError = (error: unknown): number | undefined => {
  if (!error || typeof error !== 'object') {
    return undefined
  }

  if ('status' in error && typeof (error as { status?: unknown }).status === 'number') {
    return (error as { status: number }).status
  }

  if (
    'response' in error &&
    typeof (error as { response?: { status?: unknown } }).response?.status === 'number'
  ) {
    return (error as { response: { status: number } }).response.status
  }

  return undefined
}

export const useLeadPortalFlows = (portalId: string) =>
  useQuery({
    queryKey: ['lead-portal-flows', portalId],
    queryFn: async () => {
      try {
        return await getLeadPortalFlows(portalId)
      } catch (error) {
        if (getStatusFromError(error) === 404) {
          return []
        }
        throw error
      }
    },
  })
