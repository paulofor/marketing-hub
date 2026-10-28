import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, renderHook, waitFor, act } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { PropsWithChildren } from "react";
import axios from "axios";
import { useVideoReviewSummary } from "./useCreativeVideoReviews";

vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

it("reutiliza o resumo recente ao navegar e respeita invalidação e outro produto", async () => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  vi.mocked(axios.get).mockResolvedValue({ data: { awaitingReviewCount: 1 } });
  const first = renderHook(() => useVideoReviewSummary({ productId: 31 }), {
    wrapper,
  });
  await waitFor(() => expect(first.result.current.isSuccess).toBe(true));
  first.unmount();
  const next = renderHook(() => useVideoReviewSummary({ productId: 31 }), {
    wrapper,
  });
  expect(next.result.current.data?.awaitingReviewCount).toBe(1);
  expect(axios.get).toHaveBeenCalledTimes(1);
  await act(async () => {
    await client.invalidateQueries({ queryKey: ["creative-video-reviews"] });
  });
  expect(axios.get).toHaveBeenCalledTimes(2);
  const other = renderHook(() => useVideoReviewSummary({ productId: 32 }), {
    wrapper,
  });
  await waitFor(() => expect(other.result.current.isSuccess).toBe(true));
  expect(axios.get).toHaveBeenLastCalledWith(
    "/api/creatives/video-review/summary",
    {
      params: { productId: 32 },
      signal: expect.any(AbortSignal),
    },
  );
  next.unmount();
  other.unmount();
  client.clear();
});

it("cancela consulta sem observador ao sair da tela", () => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  vi.mocked(axios.get).mockImplementation(() => new Promise(() => {}));
  const hook = renderHook(() => useVideoReviewSummary({ productId: 33 }), {
    wrapper,
  });
  const signal = vi.mocked(axios.get).mock.calls[0][1]?.signal;
  expect(signal?.aborted).toBe(false);
  hook.unmount();
  expect(signal?.aborted).toBe(true);
  client.clear();
});
