import { describe, expect, it } from "vitest";
import { getStatusLearningAction } from "./AudioVideoStudioVideosAnalysisPage";

describe("AudioVideoStudioVideosAnalysisPage", () => {
  it("distingue falha tecnica de rejeicao editorial", () => {
    expect(getStatusLearningAction("FAILED")).toContain("Falha tecnica");
    expect(getStatusLearningAction("REJECTED")).toContain(
      "direitos de uso ou relevancia",
    );
  });
});
