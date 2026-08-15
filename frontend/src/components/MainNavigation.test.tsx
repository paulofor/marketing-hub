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

  it("oferece acesso ao financeiro transversal de provedores de vídeo", () => {
    render(
      <MemoryRouter>
        <MainNavigation />
      </MemoryRouter>,
    );

    expect(
      screen
        .getAllByRole("link", { name: "Financeiro de vídeo" })
        .every(
          (link) => link.getAttribute("href") === "/financial/video-providers",
        ),
    ).toBe(true);
  });

  it("oferece acesso ao aprendizado governado dos agentes", () => {
    render(
      <MemoryRouter>
        <MainNavigation />
      </MemoryRouter>,
    );

    expect(
      screen
        .getAllByRole("link", { name: "Aprendizado dos agentes" })
        .every((link) => link.getAttribute("href") === "/agent-learning"),
    ).toBe(true);
  });
});
