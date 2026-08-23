import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import ProductForm from "./ProductForm";

vi.mock("../../api/useInstagramAccounts", () => ({
  useInstagramAccounts: () => ({ data: [] }),
}));
vi.mock("../../api/niche/useNiches", () => ({
  useNiches: () => ({ data: [] }),
}));

afterEach(cleanup);

describe("ProductForm desire association map", () => {
  it("limits fields to the backend schema before submit", () => {
    render(<ProductForm isSaving={false} onSubmit={vi.fn()} />);

    expect(screen.getByLabelText("Formato entregue")).toHaveAttribute(
      "maxlength",
      "64",
    );
    expect(screen.getByLabelText("Modo de entrega")).toHaveAttribute(
      "maxlength",
      "64",
    );
    expect(screen.getByLabelText("Versão da definição")).toHaveAttribute(
      "maxlength",
      "32",
    );
  });

  it("applies the three Agenda Cheia territories and keeps approval gates", () => {
    const onSubmit = vi.fn();
    render(<ProductForm isSaving={false} onSubmit={onSubmit} />);

    fireEvent.click(
      screen.getByRole("button", {
        name: "Aplicar mapa inicial do Agenda Cheia",
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    const payload = onSubmit.mock.calls[0][0];
    const map = JSON.parse(payload.desireAssociationMapJson);
    expect(map.territories.map((item: { code: string }) => item.code)).toEqual([
      "PROFESSIONAL_PRIDE",
      "RECOGNITION",
      "TRANQUILITY",
    ]);
    expect(map.measurementPlan.publicationRequires).toEqual([
      "AD_SPECIALIST_APPROVED",
      "HUMAN_APPROVED",
    ]);
    expect(map.prohibitedAssociations).toContain("agenda lotada garantida");
  });
});
