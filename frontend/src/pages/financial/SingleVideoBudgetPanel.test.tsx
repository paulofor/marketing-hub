import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import axios from "axios";
import SingleVideoBudgetPanel from "./SingleVideoBudgetPanel";

vi.mock("axios");
let client: QueryClient;
let context: ReturnType<typeof fixture>;
const fixture = (
  productId = 4,
  cycleId = 21,
  experimentId = 114,
  projectId = 14,
  profileId = 69,
) => ({
  productId,
  cycleId,
  experimentId,
  projectId,
  profileId,
  chainDefinitionId: 27,
  productVersion: "test-video-v18",
  revision: 8,
  scope: "SINGLE_VIDEO_INCREMENTAL_ALL_IN_V1",
  currency: "USD",
  canAuthorize: true,
  blocker: null,
  startsProduction: false,
  guidance:
    "O recibo não libera consumo. Preflight integral e revisões continuam necessários.",
  authorizations: [] as any[],
});
beforeEach(() => {
  vi.resetAllMocks();
  context = fixture();
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).endsWith("/projects")
      ? [
          {
            id: context.projectId,
            title: "Projeto privado teste",
            productId: context.productId,
            experimentId: context.experimentId,
            campaignKey: context.productVersion,
            salesVideoProfileId: context.profileId,
          },
          {
            id: 666,
            title: "Outro produto",
            productId: 666,
            experimentId: context.experimentId,
            campaignKey: context.productVersion,
            salesVideoProfileId: 1,
          },
          {
            id: 667,
            title: "Outra versão",
            productId: context.productId,
            experimentId: context.experimentId,
            campaignKey: "old",
            salesVideoProfileId: 2,
          },
        ]
      : context,
  }));
});
afterEach(() => {
  cleanup();
  client?.clear();
});
function show() {
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <SingleVideoBudgetPanel
          productId={context.productId}
          cycleId={context.cycleId}
          chainId={27}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
async function fill() {
  await screen.findByLabelText("Projeto existente *");
  fireEvent.change(screen.getByLabelText("Projeto existente *"), {
    target: { value: String(context.projectId) },
  });
  fireEvent.change(screen.getByLabelText("Aprovado — teto total em US$ *"), {
    target: { value: "15" },
  });
  fireEvent.change(screen.getByLabelText("Operador que registra *"), {
    target: { value: "Codex em teste" },
  });
  fireEvent.change(
    screen.getByLabelText("Origem da autorização do cliente *"),
    {
      target: {
        value: "Autorização sintética única de USD 15 para todo o vídeo novo.",
      },
    },
  );
}
it.each([
  [4, 21, 114, 14, 69],
  [7, 99, 207, 33, 87],
])(
  "registra o contexto %s sem atribuir aprovação a um agente",
  async (p, c, e, v, profile) => {
    context = fixture(p, c, e, v, profile);
    vi.mocked(axios.post).mockImplementation(async (_url, body: any) => ({
      data: {
        ...context,
        revision: 9,
        authorizations: [
          {
            eventId: 55,
            reference: "internal://test/55",
            videoProjectId: v,
            profileId: profile,
            totalLimitUsd: 15,
            operatorName: body.operatorName,
            authorizationSource: body.authorizationSource,
            authorizedAt: "2026-11-05T14:00:00Z",
            current: true,
          },
        ],
      },
    }));
    show();
    await fill();
    expect(
      screen.queryByRole("option", { name: /Outro produto|Outra versão/ }),
    ).toBeNull();
    expect(
      screen.getByRole("button", { name: "Registrar autorização" }),
    ).toBeDisabled();
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(
      screen.getByRole("button", { name: "Registrar autorização" }),
    );
    await screen.findByText(/Recibo #55/);
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(vi.mocked(axios.post).mock.calls[0][1]).toMatchObject({
      videoProjectId: v,
      profileId: profile,
      totalLimitUsd: "15",
      experimentId: e,
      expectedRevision: 8,
      confirmed: true,
      operatorName: "Codex em teste",
    });
    expect(
      screen.getByRole("button", { name: "Registrar autorização" }),
    ).toBeDisabled();
    expect(screen.getByText(/O recibo não libera consumo/)).toBeInTheDocument();
  },
);
it("preserva a chave após erro incerto e exige confirmação novamente após editar", async () => {
  vi.mocked(axios.post).mockRejectedValue(new Error("network"));
  show();
  await fill();
  fireEvent.click(screen.getByRole("checkbox"));
  const button = screen.getByRole("button", { name: "Registrar autorização" });
  fireEvent.click(button);
  await screen.findByRole("alert");
  await waitFor(() => expect(button).not.toBeDisabled());
  fireEvent.click(button);
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));
  const calls = vi.mocked(axios.post).mock.calls;
  expect((calls[0][1] as any).requestKey).toBe((calls[1][1] as any).requestKey);
  await waitFor(() => expect(button).not.toBeDisabled());
  fireEvent.change(screen.getByLabelText("Aprovado — teto total em US$ *"), {
    target: { value: "14" },
  });
  expect(screen.getByRole("checkbox")).not.toBeChecked();
  expect(button).toBeDisabled();
});
it("desabilita envio e campos durante a gravação", async () => {
  vi.mocked(axios.post).mockImplementation(() => new Promise(() => {}));
  show();
  await fill();
  fireEvent.click(screen.getByRole("checkbox"));
  fireEvent.click(
    screen.getByRole("button", { name: "Registrar autorização" }),
  );
  await waitFor(() =>
    expect(
      screen.getByLabelText("Aprovado — teto total em US$ *"),
    ).toBeDisabled(),
  );
  expect(axios.post).toHaveBeenCalledTimes(1);
});
it("não apresenta autorização de contexto divergente nem permite envio", async () => {
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: String(url).endsWith("/projects")
      ? []
      : { ...context, productId: 999 },
  }));
  show();
  await screen.findByRole("alert");
  expect(
    screen.queryByRole("button", { name: "Registrar autorização" }),
  ).toBeNull();
  expect(axios.post).not.toHaveBeenCalled();
});
