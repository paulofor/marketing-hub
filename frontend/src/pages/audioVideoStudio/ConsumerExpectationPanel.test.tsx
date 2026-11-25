import { render, screen, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import axios from "axios";
import ConsumerExpectationPanel from "./ConsumerExpectationPanel";
import protocolSource from "../../../../backend/ads-service/src/main/resources/consumer-expectation/v1/protocol.json?raw";
const protocol = JSON.parse(protocolSource);
vi.mock("axios");
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(axios.get).mockResolvedValue({ data: protocol });
});
afterEach(cleanup);
function mount() {
  render(
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
      <MemoryRouter>
        <ConsumerExpectationPanel />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
it("separa referências e hipóteses e não dispara tarefa ao carregar", async () => {
  mount();
  await screen.findByText("Base científica, dimensões e limites");
  expect(
    screen.getByRole("link", { name: "Rosengren et al. (2020)" }),
  ).toHaveAttribute("target", "_blank");
  expect(axios.post).not.toHaveBeenCalled();
});
it("envia contexto ao contrato real e mostra erro sem inventar alternativa", async () => {
  vi.mocked(axios.post).mockRejectedValue(new Error("HTTP 503"));
  mount();
  const user = userEvent.setup();
  for (const label of [
    "Público *",
    "Situação e canal *",
    "O que a pessoa espera *",
    "O que ela deseja alcançar *",
    "O que o produto realmente entrega *",
  ])
    await user.type(screen.getByLabelText(label), "Teste sintético");
  await user.click(
    screen.getByRole("button", { name: "Preparar três alternativas" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Não foi possível preparar",
  );
  expect(axios.post).toHaveBeenCalledWith(
    "/api/consumer-expectations/v1/plans/preview",
    expect.objectContaining({ evidenceBasis: "HYPOTHESIS", surface: "AD" }),
  );
});
it("limpa resultado antigo ao mudar identidade e informa ausência sem zero", async () => {
  mount();
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("ID do produto *"), "502");
  await user.type(screen.getByLabelText("ID do ciclo *"), "501");
  vi.mocked(axios.get).mockResolvedValue({
    data: {
      productId: 502,
      cycleId: 501,
      experimentId: 503,
      productVersion: "test-v1",
      guidance: "Sem medição conciliada.",
      nextOwner: "Hermes",
      metrics: { contributionBrl: null },
      cycleUrl: "/cycles/501",
    },
  });
  await user.click(
    screen.getByRole("button", { name: "Consultar aprendizado" }),
  );
  expect(await screen.findByText("Desconhecido")).toBeInTheDocument();
  await user.type(screen.getByLabelText("ID do ciclo *"), "2");
  expect(screen.queryByText("Sem medição conciliada.")).not.toBeInTheDocument();
});
