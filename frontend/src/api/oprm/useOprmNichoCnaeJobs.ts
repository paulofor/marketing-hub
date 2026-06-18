import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeJobSummary {
  id: number;
  cnaeCode: string;
  cnaeDescription: string;
  subniche: string | null;
  status: string;
  costUsd: number | string | null;
  lastStageCode: string | null;
  lastStageAt: string | null;
  reportUrl: string;
  trackingUrl: string;
}

export interface OprmNichoCnaeJobsPage {
  content: OprmNichoCnaeJobSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

async function fetchOprmNichoCnaeJobs(
  page: number,
  size: number,
): Promise<OprmNichoCnaeJobsPage> {
  const response = await fetch(
    buildApiUrl(`/api/oprm/nichocnae/jobs?page=${page}&size=${size}`),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os jobs OPRM NichoCNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNichoCnaeJobsPage;
}

export function useOprmNichoCnaeJobs(page: number, size = 20) {
  return useQuery({
    queryKey: ["oprm", "nichocnae", "jobs", page, size],
    queryFn: () => fetchOprmNichoCnaeJobs(page, size),
    staleTime: 30_000,
  });
}
