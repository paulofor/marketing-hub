import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  IndependentBusinessProcessCatalogItem,
  IndependentBusinessProcessExecution,
  IndependentBusinessProcessExecutionSummary,
  StartIndependentBusinessProcessExecution,
} from "./types";

const executionRootKey = ["independent-business-process-executions"];
const executionListKey = [...executionRootKey, "list"];

export function useIndependentBusinessProcessCatalog() {
  return useQuery({
    queryKey: [...executionRootKey, "catalog"],
    queryFn: async () =>
      (
        await axios.get<IndependentBusinessProcessCatalogItem[]>(
          "/api/independent-business-process-executions/catalog",
        )
      ).data,
  });
}

export function useIndependentBusinessProcessExecutions() {
  return useQuery({
    queryKey: executionListKey,
    queryFn: async () =>
      (
        await axios.get<IndependentBusinessProcessExecutionSummary[]>(
          "/api/independent-business-process-executions",
        )
      ).data,
    refetchInterval: (query) =>
      query.state.data?.some((item) =>
        ["PENDING", "IN_PROGRESS"].includes(item.status),
      )
        ? 5000
        : false,
  });
}

export function useIndependentBusinessProcessExecution(executionId?: number) {
  return useQuery({
    queryKey: [...executionRootKey, "detail", executionId],
    enabled: executionId !== undefined,
    queryFn: async () =>
      (
        await axios.get<IndependentBusinessProcessExecution>(
          `/api/independent-business-process-executions/${executionId}`,
        )
      ).data,
    refetchInterval: (query) => {
      const status = query.state.data?.execution?.status;
      return status && ["PENDING", "IN_PROGRESS"].includes(status)
        ? 5000
        : false;
    },
  });
}

export function useStartIndependentBusinessProcessExecution() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (request: StartIndependentBusinessProcessExecution) =>
      (
        await axios.post<IndependentBusinessProcessExecution>(
          "/api/independent-business-process-executions",
          request,
        )
      ).data,
    onSuccess: (response) => {
      client.setQueryData(
        [...executionRootKey, "detail", response.execution.id],
        response,
      );
      void client.invalidateQueries({
        queryKey: executionListKey,
        exact: true,
      });
    },
  });
}
