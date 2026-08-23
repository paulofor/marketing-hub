import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BrowserRouter } from "react-router-dom";
import ProductForm from "./ProductForm";
import { Product } from "../../api/product/useProducts";

vi.mock("../../api/useInstagramAccounts", () => ({
  useInstagramAccounts: () => ({ data: [] }),
}));
vi.mock("../../api/niche/useNiches", () => ({
  useNiches: () => ({ data: [] }),
}));
vi.mock("../../api/productType/useProductTypes", () => ({
  useProductTypes: () => ({
    data: [
      {
        id: 1,
        code: "PDE",
        name: "PDE - Produto Digital Experiencial",
        aliases: ["PDE"],
        status: "ACTIVE",
        productCount: 4,
      },
    ],
  }),
}));

function selectProductType() {
  fireEvent.change(screen.getByLabelText(/Tipo de produto/i), {
    target: { value: "1" },
  });
}

function renderForm(onSubmit = vi.fn()) {
  render(
    <BrowserRouter>
      <ProductForm isSaving={false} onSubmit={onSubmit} />
    </BrowserRouter>,
  );
}

afterEach(cleanup);

describe("ProductForm desire association map", () => {
  it("separates the commercial name from internal names and aliases", () => {
    const onSubmit = vi.fn();
    renderForm(onSubmit);

    fireEvent.change(
      screen.getByLabelText(/Nome comercial \(visível ao cliente\)/i),
      { target: { value: "Método MUSA" } },
    );
    fireEvent.change(screen.getByLabelText(/Nome interno\/de trabalho/i), {
      target: { value: "MUSA desejo v7" },
    });
    fireEvent.change(screen.getByLabelText(/Apelidos internos/i), {
      target: {
        value: "MUSA v7, vídeos orientados ao desejo\nprojeto presença",
      },
    });
    selectProductType();
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "Método MUSA",
        internalName: "MUSA desejo v7",
        aliases: ["MUSA v7", "vídeos orientados ao desejo", "projeto presença"],
        productTypeId: 1,
      }),
    );
    expect(screen.getByText(/nunca aparecem na oferta pública/i)).toBeTruthy();
  });

  it("limits fields to the backend schema before submit", () => {
    renderForm();

    selectProductType();

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

  it("saves an internal name without requiring a type on a legacy product", () => {
    const onSubmit = vi.fn();
    const onInternalNameSubmit = vi.fn();
    const legacyProduct = {
      id: 2,
      internalName: "",
      aliases: [],
      niche: "Manicure autônoma em domicílio",
      avatar: "",
      explicitPain: "",
      promise: "",
      uniqueMechanism: "",
      tripwire: "",
      riskReversal: "",
      socialProof: "",
      checkoutMonetization: "",
      funnel: "",
      creativeVolume: "",
      storytelling: "",
      aiCost: 0,
    } satisfies Product;

    render(
      <BrowserRouter>
        <ProductForm
          initialProduct={legacyProduct}
          isSaving={false}
          onSubmit={onSubmit}
          onInternalNameSubmit={onInternalNameSubmit}
        />
      </BrowserRouter>,
    );

    fireEvent.change(screen.getByLabelText(/Nome interno\/de trabalho/i), {
      target: { value: "Spica" },
    });
    expect(screen.getByRole("button", { name: "Salvar" })).toBeDisabled();
    fireEvent.click(
      screen.getByRole("button", { name: "Salvar somente nome interno" }),
    );

    expect(onInternalNameSubmit).toHaveBeenCalledWith({
      internalName: "Spica",
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("shows loading and error states for the isolated internal-name request", () => {
    const legacyProduct = {
      id: 2,
      internalName: "Spica",
      aliases: [],
      niche: "Manicure autônoma em domicílio",
      avatar: "",
      explicitPain: "",
      promise: "",
      uniqueMechanism: "",
      tripwire: "",
      riskReversal: "",
      socialProof: "",
      checkoutMonetization: "",
      funnel: "",
      creativeVolume: "",
      storytelling: "",
      aiCost: 0,
    } satisfies Product;

    const { rerender } = render(
      <BrowserRouter>
        <ProductForm
          initialProduct={legacyProduct}
          isSaving={false}
          isSavingInternalName
          onSubmit={vi.fn()}
          onInternalNameSubmit={vi.fn()}
        />
      </BrowserRouter>,
    );

    expect(
      screen.getByRole("button", { name: "Salvando nome interno..." }),
    ).toBeDisabled();

    rerender(
      <BrowserRouter>
        <ProductForm
          initialProduct={legacyProduct}
          isSaving={false}
          internalNameSaveFailed
          onSubmit={vi.fn()}
          onInternalNameSubmit={vi.fn()}
        />
      </BrowserRouter>,
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Não foi possível salvar o nome interno.",
    );
  });

  it("applies the three Agenda Cheia territories and keeps approval gates", () => {
    const onSubmit = vi.fn();
    renderForm(onSubmit);

    selectProductType();

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
