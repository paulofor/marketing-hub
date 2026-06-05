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
        nameOnly
      />,
    );

    fireEvent.change(screen.getByLabelText(/Nome do modelo/i), {
      target: { value: "5.5" },
    });

    expect(screen.getByText("gpt-5.5")).toBeTruthy();
    expect(screen.getByText("gpt-5.5-mini")).toBeTruthy();
    expect(screen.queryByText("gpt-4o-mini")).toBeNull();

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
