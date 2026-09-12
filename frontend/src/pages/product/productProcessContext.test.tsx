import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import fixture from "../../../../infra/testing/process-context-copy/fixture.json?raw";
import {
  processContextText,
  type ProcessContext,
} from "./productProcessContext";
import ProductProcessContextCopy from "./ProductProcessContextCopy";

function openPreview() {
  const details = screen.getByText("Ver contexto completo")
    .parentElement as HTMLDetailsElement;
  details.open = true;
  fireEvent(details, new Event("toggle"));
}

let context: ProcessContext;
const origin = "http://marketing-hub.test";
beforeEach(() => {
  context = JSON.parse(fixture);
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("Contexto do processo", () => {
  it("copia decisão pendente, destino e continuidade sem confundir teto com conclusão", () => {
    context.automation!.userAction = {
      code: "AUTHORIZE_VIDEO_BUDGET",
      title: "Falta informar o teto dos dois vídeos",
      reason: "Informe o valor em USD.",
      responsible: "Responsável pelo orçamento",
      actionLabel: "Informar teto dos vídeos",
      actionUrl:
        "/financial/videos?productId=92004&chainId=92014&cycleId=92002",
      afterAction: "Continue no briefing. Não autoriza mídia.",
      evidenceReference: null,
    };
    const text = processContextText(context, origin);
    expect(text).toContain(
      "Próxima ação necessária: Falta informar o teto dos dois vídeos",
    );
    expect(text).toContain(
      "http://marketing-hub.test/financial/videos?productId=92004&chainId=92014&cycleId=92002",
    );
    expect(text).toContain(
      "Depois desta ação: Continue no briefing. Não autoriza mídia.",
    );
  });
  it("preserva produto, versões, ciclo, aprendizado, tarefas e links com seu próprio destino", () => {
    context.history.activities[0].executionControl!.navigationUrl =
      "https://local.example/private";
    const text = processContextText(context, origin);
    for (const expected of [
      "Produto (nome interno): Vega (ID: 92004)",
      "Produto (nome comercial): Método MUSA — simulação local",
      "Cadeia de valor: #92014 · Cadeia PDE de teste · versão v3",
      "Processo: 4 — Comunicação e jornada de venda do PDE",
      "Versão do processo: v7 · definição ID 92063",
      "Ciclo: 2º ciclo (ID: 92002)",
      "Experimento: #92092",
      "Versão do produto: musa-pde-entry-v12-primeiro-ajuste-aplicavel",
      "Ciclo #92001 · Experimento #92091",
      "Aprendizado: Não atribuir abandono sem evidência.",
      "Execução: #920001",
      "Atividade atual: 4.1 — Materializar contrato de comunicação (communicationContract)",
      "Responsável: Responsável pelo produto · Tipo de executor: HUMAN",
      "Responsável: Backend · Tipo de executor: BACKEND",
      "Critério PROOF — Prova aprovada: Atendido",
      "Tarefa #920401",
      "Tarefa #920301",
      "Erro: Contrato anterior não comprovou o objetivo.",
      "Versão da atividade: v6 · definição histórica não informada",
      "Registro da atividade: histórico (fora da versão selecionada)",
      "Link do processo: http://marketing-hub.test/products/92004/value-chain-history/processes/92063/activities?chainId=92014&learningCycleId=92002\n",
      "Destino da atividade: https://local.example/private",
      "learningCycleId=92002#activity-commercialReview",
    ])
      expect(text).toContain(expected);
  });

  it("copia contagens e ausência de custo oficiais sem deduzir sucesso por quantidade de tarefas", () => {
    context.automation!.completedActivities = 2;
    context.automation!.remainingActivities = 1;
    context.automation!.completionPercentage = 50;
    const text = processContextText(context, origin);
    expect(text).toContain(
      "2 concluídas · 1 restantes · 1 dispensadas pelo fluxo · 4 no total · 50%",
    );
    expect(text).toContain("Objetivo do processo comprovado: Não");
    expect(text).toContain("Custo conhecido da execução: Não informado");
    expect(text).toContain("cobertura: NOT_REPORTED");
  });

  it("copia as chamadas oficiais e o retorno ao pai com o mesmo ciclo", () => {
    context.automation!.parentProcesses = [
      {
        processDefinitionId: 63,
        processName: "Comunicação",
        processVersion: 7,
        activityId: "creatives",
        activityName: "Criativos",
        navigationUrl:
          "/products/92004/value-chain-history/processes/63/activities?chainId=92014&learningCycleId=92002#activity-creatives",
      },
    ];
    context.automation!.subprocesses = [
      {
        processDefinitionId: 65,
        processName: "Landing",
        processVersion: 6,
        activityId: "destination",
        activityName: "Destino",
        navigationUrl:
          "/products/92004/value-chain-history/processes/65/activities?chainId=92014&learningCycleId=92002",
      },
    ];
    const text = processContextText(context, origin);
    expect(text).toContain("Comunicação");
    expect(text).toContain("Landing");
    expect(text).toContain(
      "http://marketing-hub.test/products/92004/value-chain-history/processes/63/activities?chainId=92014&learningCycleId=92002#activity-creatives",
    );
    expect(text).toContain(
      "http://marketing-hub.test/products/92004/value-chain-history/processes/65/activities?chainId=92014&learningCycleId=92002",
    );
  });
  it("declara dados ausentes sem substituir nome interno por comercial ou inventar ciclo", () => {
    context.cycle = null;
    context.cycleId = undefined;
    context.automation = undefined;
    context.history.productInternalName = " ";
    context.history.currentExecutionReference = undefined;
    context.history.activities[0].activityDefinitionId = undefined;
    context.history.activities[0].tasks = [];
    const text = processContextText(context, origin);
    expect(text).toContain("Produto (nome interno): Não informado (ID: 92004)");
    expect(text).toContain("Ciclo: Não informado pelo backend");
    expect(text).toContain(
      "Versão da atividade: Não informada · definição histórica não disponível",
    );
    expect(text).not.toMatch(/undefined|null|learningCycleId=|2º ciclo/);
  });

  it("usa ciclo oficial em vez do parâmetro antigo e mantém todos os aprendizados retornados", () => {
    context.cycleId = 999;
    context.chainId = 999;
    const text = processContextText(context, origin);
    expect(text).toContain("chainId=92014&learningCycleId=92002");
    expect(text).toContain(
      "Evidência: urn:marketinghub:test:process-context-copy",
    );
    expect(text).toContain("Próxima hipótese: Explicar o primeiro ajuste.");
    expect(text).not.toContain("999");
  });

  it.each([
    "productId",
    "processDefinitionId",
    "chainId",
    "learningCycleId",
    "sourceReference",
  ] as const)("não inclui uma execução de outro escopo (%s)", (field) => {
    Object.assign(context.automation!, {
      [field]: field === "sourceReference" ? "experiment:999" : 999,
    });
    const text = processContextText(context, origin);
    expect(text).toContain(
      "dados de execução fora do contexto selecionado não foram incluídos",
    );
    expect(text).not.toContain("Execução: #920001");
  });

  it("não empresta nome, versão ou numeração de outra cadeia", () => {
    context.position!.chainDefinitionId = 999;
    const text = processContextText(context, origin);
    expect(text).toContain("Cadeia de valor: #92014\n");
    expect(text).toContain("Processo: Número não informado —");
    expect(text).not.toContain("Cadeia PDE de teste");
    expect(text).not.toContain("Atividade: 4.");
  });

  it("mantém falha da consulta explícita e não copia prompts, payloads ou tokens de confirmação", () => {
    context.warnings = [
      "Consulta da execução falhou; exibindo a última leitura disponível.",
    ];
    const text = processContextText(context, origin);
    expect(text).toContain("Atenção: Consulta da execução falhou");
    expect(text).not.toMatch(
      /PROMPT_BRUTO|PAYLOAD_NAO|TOKEN_NAO|confirmationToken|evidenceJson/,
    );
  });

  it("aguarda a cópia assíncrona antes de confirmar e mantém a prévia idêntica ao texto copiado", async () => {
    let finish!: () => void;
    const writeText = vi.fn(
      (_text: string) =>
        new Promise<void>((resolve) => {
          finish = resolve;
        }),
    );
    vi.stubGlobal("isSecureContext", true);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    render(<ProductProcessContextCopy {...context} />);
    const button = screen.getByRole("button", {
      name: "Copiar contexto do processo",
    });
    button.focus();
    fireEvent.click(button);
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("aria-busy", "true");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    openPreview();
    expect(writeText.mock.calls[0][0]).toBe(
      screen.getByLabelText("Contexto completo do processo").textContent,
    );
    finish();
    await screen.findByText("Contexto copiado!");
    expect(button).toBeEnabled();
    expect(button).toHaveFocus();
  });

  it("oferece texto selecionável e nova tentativa se a área de transferência for bloqueada", async () => {
    vi.stubGlobal("isSecureContext", true);
    const writeText = vi.fn().mockRejectedValue(new Error("Permissão negada"));
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    render(<ProductProcessContextCopy {...context} />);
    const button = screen.getByRole("button", {
      name: "Copiar contexto do processo",
    });
    fireEvent.click(button);
    await screen.findByRole("alert");
    const manual = screen.getByRole("textbox", { name: /copiar manualmente/ });
    expect(manual).toHaveValue(
      processContextText(context, window.location.origin),
    );
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    writeText.mockResolvedValue(undefined);
    fireEvent.click(button);
    await waitFor(() =>
      expect(screen.queryByRole("alert")).not.toBeInTheDocument(),
    );
    expect(screen.getByRole("status")).toHaveTextContent("Contexto copiado!");
  });

  it("desabilita a cópia enquanto o contexto carrega e usa os dados atualizados após a consulta", () => {
    const { rerender } = render(
      <ProductProcessContextCopy {...context} loading />,
    );
    expect(
      screen.getByRole("button", { name: "Copiar contexto do processo" }),
    ).toBeDisabled();
    context.automation!.reason = "Nova pendência persistida";
    rerender(<ProductProcessContextCopy {...context} />);
    expect(
      screen.getByRole("button", { name: "Copiar contexto do processo" }),
    ).toBeEnabled();
    openPreview();
    expect(
      screen.getByLabelText("Contexto completo do processo"),
    ).toHaveTextContent("Nova pendência persistida");
  });
});
