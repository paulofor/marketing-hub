import { render, screen } from "@testing-library/react";
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

describe("VideoProviderManagementPage links", () => {
  it("abre a compra de creditos da API Runway em uma nova aba segura", () => {
    render(<VideoProviderManagementPage />);

    const link = screen.getByRole("link", { name: /comprar créditos/i });
    expect(link).toHaveAttribute("href", "https://dev.runwayml.com/");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });
});
