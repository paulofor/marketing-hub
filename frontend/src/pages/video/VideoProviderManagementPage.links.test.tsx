import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import VideoProviderManagementPage from "./VideoProviderManagementPage";

vi.mock("../../api/salesVideo/useSalesVideoProviderScores", () => ({
  useSalesVideoProviderScores: () => ({
    data: [],
    isError: false,
    isFetching: false,
    refetch: vi.fn(),
  }),
}));

vi.mock("../../api/salesVideo/useSalesVideoProviderModels", () => ({
  useSalesVideoProviderModels: () => ({
    data: [
      "RUNWAY",
      "RUNWAY_SEEDANCE_2_5",
      "RUNWAY_HAILUO_3",
      "RUNWAY_GEN_4_TURBO",
      "RUNWAY_VEO_3_1_FAST",
      "RUNWAY_VEO_3_1",
    ].map((providerName, index) => ({
      id: index + 1,
      code: providerName.toLowerCase(),
      displayName: providerName.replace(/_/g, " "),
      providerName,
      providerFamily: "EXTERNAL_VIDEO_MODULE",
      adapterKey: "RUNWAY",
      externalModelId: "gen4.5",
      recommendedUse: "Teste",
      lifecycleStatus: "ACTIVE",
      clipDurationSeconds: 10,
      maxDirectDurationSeconds: 10,
      supportsHeroVideo: true,
      supportsSceneAssembly: true,
      requiresSourceImage: false,
      creditsUrl: "https://dev.runwayml.com/",
      documentationUrl: "https://docs.dev.runwayml.com/guides/models/",
      adapterVerified: true,
      pricingVerified: true,
      commercialLicenseVerified: true,
      qualityGateVerified: true,
    })),
    isError: false,
  }),
  useUpdateSalesVideoProviderModel: () => ({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
  }),
}));

vi.mock("../../api/planning/useProviderCreditPurchases", () => ({
  useProviderCreditPurchases: () => ({ data: [], isLoading: false }),
  useRegisterProviderCreditPurchase: () => ({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    isSuccess: false,
  }),
}));

describe("VideoProviderManagementPage links", () => {
  const renderPage = () =>
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <VideoProviderManagementPage />
      </QueryClientProvider>,
    );

  it("abre a compra de creditos da API Runway em uma nova aba segura", () => {
    renderPage();

    const links = screen.getAllByRole("link", { name: /comprar créditos/i });
    expect(links).toHaveLength(6);
    links.forEach((link) => {
      expect(link).toHaveAttribute("href", "https://dev.runwayml.com/");
      expect(link).toHaveAttribute("target", "_blank");
      expect(link).toHaveAttribute("rel", "noopener noreferrer");
    });
  });

  it("abre o formulário financeiro de compra de créditos", async () => {
    renderPage();
    await userEvent.click(
      screen.getAllByRole("button", { name: /registrar compra runway/i })[0],
    );
    expect(
      screen.getByRole("dialog", { name: /registrar créditos/i }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/data e hora/i)).toBeRequired();
    expect(screen.getByLabelText(/créditos adquiridos/i)).toBeRequired();
  });
});
