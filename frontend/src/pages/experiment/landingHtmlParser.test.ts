import { describe, expect, it } from "vitest";

import { parseLandingHtmlPayload } from "./landingHtmlParser";

describe("landingHtmlParser", () => {
  it("extracts html document from artifact envelope", () => {
    const raw = JSON.stringify({
      artifact: {
        artifactType: "experiment.landing.html",
        content: {
          htmlDocument:
            "<!doctype html><html><body><h1>Teste</h1></body></html>",
          summary: "Landing validada",
        },
      },
    });

    const parsed = parseLandingHtmlPayload(raw);
    expect(parsed?.summary).toBe("Landing validada");
    expect(parsed?.htmlDocument).toContain("<h1>Teste</h1>");
  });
});
