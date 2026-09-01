import { describe, expect, it } from "vitest";
import { fingerprintDirectContact } from "./useExperimentDirectContactSample";

describe("fingerprintDirectContact", () => {
  it("normaliza e pseudonimiza e-mail sem depender de WebCrypto", () => {
    expect(fingerprintDirectContact("  MARIA@EXAMPLE.COM ", 89)).toBe(
      "ab11b96325a932145fce3c6d9629880f164cce846ad3eb81b4aa589bb6301534",
    );
  });

  it("normaliza pontuação do telefone antes de impedir duplicidade", () => {
    expect(fingerprintDirectContact("+55 (11) 99999-9999", 89)).toBe(
      "247d0dcd20e85ef7b0875245ceafb57b7afcc70988d6cebdb4442d1061758748",
    );
  });

  it("rejeita uma referência curta que não identifica um contato", () => {
    expect(() => fingerprintDirectContact("123", 89)).toThrow(
      "Informe um telefone ou e-mail válido",
    );
  });

  it("impede correlacionar o mesmo contato entre experimentos", () => {
    expect(fingerprintDirectContact("maria@example.com", 89)).not.toBe(
      fingerprintDirectContact("maria@example.com", 90),
    );
  });
});
