import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import FacebookTokenUpdateDialog from "./FacebookTokenUpdateDialog";

const account = {
  id: 27,
  name: "Conta QA",
  currency: "BRL",
  appId: "12345",
  adAccountId: "7777",
  hasAccessToken: true,
};
const fetchMock = vi.fn();
beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  HTMLDialogElement.prototype.showModal = vi.fn(function (
    this: HTMLDialogElement,
  ) {
    this.setAttribute("open", "");
  });
});
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  fetchMock.mockReset();
});

function setup() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const onClose = vi.fn();
  const onSaved = vi.fn();
  render(
    <QueryClientProvider client={client}>
      <FacebookTokenUpdateDialog
        account={account}
        onClose={onClose}
        onSaved={onSaved}
      />
    </QueryClientProvider>,
  );
  return { client, onClose, onSaved };
}

it("envia apenas o token à conta correta, limpa o campo e não guarda segredo no cache", async () => {
  fetchMock.mockResolvedValue({
    ok: true,
    json: async () => ({
      accountId: 27,
      validatedAt: "2026-10-21T00:00:00",
      tokenRenewalEnabled: true,
    }),
  });
  const { client, onSaved, onClose } = setup();
  const input = screen.getByLabelText("Novo token de usuário");
  expect(input).toHaveAttribute("type", "password");
  fireEvent.change(input, { target: { value: " synthetic-token-qa " } });
  fireEvent.click(
    screen.getByRole("button", { name: "Validar e salvar token" }),
  );
  expect(input).toHaveValue("");
  await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
  expect(fetchMock).toHaveBeenCalledOnce();
  expect(fetchMock.mock.calls[0][0]).toMatch(
    /\/api\/accounts\/facebook\/27\/token$/,
  );
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    accessToken: "synthetic-token-qa",
  });
  expect(fetchMock.mock.calls[0][1]).toMatchObject({
    method: "POST",
    cache: "no-store",
    redirect: "error",
  });
  expect(onSaved.mock.calls[0][0]).toContain(
    "renovação automática continua habilitada",
  );
  expect(client.getMutationCache().getAll()).toHaveLength(0);
  expect(JSON.stringify(client.getQueryCache().getAll())).not.toContain(
    "synthetic-token-qa",
  );
});

it.each([400, 409, 422, 503, 500])(
  "recusa %s sem renderizar corpo externo ou manter o token",
  async (status) => {
    fetchMock.mockResolvedValue({
      ok: false,
      status,
      json: async () => ({ message: "echo synthetic-token-qa" }),
    });
    const { onSaved, onClose } = setup();
    fireEvent.change(screen.getByLabelText("Novo token de usuário"), {
      target: { value: "synthetic-token-qa" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Validar e salvar token" }),
    );
    await screen.findByRole("alert");
    expect(document.body.textContent).not.toContain("synthetic-token-qa");
    expect(screen.getByLabelText("Novo token de usuário")).toHaveValue("");
    expect(onSaved).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  },
);

it("bloqueia duplo clique durante a verificação e não retenta falha de transporte", async () => {
  let reject: (value: unknown) => void = () => {};
  fetchMock.mockReturnValue(
    new Promise((_resolve, fail) => {
      reject = fail;
    }),
  );
  setup();
  fireEvent.change(screen.getByLabelText("Novo token de usuário"), {
    target: { value: "synthetic-token-qa" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Validar e salvar token" }),
  );
  fireEvent.click(screen.getByRole("button", { name: "Validando na Meta…" }));
  expect(screen.getByRole("button", { name: "Cancelar" })).toBeDisabled();
  reject(new Error("synthetic-token-qa"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "confira o estado antes de reenviar",
  );
  expect(fetchMock).toHaveBeenCalledOnce();
  expect(document.body.textContent).not.toContain("synthetic-token-qa");
});

it("cancela sem enviar nem persistir credencial", () => {
  const { onClose } = setup();
  fireEvent.change(screen.getByLabelText("Novo token de usuário"), {
    target: { value: "synthetic-token-qa" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));
  expect(onClose).toHaveBeenCalledOnce();
  expect(fetchMock).not.toHaveBeenCalled();
});
