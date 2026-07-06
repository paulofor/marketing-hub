import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import CommercialPlanningPage from "./CommercialPlanningPage";

afterEach(() => {
  cleanup();
});

describe("CommercialPlanningPage", () => {
  it("renderiza a tela de planejamento vazia para reconstrucao incremental", () => {
    render(<CommercialPlanningPage />);

    expect(screen.getByText("Planejamento")).toBeTruthy();
    expect(screen.getByLabelText("Área de planejamento")).toBeTruthy();
    expect(screen.queryByText("Novo Plano de Primeira Venda")).toBeNull();
  });
});
