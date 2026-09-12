import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import fixture from "../../../../infra/testing/process-context-copy/fixture.json?raw";
import ProductProcessContextCopy from "./ProductProcessContextCopy";
import {
  processContextText,
  type ProcessContext,
} from "./productProcessContext";

let context: ProcessContext;
const success = "Prompt copiado! Cole na conversa do AIHUB.";
const promptButton = () =>
  screen.getByRole("button", { name: "Prompt para AIHUB" });

beforeEach(() => {
  context = JSON.parse(fixture);
  vi.stubGlobal("isSecureContext", true);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("Pedido de ajuda ao AIHUB pelo processo", () => {
  it("copia pedido completo e o mesmo contexto oficial, com prévia e confirmação próprias", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    render(<ProductProcessContextCopy {...context} />);
    expect(screen.queryByLabelText("Prompt completo para AIHUB")).toBeNull();

    fireEvent.click(promptButton());
    await screen.findByText(success);
    const prompt: string = writeText.mock.calls[0][0];
    for (const part of [
      "Não estou conseguindo executar esse processo da tela.",
      "causa-raiz",
      "ambiente local da sandbox",
      "duas rodadas completas e consecutivas",
      "Você tem acesso SSH",
      "imagens Docker manualmente no host",
      "versionados neste repositório",
      "aguarde `ACTIVE`",
      "prompts e schemas versionados",
      "A solicitação pode demorar",
      "vendas, receitas e lucros",
      "diferencie hipótese",
    ])
      expect(prompt).toContain(part);
    expect(
      prompt.endsWith(processContextText(context, window.location.origin)),
    ).toBe(true);
    expect(prompt).not.toMatch(/PROMPT_BRUTO|PAYLOAD_NAO|TOKEN_NAO/);
    const details = screen.getByText("Ver prompt para AIHUB")
      .parentElement as HTMLDetailsElement;
    details.open = true;
    fireEvent(details, new Event("toggle"));
    expect(
      screen.getByLabelText("Prompt completo para AIHUB").textContent,
    ).toBe(prompt);

    fireEvent.click(
      screen.getByRole("button", { name: "Copiar contexto do processo" }),
    );
    await screen.findByText("Contexto copiado!");
    expect(writeText.mock.calls[1][0]).toBe(
      processContextText(context, window.location.origin),
    );
  });

  it("aguarda o clipboard, evita clique duplicado e restaura o foco após a confirmação", async () => {
    let finish!: () => void;
    const writeText = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          finish = resolve;
        }),
    );
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    render(<ProductProcessContextCopy {...context} />);
    const button = promptButton();
    button.focus();
    fireEvent.click(button);
    fireEvent.click(button);
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("aria-busy", "true");
    expect(writeText).toHaveBeenCalledTimes(1);
    expect(screen.queryByText(success)).toBeNull();
    finish();
    await screen.findByText(success);
    expect(button).toBeEnabled();
    expect(button).toHaveFocus();
  });

  it("oferece prompt integral selecionável quando a cópia falha e permite retentar", async () => {
    const writeText = vi.fn().mockRejectedValue(new Error("Permissão negada"));
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    render(<ProductProcessContextCopy {...context} />);
    fireEvent.click(promptButton());
    await screen.findByRole("alert");
    const manual = screen.getByRole("textbox", {
      name: "Prompt para AIHUB para copiar manualmente",
    });
    expect(manual).toHaveValue(writeText.mock.calls[0][0]);
    expect(screen.queryByText(success)).toBeNull();
    fireEvent.focus(manual);
    expect((manual as HTMLTextAreaElement).selectionEnd).toBe(
      writeText.mock.calls[0][0].length,
    );
    writeText.mockResolvedValue(undefined);
    fireEvent.click(promptButton());
    await screen.findByText(success);
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("aguarda o carregamento e copia o contexto novo após mudar de produto e ciclo", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    const { rerender } = render(
      <ProductProcessContextCopy {...context} loading />,
    );
    expect(promptButton()).toBeDisabled();
    rerender(<ProductProcessContextCopy {...context} />);
    fireEvent.click(promptButton());
    await screen.findByText(success);
    const next = structuredClone(context);
    next.history.productId = 92010;
    next.history.productInternalName = "Mira";
    next.history.activities = [];
    next.history.currentExecutionReference = undefined;
    next.cycle = null;
    next.cycleId = undefined;
    next.automation = undefined;
    next.warnings = [
      "A consulta da execução falhou; exibindo a última leitura disponível.",
    ];
    rerender(<ProductProcessContextCopy {...next} />);
    fireEvent.click(promptButton());
    await waitFor(() => expect(writeText).toHaveBeenCalledTimes(2));
    const prompt = writeText.mock.calls[1][0];
    expect(prompt).toContain("Produto (nome interno): Mira (ID: 92010)");
    expect(prompt).toContain("Ciclo: Não informado pelo backend");
    expect(prompt).toContain("Atenção: A consulta da execução falhou");
    expect(prompt).not.toMatch(
      /Execução: #920001|learningCycleId=92002|Tarefa #920401/,
    );
  });
});
