import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import {
  ProductProofSequenceEditor,
  validateProofSequence,
} from "./ProductProofSequenceEditor";
const shot = {
  reference: "internal://agent-tasks/123/visual-evidence/456#crop=0,0,900,1200",
  captionCueIndex: 0,
  minimumSeconds: 6,
};
describe("sequência de prova homologada", () => {
  it("preserva legado e valida ordem, tempo e referência antes do envio", () => {
    expect(validateProofSequence([], "A")).toBeUndefined();
    expect(validateProofSequence([shot], "A | B")).toBeUndefined();
    expect(validateProofSequence([shot, shot], "A | B")).toContain("ordem");
    expect(
      validateProofSequence(
        [{ ...shot, reference: "https://example.org/x.png" }],
        "A",
      ),
    ).toContain("homologada");
    expect(
      validateProofSequence([{ ...shot, minimumSeconds: 1 }], "A"),
    ).toContain("tempo");
  });
  it("mantém a numeração humana e emite o índice exato da legenda", () => {
    const change = vi.fn();
    render(<ProductProofSequenceEditor value={[shot]} onChange={change} />);
    fireEvent.change(screen.getByLabelText("Parte da legenda *"), {
      target: { value: "3" },
    });
    expect(change).toHaveBeenCalledWith([{ ...shot, captionCueIndex: 2 }]);
    fireEvent.change(screen.getByLabelText("Enquadramento"), {
      target: { value: "FULL_PRODUCT_PROOF_V1" },
    });
    expect(change).toHaveBeenCalledWith([
      { ...shot, layout: "FULL_PRODUCT_PROOF_V1" },
    ]);
    expect(
      validateProofSequence(
        [{ ...shot, layout: "FULL_PRODUCT_PROOF_V1" }],
        "A",
      ),
    ).toBeUndefined();
    fireEvent.change(screen.getByLabelText("Identificação junto da captura"), {
      target: { value: "Site Exemplo — demonstração" },
    });
    expect(change).toHaveBeenCalledWith([
      { ...shot, label: "Site Exemplo — demonstração" },
    ]);
    expect(
      validateProofSequence([{ ...shot, label: "x".repeat(49) }], "A"),
    ).toContain("48");
    expect(validateProofSequence([{ ...shot, label: "A\nB" }], "A")).toContain(
      "linha",
    );
    fireEvent.click(screen.getByText("Adicionar captura"));
    expect(change.mock.lastCall?.[0][1].captionCueIndex).toBe(1);
  });
});
