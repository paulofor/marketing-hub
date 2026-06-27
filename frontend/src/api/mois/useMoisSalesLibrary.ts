import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  MoisCollectedReferenceUrlSummary,
  MoisMarketWarmupOpportunityRankingResponse,
  MoisMarketWarmupReprocessStaleResponse,
  MoisMarketWarmupSearchAttemptListResponse,
  MoisMarketWarmupSignalListResponse,
  MoisMarketWarmupSourceListResponse,
  MoisMarketWarmupSummary,
  MoisDossierProductPipelineResponse,
  MoisSalesLibraryEntryPageResponse,
  MoisSalesLibraryJobPageResponse,
  MoisSalesLibraryPage,
  MoisSalesLibraryPageAnalysis,
  MoisSalesLibraryPageExecution,
  MoisSalesLibraryPageListResponse,
  MoisSalesLibraryPageSnapshot,
  MoisSalesLibraryPageSummary,
  MoisSalesLibraryReanalyzeResponse,
  MoisSalesLibrarySnapshotCaptureResponse,
  MoisSalesLibraryStatusUpdateResponse,
} from "./types";

function isHttpNotFound(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 404;
}

export function useMoisSalesLibraryEntries(
  workspaceId: string,
  page: number,
  pageSize: number,
) {
  return useQuery({
    queryKey: ["mois", "sales-library", "entries", workspaceId, page, pageSize],
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryEntryPageResponse>(
        "/api/mois/sales-library/entries",
        {
          params: { workspaceId, page, pageSize },
        },
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryJobs(
  workspaceId: string,
  page: number,
  pageSize: number,
  status?: string,
) {
  return useQuery({
    queryKey: [
      "mois",
      "sales-library",
      "jobs",
      workspaceId,
      page,
      pageSize,
      status,
    ],
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryJobPageResponse>(
        "/api/mois/sales-library/jobs",
        {
          params: { workspaceId, page, pageSize, status: status || undefined },
        },
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryPages(
  workspaceId: string,
  page: number,
  pageSize: number,
  marketWarmupFilter?: string,
  sort?: string,
) {
  return useQuery({
    queryKey: [
      "mois",
      "sales-library",
      "pages",
      workspaceId,
      page,
      pageSize,
      marketWarmupFilter || "ALL",
      sort || "RECENT_ANALYSIS",
    ],
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPageListResponse>(
        "/api/mois/sales-library/pages",
        {
          params: {
            workspaceId,
            page,
            pageSize,
            marketWarmupFilter:
              marketWarmupFilter && marketWarmupFilter !== "ALL"
                ? marketWarmupFilter
                : undefined,
            sort,
          },
        },
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryOpportunityRanking(
  workspaceId: string,
  limit = 5,
) {
  return useQuery({
    queryKey: [
      "mois",
      "sales-library",
      "market-warmup",
      "opportunity-ranking",
      workspaceId,
      limit,
    ],
    queryFn: async () => {
      const { data } =
        await axios.get<MoisMarketWarmupOpportunityRankingResponse>(
          "/api/mois/sales-library/market-warmup/opportunity-ranking",
          { params: { workspaceId, limit } },
        );
      return data;
    },
  });
}

export function useMoisSalesLibraryPageSummary(workspaceId: string) {
  return useQuery({
    queryKey: ["mois", "sales-library", "pages-summary", workspaceId],
    refetchInterval: 30000,
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPageSummary>(
        "/api/mois/sales-library/pages/summary",
        { params: { workspaceId } },
      );
      return data;
    },
  });
}

export function useMoisCollectedReferenceUrlSummary(workspaceId: string) {
  return useQuery({
    queryKey: [
      "mois",
      "sales-library",
      "collected-reference-url-summary",
      workspaceId,
    ],
    queryFn: async () => {
      const { data } = await axios.get<MoisCollectedReferenceUrlSummary>(
        "/api/mois/sales-library/collected-references/url-summary",
        { params: { workspaceId } },
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryPageAnalysis(pageId?: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "analysis", pageId],
    enabled: Boolean(pageId),
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPageAnalysis>(
        `/api/mois/sales-library/pages/${pageId}/analysis`,
      );
      return data;
    },
  });
}

export function useReanalyzeMoisSalesLibraryPage(workspaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (pageId: number) => {
      const { data } = await axios.post<MoisSalesLibraryReanalyzeResponse>(
        `/api/mois/sales-library/pages/${pageId}:reanalyze`,
      );
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "jobs", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages-summary", workspaceId],
      });
    },
  });
}

export function useUpdateMoisSalesLibraryPageStatus(workspaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      pageId,
      status,
    }: {
      pageId: number;
      status: "PENDING" | "ANULADO";
    }) => {
      const { data } = await axios.post<MoisSalesLibraryStatusUpdateResponse>(
        `/api/mois/sales-library/pages/${pageId}:status`,
        { status },
      );
      return data;
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "page", variables.pageId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "analysis", variables.pageId],
      });
      void queryClient.invalidateQueries({
        queryKey: [
          "mois",
          "sales-library",
          "page-executions",
          variables.pageId,
        ],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages-summary", workspaceId],
      });
    },
  });
}

export function useCaptureMoisSalesLibrarySnapshots(workspaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ limit, force }: { limit: number; force: boolean }) => {
      const { data } =
        await axios.post<MoisSalesLibrarySnapshotCaptureResponse>(
          "/api/mois/sales-library/snapshots:capture",
          { workspaceId, limit, force },
        );
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages-summary", workspaceId],
      });
    },
  });
}

export function getSalesLibraryJobBadgeClass(job: {
  status?: string;
  analysisStatus?: string;
}) {
  switch (job.status || job.analysisStatus) {
    case "DONE":
    case "CAPTURED":
    case "ANALYZED":
      return "bg-success-subtle text-success-emphasis";
    case "FAILED":
      return "bg-danger-subtle text-danger-emphasis";
    case "FETCHING":
    case "CAPTURING":
      return "bg-primary-subtle text-primary-emphasis";
    case "PENDING":
    case "BLOCKED_COOLDOWN":
      return "bg-warning-subtle text-warning-emphasis";
    default:
      return "bg-secondary-subtle text-secondary-emphasis";
  }
}

export function useMoisSalesLibraryPage(pageId?: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "page", pageId],
    enabled: Boolean(pageId),
    refetchInterval: 30000,
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPage>(
        `/api/mois/sales-library/pages/${pageId}`,
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryPageSnapshots(pageId?: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "page-snapshots", pageId],
    enabled: Boolean(pageId),
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPageSnapshot[]>(
        `/api/mois/sales-library/pages/${pageId}/snapshots`,
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryPageExecutions(pageId?: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "page-executions", pageId],
    enabled: Boolean(pageId),
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPageExecution[]>(
        `/api/mois/sales-library/pages/${pageId}/executions`,
      );
      return data;
    },
  });
}


export function useMoisDossierProductPipeline(pageId?: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "dossier-product-pipeline", pageId],
    enabled: Boolean(pageId),
    refetchInterval: 30000,
    queryFn: async () => {
      const { data } = await axios.get<MoisDossierProductPipelineResponse>(
        `/api/mois/sales-library/pages/${pageId}/dossier-product/pipeline`,
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryMarketWarmup(pageId?: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "market-warmup", pageId],
    enabled: Boolean(pageId),
    queryFn: async () => {
      try {
        const { data } = await axios.get<MoisMarketWarmupSummary>(
          `/api/mois/sales-library/pages/${pageId}/market-warmup`,
        );
        return data;
      } catch (error) {
        if (isHttpNotFound(error)) {
          return null;
        }
        throw error;
      }
    },
  });
}

export function useMoisSalesLibraryMarketWarmupSearchAttempts(
  pageId?: number,
  enabled = true,
) {
  return useQuery({
    queryKey: [
      "mois",
      "sales-library",
      "market-warmup",
      pageId,
      "search-attempts",
    ],
    enabled: Boolean(pageId) && enabled,
    queryFn: async () => {
      const { data } =
        await axios.get<MoisMarketWarmupSearchAttemptListResponse>(
          `/api/mois/sales-library/pages/${pageId}/market-warmup/search-attempts`,
        );
      return data;
    },
  });
}

export function useMoisSalesLibraryMarketWarmupSources(
  pageId?: number,
  enabled = true,
) {
  return useQuery({
    queryKey: ["mois", "sales-library", "market-warmup", pageId, "sources"],
    enabled: Boolean(pageId) && enabled,
    queryFn: async () => {
      const { data } = await axios.get<MoisMarketWarmupSourceListResponse>(
        `/api/mois/sales-library/pages/${pageId}/market-warmup/sources`,
      );
      return data;
    },
  });
}

export function useMoisSalesLibraryMarketWarmupSignals(
  pageId?: number,
  enabled = true,
) {
  return useQuery({
    queryKey: ["mois", "sales-library", "market-warmup", pageId, "signals"],
    enabled: Boolean(pageId) && enabled,
    queryFn: async () => {
      const { data } = await axios.get<MoisMarketWarmupSignalListResponse>(
        `/api/mois/sales-library/pages/${pageId}/market-warmup/signals`,
      );
      return data;
    },
  });
}

export function useReprocessStaleMoisSalesLibraryMarketWarmup(
  workspaceId: string,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (staleMinutes?: number) => {
      const { data } = await axios.post<MoisMarketWarmupReprocessStaleResponse>(
        "/api/mois/sales-library/market-warmup/jobs:reprocess-stale",
        { workspaceId, staleMinutes: staleMinutes ?? 120 },
      );
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages-summary", workspaceId],
      });
    },
  });
}

export function useStartMoisDossierPipeline(workspaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (pageId: number) => {
      await axios.post(
        `/api/internal/moissaleslibraryworker/dossieproduto/v1/intake/stage-executions/${pageId}/start`,
      );
    },
    onSuccess: (_, pageId) => {
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "page", pageId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "dossier-product-pipeline", pageId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages", workspaceId],
      });
      void queryClient.invalidateQueries({
        queryKey: ["mois", "sales-library", "pages-summary", workspaceId],
      });
    },
  });
}
