import { describe, expect, it } from "vitest";
import type { OpenAiModel } from "../../api/openAiModel/useOpenAiModels";
import { sortOpenAiModelsByHighestPrice } from "./OpenAiModelListPage";

function model(overrides: Partial<OpenAiModel>): OpenAiModel {
  return {
    id: 1,
    name: "Modelo base",
    code: "modelo-base",
    priceInputStandard: 0,
    priceInputCachedStandard: 0,
    priceOutputStandard: 0,
    priceInputBatch: 0,
    priceInputCachedBatch: 0,
    priceOutputBatch: 0,
    acceptsImageInput: false,
    ...overrides,
  };
}

describe("sortOpenAiModelsByHighestPrice", () => {
  it("ordena os modelos do mais caro para o mais barato pelo maior preço cadastrado", () => {
    const cheap = model({ id: 1, name: "Barato", priceOutputStandard: 2 });
    const expensive = model({ id: 2, name: "Caro", priceOutputBatch: 30 });
    const medium = model({
      id: 3,
      name: "Intermediário",
      priceInputStandard: 10,
    });

    const sorted = sortOpenAiModelsByHighestPrice([cheap, expensive, medium]);

    expect(sorted.map((item) => item.name)).toEqual([
      "Caro",
      "Intermediário",
      "Barato",
    ]);
  });

  it("mantém ordenação determinística por nome quando o maior preço empata", () => {
    const beta = model({ id: 1, name: "Beta", priceOutputStandard: 10 });
    const alpha = model({ id: 2, name: "Alpha", priceInputBatch: 10 });

    const sorted = sortOpenAiModelsByHighestPrice([beta, alpha]);

    expect(sorted.map((item) => item.name)).toEqual(["Alpha", "Beta"]);
  });
});
