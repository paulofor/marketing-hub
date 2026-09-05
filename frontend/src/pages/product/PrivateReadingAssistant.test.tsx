import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import PrivateReadingAssistant from "./PrivateReadingAssistant";
import type { ProductProcessActivityExecutionGroup } from "../../api/businessProcess/types";

const activity = {
  activityId: "privateReading1",
  executionControl: {
    workspaceReferenceId: 10,
    actionAvailable: true,
    confirmationTitle: "Primeira leitura privada",
    confirmationMessage: "Confirmo a leitura humana observada",
    confirmationToken: "CONFIRM:privateReading1",
  },
} as ProductProcessActivityExecutionGroup;

const workspace = {
  prototypeUrl: "https://v7.clubemusa.com.br/mira-private",
  prototypeVersion: "mira-private-v1",
  readingNumber: 1,
  participantReference: "PV-000000000001",
  evidenceId: "proof-1",
  signals: {
    EXPERIENCE_STARTED: true,
    VALUE_MOMENT: true,
    READY_RESULT_USED: true,
    PREFERRED_OVER_FREE: true,
    CHECKOUT_STARTED: true,
  },
  canRecord: true,
  status: "FINISHED",
  guidance: "Resultado importado; confirme a observação humana.",
};

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

function setup(data = workspace, pending = false) {
  vi.spyOn(axios, "get").mockResolvedValue({ data });
  const execute = vi.fn();
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  render(
    <QueryClientProvider client={client}>
      <PrivateReadingAssistant
        activity={activity}
        executing={pending}
        onExecute={execute}
      />
    </QueryClientProvider>,
  );
  return execute;
}

describe("leitura privada assistida", () => {
  it("mostra o acesso e remove a transcrição de código, métricas e evidência", async () => {
    setup();
    const link = await screen.findByRole("link", {
      name: /Abrir somente a tela do protótipo/,
    });
    expect(link).toHaveAttribute("href", workspace.prototypeUrl);
    expect(link).toHaveAttribute("target", "_blank");
    expect(
      screen.queryByLabelText(/Código pseudonimizado/),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText(/Evidência auditável/),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox")).not.toBeChecked();
    expect(
      screen.getByText(/o código já chega preenchido e protegido/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/este botão serve apenas para conferir/i),
    ).toBeInTheDocument();
  });

  it("envia somente referência real e confirmação humana explícita", async () => {
    const execute = setup();
    const checkbox = await screen.findByRole("checkbox");
    const button = screen.getByRole("button", {
      name: "Registrar resultado da leitura",
    });
    expect(button).toBeDisabled();
    fireEvent.click(checkbox);
    fireEvent.click(button);
    expect(execute).toHaveBeenCalledWith({
      activityId: "privateReading1",
      decision: {
        decision: "APPROVE",
        confirmationToken: "CONFIRM:privateReading1",
        structuredEvidence: {
          evidenceId: "proof-1",
          humanReadingConfirmed: true,
          observation: "",
        },
      },
    });
  });

  it("mantém registro desabilitado e orienta acesso quando ninguém leu", async () => {
    setup({
      ...workspace,
      canRecord: false,
      status: "NOT_STARTED",
      signals: {
        EXPERIENCE_STARTED: false,
        VALUE_MOMENT: false,
        READY_RESULT_USED: false,
        PREFERRED_OVER_FREE: false,
        CHECKOUT_STARTED: false,
      },
      guidance: "Aguardando leitura humana",
    });
    await screen.findByText("Aguardando leitura humana");
    expect(
      screen.getByRole("button", { name: "Registrar resultado da leitura" }),
    ).toBeDisabled();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  });

  it("mostra sinal negativo sem induzir o operador a alterá-lo", async () => {
    setup({
      ...workspace,
      signals: { ...workspace.signals, CHECKOUT_STARTED: false },
    });
    await screen.findByRole("checkbox");
    expect(screen.getByText("Não observado")).toBeInTheDocument();
  });

  it("preserva o acesso durante indisponibilidade sem reaproveitar sinais nem liberar registro", async () => {
    const execute = setup({
      ...workspace,
      status: "EVIDENCE_UNAVAILABLE",
      canRecord: false,
      guidance:
        "Não foi possível consultar o resultado. O acesso aceito está abaixo.",
    });
    await screen.findByRole("alert");
    expect(
      screen.getByRole("link", {
        name: /Abrir somente a tela do protótipo/,
      }),
    ).toHaveAttribute("href", workspace.prototypeUrl);
    expect(screen.getAllByText("Consulta indisponível")).toHaveLength(5);
    expect(screen.queryByText("Observado")).not.toBeInTheDocument();
    expect(screen.queryByText("Aguardando")).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Registrar resultado da leitura" }),
    ).toBeDisabled();
    expect(execute).not.toHaveBeenCalled();
  });

  it("limpa a confirmação ao atualizar e impede envio durante a consulta", async () => {
    const execute = setup();
    fireEvent.click(await screen.findByRole("checkbox"));
    let resolve!: (value: unknown) => void;
    vi.mocked(axios.get).mockImplementationOnce(
      () =>
        new Promise((r) => {
          resolve = r;
        }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: /Atualizar resultado/ }),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: /Atualizando/ }),
      ).toBeDisabled(),
    );
    expect(
      screen.getByRole("button", { name: "Registrar resultado da leitura" }),
    ).toBeDisabled();
    await act(async () => resolve({ data: workspace }));
    expect(screen.getByRole("checkbox")).not.toBeChecked();
    expect(execute).not.toHaveBeenCalled();
  });

  it("trata falha de consulta sem habilitar evidência antiga", async () => {
    setup();
    fireEvent.click(await screen.findByRole("checkbox"));
    vi.mocked(axios.get).mockRejectedValueOnce(new Error("unavailable"));
    fireEvent.click(
      screen.getByRole("button", { name: /Atualizar resultado/ }),
    );
    await screen.findByRole("alert");
    expect(
      screen.getByRole("button", { name: "Registrar resultado da leitura" }),
    ).toBeDisabled();
  });
});
