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

export async function downloadOprmNichoCnaeJobReport(jobId: number) {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/routine-research-cycle/stage-executions/${jobId}/report`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível baixar o relatório do job #${jobId} (status ${response.status}).`,
    );
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `nicho-cnae${jobId}.md`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function useOprmNichoCnaeJobs(page: number, size = 20) {
  return useQuery({
    queryKey: ["oprm", "nichocnae", "jobs", page, size],
    queryFn: () => fetchOprmNichoCnaeJobs(page, size),
    staleTime: 30_000,
  });
}
