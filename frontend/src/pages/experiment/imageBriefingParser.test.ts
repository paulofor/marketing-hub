import { describe, expect, it } from "vitest";

import {
  hasImagePromptContent,
  parseImagePromptPayload,
} from "./imageBriefingParser";

describe("imageBriefingParser", () => {
  it("parses structured briefings from nested payload", () => {
    const raw = JSON.stringify({
      adImageBriefing: {
        briefings: [
          {
            mustMatchAdVariant: "dor",
            assetType: "estatico",
            imageTextMaxWords: 8,
            visualBriefing: "Mostrar o dashboard com destaque para o ganho",
          },
        ],
      },
    });

    const parsed = parseImagePromptPayload(raw);
    expect(hasImagePromptContent(parsed)).toBe(true);
    expect(parsed?.briefings[0].assetType).toBe("estatico");
    expect(parsed?.briefings[0].imageTextMaxWords).toBe(8);
  });

  it("returns undefined when no structured objects are present", () => {
    const parsed = parseImagePromptPayload("{\"briefings\":\"texto livre\"}");
    expect(parsed).toBeUndefined();
  });
});
