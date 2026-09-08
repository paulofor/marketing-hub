import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import LearningCycleProcessEntry from "./LearningCycleProcessEntry";
import { cycleApi } from "../../api/learningCycle/useLearningCycles";
vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});
const entry = {
  chainDefinitionId: 13,
  chainName: "Cadeia PDE",
  parentProcessDefinitionId: 60,
  parentProcessName: "Venda e aprendizado",
  sequenceNumber: 6,
  activityId: "learningCycle",
  processDefinitionId: 72,
  processName: "Ciclos de aprendizado e vendas",
  integrated: true,
  canStartCycle: true,
  guidance: "Resultados alimentam a decisão; ajuste exige revalidação.",
  workspaceUrl:
    "/business-process-chains/learning-cycles?chainId=13&productId=4&cycleId=91",
  actionLabel: "Retomar ciclo #91",
  parentUrl: "/business-processes?processId=60&chainId=13",
  returnRoutes: [
    {
      label: "Utilidade",
      condition: "Primeiro ajuste precisa ser executável",
      processDefinitionId: 3,
      sequenceNumber: 3,
      processName: "Produto",
      url: "/business-processes?processId=3",
    },
  ],
};
function renderEntry() {
  return render(
    <MemoryRouter
      initialEntries={[
        "/business-processes?processId=60&chainId=13&productId=4",
      ]}
    >
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <LearningCycleProcessEntry processDefinitionId={60} />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}
describe("Entrada contextual do ciclo no BPM", () => {
  it("preserva produto, cadeia e ocorrência do backend sem mutação", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: entry });
    renderEntry();
    expect(
      await screen.findByRole("link", { name: "Retomar ciclo #91" }),
    ).toHaveAttribute("href", entry.workspaceUrl);
    expect(screen.getByText(/Subprocesso do processo 6/)).toBeVisible();
    expect(axios.get).toHaveBeenCalledWith(`${cycleApi}/entry`, {
      params: { processDefinitionId: 60, productId: 4, chainId: 13 },
    });
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("não inventa vínculo para um processo sem ciclo", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: null });
    const view = renderEntry();
    await vi.waitFor(() => expect(axios.get).toHaveBeenCalled());
    expect(view.container).toBeEmptyDOMElement();
  });
  it("mostra erro legível se o contrato chegar incompleto", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: { integrated: true } });
    renderEntry();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível consultar o vínculo",
    );
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });
  it("mantém falha de integração visível sem link de execução inferido", async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error("503"));
    renderEntry();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "orientação oficial",
    );
    expect(axios.post).not.toHaveBeenCalled();
  });
});
