import { AxiosError } from "axios";
import { describe, expect, it } from "vitest";
import { videoProductionError } from "./useVideoProductionCycles";

describe("causa da recusa de produção audiovisual", () => {
  it.each([67, 9202])(
    "expõe o perfil %i que precisa de roteiro",
    (profileId) => {
      const message = `Registre um roteiro aprovado no perfil de vídeo #${profileId}.`;
      const error = new AxiosError(
        "Request failed",
        "ERR_BAD_REQUEST",
        undefined,
        undefined,
        { data: { message }, status: 400 } as any,
      );
      expect(videoProductionError(error, "Falha de produção")).toBe(message);
    },
  );

  it("preserva o detalhe de um bloqueio financeiro e usa fallback sem contrato", () => {
    const error = new AxiosError(
      "Request failed",
      "ERR_BAD_REQUEST",
      undefined,
      undefined,
      { data: { detail: "Teto excedido" }, status: 409 } as any,
    );
    expect(videoProductionError(error, "Falha de produção")).toBe(
      "Teto excedido",
    );
    expect(
      videoProductionError(new Error("network"), "Falha de produção"),
    ).toBe("Falha de produção");
  });
});
