import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import axios from "axios";
import { useMdsArtifacts, useMdsHealth, useMdsRequests, useRetryMdsRequest } from "./useMdsAdmin";

vi.mock("axios");

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe("useMdsAdmin hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("envia filtros na listagem de requests", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    } as any);

    const { result } = renderHook(
      () =>
        useMdsRequests({
          status: "FAILED",
          tenantOrProduct: "tenant-a",
          from: "2026-04-20T00:00:00Z",
          to: "2026-04-27T23:59:59Z",
          page: 0,
          size: 20,
        }),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(axios.get).toHaveBeenCalledWith("/api/mds/requests", {
      headers: { "X-User-Role": "ADMIN" },
      params: {
        status: "FAILED",
        tenantOrProduct: "tenant-a",
        from: "2026-04-20T00:00:00Z",
        to: "2026-04-27T23:59:59Z",
        page: 0,
        size: 20,
      },
    });
  });


  it("carrega artefatos com envelope e lineage", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        requestId: 12,
        artifacts: [
          {
            artifactId: 401,
            artifactType: "mechanismSpec",
            schemaVersion: "v1",
            version: "v1",
            status: "VALIDATED",
            parentArtifactIds: [301],
            childArtifactIds: [501],
            content: { intervention: "micro-ciclo" },
          },
        ],
        lineage: [
          { id: 9001, parentArtifactId: 301, childArtifactId: 401, relationType: "DERIVED_FROM" },
        ],
      },
    } as any);

    const { result } = renderHook(() => useMdsArtifacts(12), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(axios.get).toHaveBeenCalledWith("/api/mds/requests/12/artifacts", {
      headers: { "X-User-Role": "ADMIN" },
    });
    expect(result.current.data?.artifacts[0].content.intervention).toBe("micro-ciclo");
    expect(result.current.data?.lineage[0].relationType).toBe("DERIVED_FROM");
  });


  it("consulta health do módulo MDS", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { status: "ok", module: "mds-admin-api" },
    } as any);

    const { result } = renderHook(() => useMdsHealth(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(axios.get).toHaveBeenCalledWith("/api/mds/health", {
      headers: { "X-User-Role": "ADMIN" },
    });
  });

  it("executa retry de request com endpoint correto", async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        requestId: 99,
        previousStatus: "FAILED",
        currentStatus: "PENDING",
        message: "retry accepted",
      },
    } as any);

    const { result } = renderHook(() => useRetryMdsRequest(), {
      wrapper: createWrapper(),
    });

    await result.current.mutateAsync(99);

    expect(axios.post).toHaveBeenCalledWith(
      "/api/mds/requests/99/retry",
      null,
      {
        headers: { "X-User-Role": "ADMIN" },
      },
    );
  });
});
