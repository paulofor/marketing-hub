import { cleanup, render, screen, within } from "@testing-library/react";
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

  it("prioriza a gestão de produto e de agentes no início do menu", () => {
    render(
      <MemoryRouter>
        <MainNavigation />
      </MemoryRouter>,
    );

    const navigation = screen.getByRole("navigation", {
      name: "Navegação principal",
    });
    const priorityLinks = within(navigation).getAllByRole("link").slice(0, 2);

    expect(priorityLinks).toHaveLength(2);
    expect(priorityLinks[0]).toHaveAccessibleName("Gestão de Produto");
    expect(priorityLinks[0]).toHaveAttribute("href", "/products");
    expect(priorityLinks[1]).toHaveAccessibleName("Gestão de Agentes");
    expect(priorityLinks[1]).toHaveAttribute("href", "/agents");
    expect(
      within(navigation).getAllByRole("link", { name: "Gestão de Produto" }),
    ).toHaveLength(1);
    expect(
      within(navigation).getAllByRole("link", { name: "Gestão de Agentes" }),
    ).toHaveLength(1);
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

  it("oferece acesso direto à execução de processos independentes", () => {
    render(
      <MemoryRouter>
        <MainNavigation />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: "Executar processos" }),
    ).toHaveAttribute("href", "/business-process-executions");
  });

  it("oferece acesso direto ao catálogo de tipos de produto", () => {
    render(
      <MemoryRouter>
        <MainNavigation />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: "Tipos de produto" }),
    ).toHaveAttribute("href", "/product-types");
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
