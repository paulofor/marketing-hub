import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import axios from "axios";
import type {
  IndependentBusinessProcessCatalogItem,
  IndependentBusinessProcessExecution,
  IndependentBusinessProcessExecutionSummary,
  StartIndependentBusinessProcessExecution,
} from "./types";

const executionRootKey = ["independent-business-process-executions"];
const executionListKey = [...executionRootKey, "list"];
const executionPageSize = 10;

type IndependentBusinessProcessExecutionPage = {
  items: IndependentBusinessProcessExecutionSummary[];
  nextBeforeId?: number;
};

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
  return useInfiniteQuery<IndependentBusinessProcessExecutionPage>({
    queryKey: executionListKey,
    initialPageParam: undefined,
    queryFn: async ({ pageParam }) => {
      const response = await axios.get<
        IndependentBusinessProcessExecutionSummary[]
      >("/api/independent-business-process-executions", {
        params: {
          limit: executionPageSize + 1,
          ...(typeof pageParam === "number" ? { beforeId: pageParam } : {}),
        },
      });
      const items = response.data.slice(0, executionPageSize);
      return {
        items,
        nextBeforeId:
          response.data.length > executionPageSize
            ? items[items.length - 1]?.id
            : undefined,
      };
    },
    getNextPageParam: (lastPage) => lastPage.nextBeforeId,
    refetchInterval: (query) =>
      query.state.data?.pages[0]?.items.some((item) =>
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
