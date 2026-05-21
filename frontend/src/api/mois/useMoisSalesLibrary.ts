import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  MoisSalesLibraryEntryPageResponse,
  MoisSalesLibraryJobPageResponse,
  MoisSalesLibraryPageAnalysis,
  MoisSalesLibraryPageListResponse,
  MoisSalesLibraryReanalyzeResponse,
  MoisSalesLibraryStatusUpdateResponse,
} from "./types";

export function useMoisSalesLibraryEntries(workspaceId: string, page: number, pageSize: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "entries", workspaceId, page, pageSize],
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryEntryPageResponse>("/api/mois/sales-library/entries", {
        params: { workspaceId, page, pageSize },
      });
      return data;
    },
  });
}

export function useMoisSalesLibraryJobs(workspaceId: string, page: number, pageSize: number, status?: string) {
  return useQuery({
    queryKey: ["mois", "sales-library", "jobs", workspaceId, page, pageSize, status],
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryJobPageResponse>("/api/mois/sales-library/jobs", {
        params: { workspaceId, page, pageSize, status: status || undefined },
      });
      return data;
    },
  });
}

export function useMoisSalesLibraryPages(workspaceId: string, page: number, pageSize: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "pages", workspaceId, page, pageSize],
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPageListResponse>("/api/mois/sales-library/pages", {
        params: { workspaceId, page, pageSize },
      });
      return data;
    },
  });
}

export function useMoisSalesLibraryPageAnalysis(pageId?: number) {
  return useQuery({
    queryKey: ["mois", "sales-library", "analysis", pageId],
    enabled: Boolean(pageId),
    queryFn: async () => {
      const { data } = await axios.get<MoisSalesLibraryPageAnalysis>(`/api/mois/sales-library/pages/${pageId}/analysis`);
      return data;
    },
  });
}

export function useReanalyzeMoisSalesLibraryPage(workspaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (pageId: number) => {
      const { data } = await axios.post<MoisSalesLibraryReanalyzeResponse>(`/api/mois/sales-library/pages/${pageId}:reanalyze`);
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["mois", "sales-library", "pages", workspaceId] });
      void queryClient.invalidateQueries({ queryKey: ["mois", "sales-library", "jobs", workspaceId] });
    },
  });
}


export function useUpdateMoisSalesLibraryPageStatus(workspaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ pageId, status }: { pageId: number; status: "PENDING" | "ANULADO" }) => {
      const { data } = await axios.post<MoisSalesLibraryStatusUpdateResponse>(`/api/mois/sales-library/pages/${pageId}:status`, { status });
      return data;
    },
    onSuccess: (_, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["mois", "sales-library", "pages", workspaceId] });
      void queryClient.invalidateQueries({ queryKey: ["mois", "sales-library", "analysis", variables.pageId] });
    },
  });
}

export function getSalesLibraryJobBadgeClass(job: { status?: string; analysisStatus?: string }) {
  switch (job.status || job.analysisStatus) {
    case "DONE":
      return "bg-success-subtle text-success-emphasis";
    case "FAILED":
      return "bg-danger-subtle text-danger-emphasis";
    case "FETCHING":
      return "bg-primary-subtle text-primary-emphasis";
    case "PENDING":
      return "bg-warning-subtle text-warning-emphasis";
    default:
      return "bg-secondary-subtle text-secondary-emphasis";
  }
}
