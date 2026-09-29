import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { TargetingElement } from "../api/targeting/types";
import { TargetingElementCard } from "./TargetingElementCard";

vi.mock("axios");

const element: TargetingElement = {
  id: 397,
  marketNicheId: 34,
  type: "INTEREST",
  term: "Skin care",
  description: "Público interessado em cuidados com a pele.",
  source: "AI",
  status: "NEEDS_REVIEW",
  metaId: "6003657105838",
  metaKey: "Mario Badescu Skin Care",
};

function renderCard() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <TargetingElementCard element={element} />
    </QueryClientProvider>,
  );
}

describe("TargetingElementCard", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(cleanup);

  it("reenvia o elemento para resolução oficial da Meta pela ação do card", async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: { ...element, metaId: null } });
    renderCard();

    await userEvent.click(
      screen.getByRole("button", { name: "Reprocessar na Meta" }),
    );

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/targeting-elements/397/metaads/reprocess",
      );
    });
  });

  it("bloqueia ações e exibe carregamento durante o reprocessamento", async () => {
    let resolveRequest: ((value: { data: TargetingElement }) => void) | undefined;
    vi.mocked(axios.post).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRequest = resolve;
        }),
    );
    renderCard();

    const reprocessButton = screen.getByRole("button", {
      name: "Reprocessar na Meta",
    });
    await userEvent.click(reprocessButton);

    expect(reprocessButton).toBeDisabled();
    expect(reprocessButton.querySelector(".spinner-border")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Editar detalhes" })).toBeDisabled();

    resolveRequest?.({ data: { ...element, metaId: null } });
    await waitFor(() => expect(reprocessButton).not.toBeDisabled());
  });
});
