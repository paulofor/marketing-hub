import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import {
  DeliveryExamplesEditor,
  validateDeliveryExamples,
} from "./DeliveryExamplesEditor";

describe("amostras de entrega", () => {
  it("preserva finalização legada e impede repetir arte ou iniciar sem fonte", () => {
    expect(validateDeliveryExamples([], "A | B")).toBeNull();
    expect(
      validateDeliveryExamples(
        [
          { visualAssetId: 1, captionCueIndex: 0 },
          { visualAssetId: 2, captionCueIndex: 1 },
        ],
        "A | B",
      ),
    ).toBeNull();
    expect(
      validateDeliveryExamples(
        [
          { visualAssetId: 1, captionCueIndex: 0 },
          { visualAssetId: 1, captionCueIndex: 1 },
        ],
        "A | B",
      ),
    ).toContain("diferentes");
    expect(
      validateDeliveryExamples(
        [
          { visualAssetId: 0, captionCueIndex: 0 },
          { visualAssetId: 2, captionCueIndex: 1 },
        ],
        "A | B",
      ),
    ).toContain("aprovada");
  });
  it("oferece escolhas da biblioteca sem autoaprovar ou pré-selecionar imagem", () => {
    const change = vi.fn();
    render(
      <DeliveryExamplesEditor
        examples={[1, 2].map((visualAssetId) => ({
          visualAssetId,
          commercialPlanId: 9,
          label: `Arte ${visualAssetId}`,
          url: "/fixture.png",
          sha256: "a",
          origin: "Fixture",
          rightsStatement: "Teste",
        }))}
        value={[]}
        captions="Veja a arte. | Conheça o kit."
        onChange={change}
        disabled={false}
        loading={false}
        error={false}
      />,
    );
    fireEvent.click(screen.getByText("Escolher amostras para a montagem"));
    expect(change).toHaveBeenCalledWith([
      { visualAssetId: 0, captionCueIndex: 0 },
      { visualAssetId: 0, captionCueIndex: 1 },
    ]);
  });
});
