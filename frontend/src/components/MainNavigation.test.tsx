import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import MainNavigation from "./MainNavigation";

vi.mock("../api/useOpsMonitor", () => ({
  useOpsMonitorAvailability: () => ({
    data: [],
    isError: false,
    isLoading: false,
  }),
}));

describe("MainNavigation", () => {
  afterEach(() => {
    cleanup();
  });

  it("leva à tela inicial ao clicar no logo do Marketing Hub", () => {
    render(
      <MemoryRouter initialEntries={["/products"]}>
        <MainNavigation />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", {
        name: "Ir para a página inicial do Marketing Hub",
      }),
    ).toHaveAttribute("href", "/");
  });

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
