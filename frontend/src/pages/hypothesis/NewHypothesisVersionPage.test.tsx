import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import NewHypothesisVersionPage from "./NewHypothesisVersionPage";

const mutateAsync = vi.fn();
const sourceHypothesis = vi.hoisted(() => ({
  id: "source-1",
  title: "MAN-H001",
  productName: "Agenda Cheia Nail Design",
  problem: "Oferta antiga",
  persona: "Nail designer",
  promise: "Promessa antiga",
  mechanism: "Mecanismo antigo",
  uniqueMechanism: "Diferencial antigo",
  entrega: "Mensagens de confirmação",
  successRule: "Venda",
  offerType: "TRIPWIRE",
  price: 27,
}));

vi.mock("../../api/hypothesis/useHypothesis", () => ({
  useHypothesis: () => ({
    data: sourceHypothesis,
    isLoading: false,
  }),
}));

vi.mock("../../api/hypothesis/useCreateHypothesisVersion", () => ({
  useCreateHypothesisVersion: () => ({ mutateAsync, isPending: false }),
}));

describe("NewHypothesisVersionPage", () => {
  beforeEach(() => mutateAsync.mockReset());

  it("preserva o contexto e envia o contrato comercial corrigido", async () => {
    mutateAsync.mockResolvedValue({ id: "version-2" });
    const user = userEvent.setup();
    render(
      <MemoryRouter
        initialEntries={["/niches/1/hypotheses/source-1/versions/new"]}
      >
        <Routes>
          <Route
            path="/niches/:nicheId/hypotheses/:hypothesisId/versions/new"
            element={<NewHypothesisVersionPage />}
          />
          <Route
            path="/niches/:nicheId/hypotheses/:hypothesisId"
            element={<p>Detalhe</p>}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText(/Agenda Cheia Nail Design/)).toBeInTheDocument();
    const delivery = screen.getByLabelText("Entrega");
    await user.clear(delivery);
    await user.type(delivery, "Posts personalizados, imagens e legendas");
    const price = screen.getByLabelText("Preço (R$)");
    await user.clear(price);
    await user.type(price, "67");
    await user.click(
      screen.getByRole("button", { name: "Criar versão em BACKLOG" }),
    );

    expect(mutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        sourceId: "source-1",
        entrega: "Posts personalizados, imagens e legendas",
        price: 67,
      }),
    );
    expect(await screen.findByText("Detalhe")).toBeInTheDocument();
  });
});
