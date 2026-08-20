import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  BusinessProcess,
  BusinessProcessExecutionResource,
  CreateBusinessProcess,
  SaveBusinessProcess,
} from "./types";

const key = ["business-processes"];

export function useBusinessProcesses() {
  return useQuery({
    queryKey: key,
    queryFn: async () =>
      (await axios.get<BusinessProcess[]>("/api/business-processes")).data,
  });
}

export function useBusinessProcessExecutionResources() {
  return useQuery({
    queryKey: ["business-process-execution-resources"],
    queryFn: async () =>
      (
        await axios.get<BusinessProcessExecutionResource[]>(
          "/api/business-process-execution-resources",
        )
      ).data,
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

export function useUpdateBusinessProcess() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      value,
    }: {
      id: number;
      value: SaveBusinessProcess;
    }) =>
      (await axios.put<BusinessProcess>(`/api/business-processes/${id}`, value))
        .data,
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
}

export function useDeleteBusinessProcess() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      axios.delete(`/api/business-processes/${id}`),
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
}
