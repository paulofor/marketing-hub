import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import ImageGeneratorPage from "./ImageGeneratorPage";

vi.mock("../../app/breadcrumbs", () => ({
  useBreadcrumbs: vi.fn(),
}));

vi.mock("../../api/product/useProducts", () => ({
  useProducts: vi.fn(() => ({
    data: [{ id: 1, name: "Agenda Cheia" }],
    isLoading: false,
  })),
}));

vi.mock("../../api/planning/useCommercialPlans", () => ({
  useCommercialPlans: vi.fn(() => ({
    data: [{ id: 2, name: "Primeiras vendas", experimentId: 85 }],
    isLoading: false,
  })),
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

vi.mock("../../api/ai/usePromoteGeneratedImage", () => ({
  usePromoteGeneratedImage: vi.fn(() => ({
    error: null,
    isError: false,
    isPending: false,
    isSuccess: false,
    mutate: vi.fn(),
  })),
}));

vi.mock("../../api/ai/usePromoteGeneratedLandingImage", () => ({
  usePromoteGeneratedLandingImage: vi.fn(() => ({
    error: null,
    isError: false,
    isPending: false,
    isSuccess: false,
    mutate: vi.fn(),
  })),
}));

vi.mock("../../api/experiment/useExperiments", () => ({
  useExperiments: vi.fn(() => ({
    data: [{ id: 88, name: "MAQA-H002-E001", productId: 1 }],
    isLoading: false,
  })),
}));

vi.mock("../../api/ai/useRecentImageGenerations", () => ({
  useRecentImageGenerations: vi.fn(() => ({
    data: [
      {
        jobId: "img-old",
        batchJobId: "img-batch-old",
        model: "gpt-image-2",
        prompt: "Prompt persistido",
        generatedAt: "2026-08-08T10:00:00Z",
      },
    ],
    isError: false,
    isLoading: false,
  })),
  useRecoverImageGeneration: vi.fn(() => ({
    data: undefined,
    error: null,
    isError: false,
    isPending: false,
    mutate: vi.fn(),
    reset: vi.fn(),
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
    expect(
      screen.getByRole("button", { name: /selecionar imagem/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /vincular e enviar ao aprovador/i }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: /aplicar ao rascunho/i }),
    ).toBeDisabled();
  });

  it("lists a persisted generation after selecting its commercial context", () => {
    render(<ImageGeneratorPage />);

    fireEvent.change(screen.getByLabelText(/produto/i), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByLabelText(/plano comercial/i), {
      target: { value: "2" },
    });

    expect(screen.getByText(/gerações recentes/i)).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /img-batch-old.*img-old/i }),
    ).toBeTruthy();
    expect(
      screen.getByText(/sem gerar novamente nem criar novo custo/i),
    ).toBeTruthy();
  });

  it("offers product-compatible experiment and canonical landing slots", () => {
    render(<ImageGeneratorPage />);

    fireEvent.change(screen.getByLabelText(/^produto/i), {
      target: { value: "1" },
    });

    expect(
      screen.getByRole("option", { name: /#88.*MAQA-H002-E001/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("option", { name: /hero principal/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("option", { name: /prova da entrega/i }),
    ).toBeTruthy();
  });
});
