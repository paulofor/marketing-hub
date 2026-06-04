import { describe, expect, it } from "vitest";
import { extractQualityReviewSentScreenshots } from "./ExperimentGeraLandingExecutionDetailPage";

const MOBILE_IMAGE = "data:image/png;base64,aGVyb19tb2JpbGU=";
const DESKTOP_IMAGE =
  "https://cdn.example.com/generated/signed-image?id=desktop";

describe("extractQualityReviewSentScreenshots", () => {
  it("extrai screenshots enviados no formato Responses API da OpenAI", () => {
    const rawRequestBody = JSON.stringify({
      model: "gpt-5.5",
      input: [
        {
          role: "user",
          content: [
            { type: "input_text", text: "avalie a landing" },
            {
              type: "input_image",
              detail: "mobile",
              image_url: MOBILE_IMAGE,
            },
            {
              type: "input_image",
              label: "desktop completo",
              image_url: { url: DESKTOP_IMAGE },
            },
          ],
        },
      ],
    });

    expect(extractQualityReviewSentScreenshots(rawRequestBody)).toEqual([
      { src: MOBILE_IMAGE, label: "mobile" },
      { src: DESKTOP_IMAGE, label: "desktop completo" },
    ]);
  });

  it("remove imagens duplicadas e também extrai data URLs de payload bruto", () => {
    const rawRequestBody = `payload quebrado ${MOBILE_IMAGE} texto ${MOBILE_IMAGE}`;

    expect(extractQualityReviewSentScreenshots(rawRequestBody)).toEqual([
      { src: MOBILE_IMAGE, label: "Screenshot 1" },
    ]);
  });
});
