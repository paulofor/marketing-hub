import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";

export type PdeVpsStatus =
  "PLANNED" | "ACTIVE" | "STAGING" | "PAUSED" | "RETIRED";

export interface PdeVpsServer {
  id: number;
  name: string;
  provider: string;
  ipAddress: string;
  planName?: string | null;
  region?: string | null;
  vcpuCount?: number | null;
  ramGb?: number | null;
  storageGb?: number | null;
  monthlyCostBrl: number;
  productSlug?: string | null;
  environment: string;
  domains?: string | null;
  status: PdeVpsStatus;
  notes?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface PdeVpsSummary {
  totalMonthlyCostBrl: number;
  totalServers: number;
  activeServers: number;
  servers: PdeVpsServer[];
}

export interface SavePdeVpsServerRequest {
  name: string;
  provider: string;
  ipAddress: string;
  planName?: string;
  region?: string;
  vcpuCount?: number | null;
  ramGb?: number | null;
  storageGb?: number | null;
  monthlyCostBrl: number;
  productSlug?: string;
  environment: string;
  domains?: string;
  status: PdeVpsStatus;
  notes?: string;
}

export function usePdeVpsServers() {
  return useQuery({
    queryKey: ["pde", "vps"],
    queryFn: async () => {
      const { data } = await axios.get<PdeVpsSummary>("/api/pde/vps");
      return data;
    },
  });
}

export function useSavePdeVpsServer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      payload,
    }: {
      id?: number;
      payload: SavePdeVpsServerRequest;
    }) => {
      const { data } = id
        ? await axios.put<PdeVpsServer>(`/api/pde/vps/${id}`, payload)
        : await axios.post<PdeVpsServer>("/api/pde/vps", payload);
      return data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["pde", "vps"] });
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      toast.success("VPS PDE salva.");
    },
    onError: () => {
      toast.error("Não foi possível salvar a VPS PDE agora.");
    },
  });
}

export function useDeletePdeVpsServer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/pde/vps/${id}`);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["pde", "vps"] });
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      toast.success("VPS PDE removida.");
    },
    onError: () => {
      toast.error("Não foi possível remover a VPS PDE agora.");
    },
  });
}
