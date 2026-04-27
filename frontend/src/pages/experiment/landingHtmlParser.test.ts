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

  it("extracts dual html variants and chooses deterministic as primary preview", () => {
    const raw = JSON.stringify({
      artifact: {
        artifactType: "experiment.landing.html",
        content: {
          canonicalInputHash: "abc123",
          htmlVariants: {
            deterministic: {
              htmlDocument:
                "<!doctype html><html><body><h1>LHM</h1></body></html>",
              publicUrl: "https://lhm.example.com",
              validationSummary: {
                status: "PASS",
              },
            },
            ai: {
              htmlDocument:
                "<!doctype html><html><body><h1>IA</h1></body></html>",
              publicUrl: "https://ai.example.com",
              validationSummary: {
                status: "PASS",
              },
            },
          },
        },
      },
    });

    const parsed = parseLandingHtmlPayload(raw);
    expect(parsed?.canonicalInputHash).toBe("abc123");
    expect(parsed?.htmlDocument).toContain("<h1>LHM</h1>");
    expect(parsed?.deterministic?.publicUrl).toBe("https://lhm.example.com");
    expect(parsed?.ai?.publicUrl).toBe("https://ai.example.com");
  });
});
