import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";
import type {
  MdsAdminArtifactsResponse,
  MdsAdminRequestDetail,
  MdsAdminRequestListResponse,
  MdsHealthResponse,
  MdsReportResponse,
  MdsRequestStatus,
  MdsRetryResponse,
} from "./types";

type ListParams = {
  status?: MdsRequestStatus | "";
  from?: string;
  to?: string;
  tenantOrProduct?: string;
  page?: number;
  size?: number;
};

const ADMIN_HEADERS = {
  "X-User-Role": "ADMIN",
};

export function useMdsRequests(params: ListParams, autoRefreshEnabled = true) {
  return useQuery({
    queryKey: ["mds", "requests", params, autoRefreshEnabled],
    queryFn: async () => {
      const { data } = await axios.get<MdsAdminRequestListResponse>("/api/mds/requests", {
        headers: ADMIN_HEADERS,
        params,
      });
      return data;
    },
    staleTime: 15_000,
    placeholderData: keepPreviousData,
    refetchInterval: autoRefreshEnabled ? 20_000 : false,
    refetchOnWindowFocus: autoRefreshEnabled,
  });
}

export function useMdsRequestDetail(requestId: number | null) {
  return useQuery({
    queryKey: ["mds", "request", requestId],
    enabled: Boolean(requestId),
    queryFn: async () => {
      const { data } = await axios.get<MdsAdminRequestDetail>(`/api/mds/requests/${requestId}`, {
        headers: ADMIN_HEADERS,
      });
      return data;
    },
    staleTime: 10_000,
    refetchInterval: 30_000,
  });
}

export function useMdsArtifacts(requestId: number | null) {
  return useQuery({
    queryKey: ["mds", "artifacts", requestId],
    enabled: Boolean(requestId),
    queryFn: async () => {
      const { data } = await axios.get<MdsAdminArtifactsResponse>(`/api/mds/requests/${requestId}/artifacts`, {
        headers: ADMIN_HEADERS,
      });
      return data;
    },
    staleTime: 30_000,
  });
}

export function useMdsReport(requestId: number | null) {
  return useQuery({
    queryKey: ["mds", "report", requestId],
    enabled: Boolean(requestId),
    queryFn: async () => {
      const { data } = await axios.get<MdsReportResponse>(`/api/mds/reports/${requestId}`, {
        headers: ADMIN_HEADERS,
      });
      return data;
    },
    staleTime: 30_000,
  });
}

export function useMdsHealth() {
  return useQuery({
    queryKey: ["mds", "health"],
    queryFn: async () => {
      const { data } = await axios.get<MdsHealthResponse>("/api/mds/health", {
        headers: ADMIN_HEADERS,
      });
      return data;
    },
    staleTime: 10_000,
    refetchInterval: 30_000,
  });
}

export function useRetryMdsRequest() {
  return useMutation({
    mutationFn: async (requestId: number) => {
      const { data } = await axios.post<MdsRetryResponse>(`/api/mds/requests/${requestId}/retry`, null, {
        headers: ADMIN_HEADERS,
      });
      return data;
    },
  });
}
