import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import axios from "axios";
import type { PropsWithChildren } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { usePromoteGeneratedImage } from "./usePromoteGeneratedImage";

vi.mock("axios");

afterEach(() => {
  vi.clearAllMocks();
});

describe("usePromoteGeneratedImage", () => {
  it("uploads, creates the creative and requests approval in order", async () => {
    const queryClient = new QueryClient();
    const post = vi.mocked(axios.post);
    post
      .mockResolvedValueOnce({ data: { url: "https://assets/image.png" } })
      .mockResolvedValueOnce({ data: { id: 261 } })
      .mockResolvedValueOnce({
        data: { id: 261, agentReviewStatus: "PENDING" },
      });
    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => usePromoteGeneratedImage(), {
      wrapper,
    });

    act(() => {
      result.current.mutate({
        experimentId: 85,
        jobId: "img-1",
        model: "gpt-image-2",
        prompt: "Orgulho profissional",
        format: "png",
        imageBase64: "YWJj",
        headline: "Seu talento merece destaque",
        primaryText: "Mostre seu trabalho com confiança.",
        description: "Prévia personalizada",
        cta: "LEARN_MORE",
        destinationUrl: "https://example.com/agenda-cheia",
      });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(post).toHaveBeenNthCalledWith(
      1,
      "/api/assets",
      expect.any(FormData),
    );
    expect(post).toHaveBeenNthCalledWith(
      2,
      "/api/experiments/85/creatives",
      expect.objectContaining({
        imageUrl: "https://assets/image.png",
        status: "DRAFT",
      }),
    );
    expect(post).toHaveBeenNthCalledWith(
      3,
      "/api/creatives/261/agent-review/request",
    );
  });
});
