import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import OpenAiModelForm from "./OpenAiModelForm";

describe("OpenAiModelForm", () => {
  afterEach(() => cleanup());
  it("mostra modelos oficiais filtrados e exige seleção antes de salvar uma busca parcial", () => {
    const onSubmit = vi.fn();
    render(
      <OpenAiModelForm
        onSubmit={onSubmit}
        submitLabel="Buscar na OpenAI e salvar"
        officialModelCodes={["gpt-5.5", "gpt-5.5-mini", "gpt-4o-mini"]}
        officialModelPrices={{
          "gpt-5.5": {
            priceInputStandard: 5,
            priceInputCachedStandard: 0.5,
            priceOutputStandard: 30,
            priceInputBatch: 2.5,
            priceInputCachedBatch: 0.25,
            priceOutputBatch: 15,
          },
        }}
        nameOnly
      />,
    );

    fireEvent.change(screen.getByLabelText(/Nome do modelo/i), {
      target: { value: "5.5" },
    });

    expect(screen.getByText("gpt-5.5")).toBeTruthy();
    expect(screen.getByText("gpt-5.5-mini")).toBeTruthy();
    expect(screen.queryByText("gpt-4o-mini")).toBeNull();
    expect(screen.getByText(/Standard input: US\$ 5,00/i)).toBeTruthy();
    expect(screen.getByText(/Standard output: US\$ 30,00/i)).toBeTruthy();

    fireEvent.click(
      screen.getByRole("button", { name: /Buscar na OpenAI e salvar/i }),
    );

    expect(onSubmit).not.toHaveBeenCalled();
    expect(
      screen.getByText(/Escolha um modelo oficial da lista/i),
    ).toBeTruthy();
  });

  it("salva o código oficial após o usuário escolher um modelo da lista", () => {
    const onSubmit = vi.fn();
    render(
      <OpenAiModelForm
        onSubmit={onSubmit}
        submitLabel="Buscar na OpenAI e salvar"
        officialModelCodes={["gpt-5.5", "gpt-5.5-mini"]}
        nameOnly
      />,
    );

    fireEvent.change(screen.getByLabelText(/Nome do modelo/i), {
      target: { value: "5.5" },
    });
    fireEvent.click(screen.getByRole("button", { name: /gpt-5\.5-mini/i }));
    fireEvent.click(
      screen.getByRole("button", { name: /Buscar na OpenAI e salvar/i }),
    );

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ name: "gpt-5.5-mini" }),
    );
  });
});
