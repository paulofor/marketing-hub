import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import ImageGeneratorPage from "./ImageGeneratorPage";

vi.mock("../../app/breadcrumbs", () => ({
  useBreadcrumbs: vi.fn(),
}));

vi.mock("../../api/ai/useGenerateImage", () => ({
  useGenerateImage: vi.fn(() => ({
    data: {
      jobId: "img-batch-1",
      images: [
        {
          jobId: "img-ok",
          model: "gpt-5.6",
          serviceTier: "flex",
          outputFormat: "png",
          imageBase64: "abc123",
          variants: [],
          generatedAt: "2026-07-25T00:00:00Z",
        },
      ],
      failures: [
        {
          model: "gpt-image-2",
          message: "OpenAI recusou a geração da imagem: limite momentâneo.",
          finishedAt: "2026-07-25T00:00:01Z",
        },
      ],
    },
    error: null,
    isError: false,
    isPending: false,
    mutate: vi.fn(),
  })),
}));

afterEach(() => {
  cleanup();
});

describe("ImageGeneratorPage", () => {
  it("shows generated images with partial batch failure warning", () => {
    render(<ImageGeneratorPage />);

    expect(screen.getByText(/parte do lote comparativo falhou/i)).toBeTruthy();
    expect(screen.getByText(/gpt-image-2:/i)).toBeTruthy();
    expect(screen.getByText(/limite momentâneo/i)).toBeTruthy();
    expect(
      screen.getByAltText(/resultado gerado por IA com gpt-5.6/i),
    ).toBeTruthy();
  });
});
