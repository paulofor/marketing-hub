import { describe, expect, it } from "vitest";

import { parseAssetUploadResponse } from "./parseAssetUploadResponse";

describe("parseAssetUploadResponse", () => {
  it("retorna string vazia quando backend responde HTML inesperado", async () => {
    const response = new Response("<!doctype html><html><body>Not found</body></html>");

    await expect(parseAssetUploadResponse(response)).resolves.toBe("");
  });

  it("retorna URL quando backend responde JSON com campo url", async () => {
    const response = new Response(
      JSON.stringify({ url: "/uploads/subcard-1.jpg" }),
      { headers: { "Content-Type": "application/json" } },
    );

    await expect(parseAssetUploadResponse(response)).resolves.toBe(
      "/uploads/subcard-1.jpg",
    );
  });
});
