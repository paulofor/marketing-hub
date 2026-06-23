import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type OpsMonitorStatus = "ONLINE" | "DEGRADED" | "OFFLINE" | "UNKNOWN";

export interface OpsMonitorSummary {
  online: number;
  degraded: number;
  offline: number;
  unknown: number;
  openIncidents: number;
}

export interface ModuleAvailability {
  moduleCode: string;
  name: string;
  type: string;
  criticality: string;
  status: OpsMonitorStatus;
  lastCheckedAt?: string | null;
  lastResponseTimeMs?: number | null;
  lastError?: string | null;
}

export interface ModuleAvailabilityHistory {
  date: string;
  totalChecks: number;
  successfulChecks: number;
  failedChecks: number;
  availabilityPercentage: number;
  offlineSeconds: number;
  degradedSeconds: number;
}

export interface ModuleIncident {
  id: number;
  moduleCode: string;
  moduleName: string;
  status: string;
  severity: string;
  startedAt: string;
  endedAt?: string | null;
  durationSeconds?: number | null;
  summary: string;
  rootSignal?: string | null;
  lastError?: string | null;
}

export function useOpsMonitorSummary() {
  return useQuery({
    queryKey: ["ops-monitor", "summary"],
    queryFn: async () => {
      const { data } = await axios.get<OpsMonitorSummary>(
        "/api/ops-monitor/v1/summary",
      );
      return data;
    },
    refetchInterval: 30_000,
  });
}

export function useOpsMonitorAvailability() {
  return useQuery({
    queryKey: ["ops-monitor", "availability"],
    queryFn: async () => {
      const { data } = await axios.get<ModuleAvailability[]>(
        "/api/ops-monitor/v1/modules/availability",
      );
      return data;
    },
    refetchInterval: 30_000,
  });
}

export function useOpsMonitorAvailabilityHistory(moduleCode?: string) {
  return useQuery({
    queryKey: ["ops-monitor", "availability-history", moduleCode],
    enabled: Boolean(moduleCode),
    queryFn: async () => {
      const { data } = await axios.get<ModuleAvailabilityHistory[]>(
        `/api/ops-monitor/v1/modules/${moduleCode}/availability-history`,
      );
      return data;
    },
    refetchInterval: 60_000,
  });
}

export function useOpsMonitorOpenIncidents() {
  return useQuery({
    queryKey: ["ops-monitor", "incidents", "open"],
    queryFn: async () => {
      const { data } = await axios.get<ModuleIncident[]>(
        "/api/ops-monitor/v1/incidents/open",
      );
      return data;
    },
    refetchInterval: 30_000,
  });
}
