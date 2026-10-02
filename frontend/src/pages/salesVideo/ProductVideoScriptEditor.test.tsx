import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { SalesVideoScript } from "../../api/salesVideo/types";
import ProductVideoScriptEditor from "./ProductVideoScriptEditor";

afterEach(cleanup);
const script = {
  id: 81,
  version: 1,
  source: "MANUAL",
  status: "APPROVED",
  scriptText: "Veja o conteúdo pronto para publicar.",
  ctaText: "Peça uma amostra gratuita",
  captionText: "Exemplos demonstrativos.",
} satisfies SalesVideoScript;

describe("roteiro com identidade do produto e perfil", () => {
  it("preserva a chamada persistida e permite editar todos os campos", () => {
    const onSave = vi.fn();
    render(
      <ProductVideoScriptEditor
        script={script}
        productCta="Comprar kit"
        pending={false}
        onSave={onSave}
      />,
    );
    expect(screen.getByLabelText("Chamada para ação *")).toHaveValue(
      script.ctaText,
    );
    fireEvent.change(screen.getByLabelText("Chamada para ação *"), {
      target: { value: "Ver minha amostra" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Salvar roteiro aprovado" }),
    );
    expect(onSave).toHaveBeenCalledWith({
      scriptText: script.scriptText,
      ctaText: "Ver minha amostra",
      captionText: script.captionText,
    });
  });
  it("usa somente a CTA cadastrada para um perfil novo, sem roteiro ou legenda de outro produto", () => {
    render(
      <ProductVideoScriptEditor
        productCta="Organizar minha rotina"
        pending={false}
        onSave={vi.fn()}
      />,
    );
    expect(screen.getByLabelText("Roteiro completo *")).toHaveValue("");
    expect(screen.getByLabelText("Chamada para ação *")).toHaveValue(
      "Organizar minha rotina",
    );
    expect(screen.getByLabelText("Legenda da peça")).toHaveValue("");
    expect(
      screen.getByRole("button", { name: "Salvar roteiro aprovado" }),
    ).toBeDisabled();
  });
  it("reinicia o rascunho quando a identidade do perfil muda", () => {
    const { rerender } = render(
      <ProductVideoScriptEditor
        key="produto-a:perfil-21"
        script={script}
        pending={false}
        onSave={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText("Roteiro completo *"), {
      target: { value: "Rascunho ainda não salvo" },
    });
    rerender(
      <ProductVideoScriptEditor
        key="produto-b:perfil-99"
        script={{
          ...script,
          ctaText: "Ver meu exemplo",
          scriptText: "Rotina consultável",
        }}
        pending={false}
        onSave={vi.fn()}
      />,
    );
    expect(screen.getByLabelText("Roteiro completo *")).toHaveValue(
      "Rotina consultável",
    );
    expect(screen.getByLabelText("Chamada para ação *")).toHaveValue(
      "Ver meu exemplo",
    );
  });
  it("bloqueia campos e repetição de salvamento enquanto aguarda o backend", () => {
    render(
      <ProductVideoScriptEditor
        script={script}
        pending={true}
        onSave={vi.fn()}
      />,
    );
    expect(screen.getByLabelText("Roteiro completo *")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Salvando..." })).toBeDisabled();
    expect(document.querySelector(".spinner-border")).not.toBeNull();
  });
});
