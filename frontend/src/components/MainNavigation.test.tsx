import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import MainNavigation from "./MainNavigation";

vi.mock("../api/useOpsMonitor", () => ({
  useOpsMonitorAvailability: () => ({
    data: [],
    isError: false,
    isLoading: false,
  }),
}));

describe("MainNavigation", () => {
  it("oferece acesso direto ao dossiê de oportunidades", () => {
    render(
      <MemoryRouter>
        <MainNavigation />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: "Dossiê de oportunidades" }),
    ).toHaveAttribute("href", "/opportunities");
  });
});
