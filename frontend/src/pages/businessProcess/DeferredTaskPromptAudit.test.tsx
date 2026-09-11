import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import DeferredTaskPromptAudit from "./DeferredTaskPromptAudit";

vi.mock("axios");
afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});
const request = {
  url: "/api/business-processes/70/products/4/tasks/396/prompt-audit?sourceReference=experiment%3A92",
  taskId: 396,
  sourceReference: "experiment:92",
};
const audit = {
  taskId: 396,
  sourceReference: "experiment:92",
  promptSent: "Contexto integral preservado",
  agentPromptPart: "Psique",
  activityPromptPart: "Cenário aderente",
};
function view() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return render(
    <QueryClientProvider client={client}>
      <DeferredTaskPromptAudit request={request} executionMode="MODEL" />
    </QueryClientProvider>,
  );
}

describe("Auditoria sob demanda", () => {
  it("keeps prompt requests out of activity tracking until explicitly opened", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: audit });
    const rendered = view();
    expect(axios.get).not.toHaveBeenCalled();
    fireEvent.click(
      screen.getByRole("button", { name: "Ver prompts desta tarefa" }),
    );
    expect(await screen.findByText(audit.promptSent)).toBeInTheDocument();
    expect(screen.getByText(audit.activityPromptPart)).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledWith(
      request.url,
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Ocultar prompts desta tarefa" }),
    );
    expect(screen.queryByText(audit.promptSent)).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Ver prompts desta tarefa" }),
    );
    expect(await screen.findByText(audit.promptSent)).toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledTimes(1);
    rendered.unmount();
  });

  it("shows failures and recovers without changing or recreating a task", async () => {
    vi.mocked(axios.get)
      .mockRejectedValueOnce(new Error("Falha local simulada"))
      .mockResolvedValueOnce({ data: audit });
    view();
    fireEvent.click(
      screen.getByRole("button", { name: "Ver prompts desta tarefa" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível carregar",
    );
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(await screen.findByText(audit.promptSent)).toBeInTheDocument();
    expect(axios.post).not.toHaveBeenCalled();
  });

  it.each([{ taskId: 397 }, { sourceReference: "experiment:91" }])(
    "rejects a mismatched audit %o",
    async (wrong) => {
      vi.mocked(axios.get).mockResolvedValue({ data: { ...audit, ...wrong } });
      view();
      fireEvent.click(
        screen.getByRole("button", { name: "Ver prompts desta tarefa" }),
      );
      await waitFor(() =>
        expect(screen.getByRole("alert")).toBeInTheDocument(),
      );
      expect(screen.queryByText(audit.promptSent)).not.toBeInTheDocument();
    },
  );
});
