import { cleanup, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import type { PropsWithChildren } from "react";
import {
  useProductProcessActivityExecutions,
  useRequestProductProcessActivityExecution,
} from "./useProductProcessActivityExecutions";

vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe("acompanhamento leve de tarefas", () => {
  it("keeps an explicit reference in reads and commands without a learning cycle", async () => {
    const reference = "product:9@private-validation-v2";
    vi.mocked(axios.get).mockImplementation(async (url) => ({
      data: String(url).endsWith("execution-progress")
        ? []
        : { currentExecutionReference: reference, activities: [] },
    }));
    vi.mocked(axios.post).mockResolvedValue({
      data: { sourceReference: reference },
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    );
    const view = renderHook(
      () => ({
        history: useProductProcessActivityExecutions(
          9,
          37,
          undefined,
          14,
          reference,
        ),
        command: useRequestProductProcessActivityExecution(
          9,
          37,
          undefined,
          reference,
        ),
      }),
      { wrapper },
    );
    await waitFor(() =>
      expect(view.result.current.history.isSuccess).toBe(true),
    );
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/37/products/9/activity-executions?chainId=14&sourceReference=product%3A9%40private-validation-v2",
      expect.anything(),
    );
    await view.result.current.command.mutateAsync({ activityId: "evidence" });
    expect(axios.post).toHaveBeenCalledWith(
      "/api/business-processes/37/products/9/activities/evidence/execution-requests?sourceReference=product%3A9%40private-validation-v2",
    );
    view.unmount();
    client.clear();
  });
  it("refreshes the full history on persisted changes, preserving scope and avoiding repeated audits", async () => {
    let state = "PENDING";
    let fail = false;
    let historyFailure = false;
    let historyReads = 0;
    let progressReads = 0;
    vi.mocked(axios.get).mockImplementation(async (url) => {
      if (String(url).endsWith("execution-progress")) {
        progressReads++;
        if (fail) throw new Error("Conexão interrompida");
        return { data: [{ taskId: 378, status: state, updatedAt: state }] };
      }
      historyReads++;
      if (historyFailure) throw new Error("Falha ao ler o resultado");
      return {
        data: {
          currentExecutionReference: "experiment:92",
          activities: [
            { activityId: "prototypeCorrection", operationalState: state },
          ],
        },
      };
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, gcTime: Infinity } },
    });
    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    );
    const view = renderHook(
      () => useProductProcessActivityExecutions(4, 70, 2, 14),
      { wrapper },
    );
    await waitFor(() => expect(historyReads).toBe(2));
    expect(view.result.current.data?.currentExecutionReference).toBe(
      "experiment:92",
    );
    await waitFor(() => expect(progressReads).toBeGreaterThan(1), {
      timeout: 5000,
    });
    expect(historyReads).toBe(2);
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/70/products/4/execution-progress",
      expect.objectContaining({ params: { sourceReference: "experiment:92" } }),
    );
    state = "IN_PROGRESS";
    await waitFor(
      () =>
        expect(view.result.current.data?.activities[0].operationalState).toBe(
          "IN_PROGRESS",
        ),
      { timeout: 5000 },
    );
    expect(historyReads).toBe(3);
    historyFailure = true;
    state = "BLOCKED";
    await waitFor(() => expect(view.result.current.isRefetchError).toBe(true), {
      timeout: 5000,
    });
    expect(view.result.current.data?.activities[0].operationalState).toBe(
      "IN_PROGRESS",
    );
    historyFailure = false;
    await waitFor(
      () =>
        expect(view.result.current.data?.activities[0].operationalState).toBe(
          "BLOCKED",
        ),
      { timeout: 6000 },
    );
    fail = true;
    await waitFor(() => expect(view.result.current.trackingError).toBe(true), {
      timeout: 6000,
    });
    expect(view.result.current.data?.activities[0].operationalState).toBe(
      "BLOCKED",
    );
    fail = false;
    state = "COMPLETED";
    await waitFor(
      () =>
        expect(view.result.current.data?.activities[0].operationalState).toBe(
          "COMPLETED",
        ),
      { timeout: 6000 },
    );
    expect(view.result.current.trackingError).toBe(false);
    expect(historyReads).toBeGreaterThanOrEqual(6);
    view.unmount();
    client.clear();
  }, 35000);
});
