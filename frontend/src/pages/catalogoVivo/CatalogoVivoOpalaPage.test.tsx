import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, it, expect, vi } from "vitest";
import axios from "axios";
import CatalogoVivoOpalaPage from "./CatalogoVivoOpalaPage";
import OpalaAdoptionPanel from "./OpalaAdoptionPanel";
vi.mock("axios");
const fixture = {
  project: "Catálogo Vivo — Piloto Opala",
  origin: "DATABASE",
  ready: true,
  issues: [],
  pinnedTasks: 3,
  resolutionFailures: 0,
  items: [
    {
      binding: {
        id: 1,
        activityName: "Preparar entrada do PDE",
        agentId: 900001,
        agentName: "Dédalo",
        activeVersionId: 10,
      },
      versions: [
        {
          id: 10,
          versionNumber: 1,
          text: "Instrução {{TASK_CONTEXT}}",
          sha256: "hash",
          status: "REVIEWED",
          createdBy: "Pacote versionado",
        },
      ],
      events: [],
    },
  ],
};
function show(element: React.ReactNode) {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
          },
        })
      }
    >
      <MemoryRouter>{element}</MemoryRouter>
    </QueryClientProvider>,
  );
}
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});
it("salva nova versão e exige revisão sem sobrescrever a instrução ativa", async () => {
  vi.mocked(axios.get).mockResolvedValue({ data: fixture });
  vi.mocked(axios.post).mockResolvedValue({ data: { id: 11 } });
  show(<CatalogoVivoOpalaPage />);
  await screen.findByText("Preparar entrada do PDE");
  expect(
    (
      screen.getByRole("button", {
        name: "Salvar como nova versão",
      }) as HTMLButtonElement
    ).disabled,
  ).toBe(true);
  fireEvent.change(screen.getByLabelText("Responsável *"), {
    target: { value: "Operador" },
  });
  fireEvent.change(screen.getByLabelText("Motivo ou parecer *"), {
    target: { value: "Clareza comercial" },
  });
  fireEvent.change(screen.getByLabelText("Instrução da atividade *"), {
    target: { value: "Novo texto {{TASK_CONTEXT}}" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Salvar como nova versão" }),
  );
  await waitFor(() =>
    expect(axios.post).toHaveBeenCalledWith(
      "/api/catalogo-vivo/v1/opala/bindings/1/drafts",
      {
        text: "Novo texto {{TASK_CONTEXT}}",
        operatorName: "Operador",
        reason: "Clareza comercial",
      },
    ),
  );
  expect(axios.post).toHaveBeenCalledTimes(1);
});
it("envia todas as versões e o snapshot anterior para prevenir perda de ativação concorrente", async () => {
  vi.mocked(axios.get).mockResolvedValue({ data: fixture });
  vi.mocked(axios.post).mockResolvedValue({ data: fixture });
  show(<CatalogoVivoOpalaPage />);
  await screen.findByText("Preparar entrada do PDE");
  fireEvent.change(screen.getByLabelText("Responsável *"), {
    target: { value: "Revisor" },
  });
  fireEvent.change(screen.getByLabelText("Motivo ou parecer *"), {
    target: { value: "Versões conferidas" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Ativar conjunto selecionado" }),
  );
  await waitFor(() =>
    expect(axios.post).toHaveBeenCalledWith(
      "/api/catalogo-vivo/v1/opala/activation",
      {
        versions: { 1: 10 },
        expectedActiveVersions: { 1: 10 },
        operatorName: "Revisor",
        reason: "Versões conferidas",
      },
    ),
  );
});
it("apresenta todos os bloqueios recebidos do backend", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...fixture,
      ready: false,
      issues: ["Entrada sem texto", "Economia sem revisão"],
    },
  });
  show(<CatalogoVivoOpalaPage />);
  expect(await screen.findByText("Entrada sem texto")).toBeTruthy();
  expect(screen.getByText("Economia sem revisão")).toBeTruthy();
});
it("inicia somente a preparação do ciclo exato, preservando revisão e origem", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      canAdopt: true,
      adopted: false,
      reason: "Disponível",
      catalogUrl: "/catalogo-vivo/opala",
    },
  });
  vi.mocked(axios.post).mockResolvedValue({ data: {} });
  show(
    <OpalaAdoptionPanel productId={900004} cycleId={900002} revision={14} />,
  );
  await screen.findByText("Disponível");
  fireEvent.change(screen.getByLabelText("Responsável *"), {
    target: { value: "Operador" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Integrar e iniciar preparação" }),
  );
  await waitFor(() =>
    expect(axios.post).toHaveBeenCalledWith(
      "/api/catalogo-vivo/v1/opala/products/900004/cycles/900002/adoption",
      expect.objectContaining({
        expectedRevision: 14,
        operatorName: "Operador",
      }),
    ),
  );
});
it("usa a navegação persistida e não oferece repetir uma adesão já registrada", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      canAdopt: false,
      adopted: true,
      reason: "Integrado",
      preparationUrl:
        "/products/900004/value-chain-history/processes/77/activities?learningCycleId=900002&chainId=14",
      catalogUrl: "/catalogo-vivo/opala",
    },
  });
  show(
    <OpalaAdoptionPanel productId={900004} cycleId={900002} revision={14} />,
  );
  expect(
    (
      await screen.findByRole("link", {
        name: "Acompanhar preparação dos agentes",
      })
    ).getAttribute("href"),
  ).toContain("learningCycleId=900002&chainId=14");
  expect(
    screen.queryByRole("button", { name: "Integrar e iniciar preparação" }),
  ).toBeNull();
  expect(axios.post).not.toHaveBeenCalled();
});

it("ativa a versão exibida quando ainda não há uma versão ativa", async () => {
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      ...fixture,
      ready: false,
      items: [
        {
          ...fixture.items[0],
          binding: { ...fixture.items[0].binding, activeVersionId: null },
        },
      ],
    },
  });
  vi.mocked(axios.post).mockResolvedValue({ data: fixture });
  show(<CatalogoVivoOpalaPage />);
  await screen.findByText("Preparar entrada do PDE");
  expect(
    (
      screen.getByLabelText(
        "Versão para consultar ou ativar",
      ) as HTMLSelectElement
    ).value,
  ).toBe("10");
  fireEvent.change(screen.getByLabelText("Responsável *"), {
    target: { value: "Revisor" },
  });
  fireEvent.change(screen.getByLabelText("Motivo ou parecer *"), {
    target: { value: "Recuperação" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Ativar conjunto selecionado" }),
  );
  await waitFor(() =>
    expect(axios.post).toHaveBeenCalledWith(
      "/api/catalogo-vivo/v1/opala/activation",
      {
        versions: { 1: 10 },
        expectedActiveVersions: { 1: null },
        operatorName: "Revisor",
        reason: "Recuperação",
      },
    ),
  );
});
