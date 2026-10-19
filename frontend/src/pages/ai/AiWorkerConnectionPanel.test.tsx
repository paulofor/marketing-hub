import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { beforeEach, afterEach, describe, expect, it, vi } from "vitest";
import AiWorkerConnectionPanel from "./AiWorkerConnectionPanel";

const test = vi.hoisted(() => ({ query: {} as any, mutation: {} as any }));
vi.mock("../../api/ai/useAiWorkerConnection", async (importOriginal) => ({
  ...(await importOriginal<any>()),
  useAiWorkerConnection: () => test.query,
  useConnectAiWorker: () => test.mutation,
}));

describe("conexão administrativa do Worker IA", () => {
  afterEach(cleanup);
  beforeEach(() => {
    test.query = {
      data: { status: "DISCONNECTED", connected: false, workerAvailable: true },
      refetch: vi.fn(),
    };
    test.mutation = { mutate: vi.fn() };
  });

  it("solicita a conexão na própria tela sem formulário de credenciais", () => {
    render(<AiWorkerConnectionPanel />);
    fireEvent.click(screen.getByRole("button", { name: "Conectar Worker IA" }));
    expect(test.mutation.mutate).toHaveBeenCalledOnce();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  });

  it("mostra somente o link oficial e mantém código selecionável mesmo em HTTP", async () => {
    test.query.data = {
      status: "AWAITING_CONFIRMATION",
      userCode: "TEST-1234",
      verificationUrl: "https://auth.openai.com/codex/device",
      expiresAt: "2026-10-19T16:00:00Z",
    };
    render(<AiWorkerConnectionPanel />);
    expect(
      screen.getByRole("link", { name: "Abrir conexão na OpenAI" }),
    ).toHaveAttribute("href", "https://auth.openai.com/codex/device");
    expect(screen.getByText("TEST-1234")).toHaveClass("user-select-all");
    expect(
      screen.queryByRole("button", { name: "Conectar Worker IA" }),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Copiar código" }));
    expect(await screen.findByText(/Selecione o código abaixo/)).toBeVisible();
  });

  it("permite renovar código expirado e não revela link não oficial", () => {
    test.query.data = {
      status: "EXPIRED",
      verificationUrl: "https://invalid.example",
      userCode: "OLD",
    };
    render(<AiWorkerConnectionPanel />);
    expect(
      screen.getByRole("button", { name: "Gerar novo código" }),
    ).toBeVisible();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.queryByText("OLD")).not.toBeInTheDocument();
  });

  it("atualiza para conectado e não mostra senha ou código anterior", () => {
    test.query.data = {
      status: "AUTHENTICATED",
      connected: true,
      workerAvailable: true,
    };
    render(<AiWorkerConnectionPanel />);
    expect(screen.getByText(/Worker IA conectado/)).toBeVisible();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });

  it("falha de consulta esconde provas antigas e oferece atualização", () => {
    test.query.isError = true;
    test.query.data = {
      status: "AWAITING_CONFIRMATION",
      userCode: "OLD",
      verificationUrl: "https://auth.openai.com/codex/device",
    };
    render(<AiWorkerConnectionPanel />);
    expect(screen.queryByText("OLD")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Atualizar estado" }));
    expect(test.query.refetch).toHaveBeenCalledOnce();
  });
});
