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
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <VideoProviderManagementPage />
      </QueryClientProvider>,
    );

  it("abre a compra de creditos da API Runway em uma nova aba segura", () => {
    renderPage();

    const link = screen.getByRole("link", { name: /comprar créditos/i });
    expect(link).toHaveAttribute("href", "https://dev.runwayml.com/");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("abre o formulário financeiro de compra de créditos", async () => {
    renderPage();
    await userEvent.click(
      screen.getAllByRole("button", { name: /registrar compra runway/i })[0],
    );
    expect(screen.getByRole("dialog", { name: /registrar créditos/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/data e hora/i)).toBeRequired();
    expect(screen.getByLabelText(/créditos adquiridos/i)).toBeRequired();
  });
});
