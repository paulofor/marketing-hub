import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { BusinessProcess, CreateBusinessProcess } from "./types";

const key = ["business-processes"];

export function useBusinessProcesses() {
  return useQuery({
    queryKey: key,
    queryFn: async () =>
      (await axios.get<BusinessProcess[]>("/api/business-processes")).data,
  });
}

export function useCreateBusinessProcess() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (value: CreateBusinessProcess) =>
      (await axios.post<BusinessProcess>("/api/business-processes", value))
        .data,
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
}

export function usePublishBusinessProcess() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      (
        await axios.post<BusinessProcess>(
          `/api/business-processes/${id}/publish`,
        )
      ).data,
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
}
