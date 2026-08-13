import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import CodexAuthReconnectPanel from "./CodexAuthReconnectPanel";

const mutate = vi.fn();
vi.mock("../../api/agent/useCodexAuthReconnect", () => ({
  useCodexAuthReconnect: () => ({
    data: {
      id: 12,
      agentId: 7,
      agentKey: "landing-generator",
      status: "AWAITING_CONFIRMATION",
      verificationUrl: "https://auth.openai.com/codex/device",
      userCode: "ABCD-EFGH",
      requestedBy: "admin",
      requestedAt: "2026-08-13T00:00:00Z",
    },
  }),
  useStartCodexAuthReconnect: () => ({ mutate, isPending: false, isError: false }),
}));

describe("CodexAuthReconnectPanel", () => {
  afterEach(() => cleanup());

  it("exibe somente o link e o código temporário devolvidos pelo backend", () => {
    render(<CodexAuthReconnectPanel agentId={7} nickname="Dédalo" onClose={vi.fn()} />);

    expect(screen.getByText("ABCD-EFGH")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Abrir autenticação OpenAI" })).toHaveAttribute(
      "target",
      "_blank",
    );
    expect(screen.queryByText(/token/i)).toBeInTheDocument();
  });

  it("permite fechar o painel sem alterar estado local de negócio", () => {
    const close = vi.fn();
    render(<CodexAuthReconnectPanel agentId={7} nickname="Dédalo" onClose={close} />);
    fireEvent.click(screen.getByRole("button", { name: "Fechar" }));
    expect(close).toHaveBeenCalledOnce();
  });
});
