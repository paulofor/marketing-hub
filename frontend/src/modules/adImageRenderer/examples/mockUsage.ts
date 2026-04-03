import { createMockRenderAdImageInput } from "../mockData";
import { RenderAdImagePayloadResult, renderAdImagePayloads } from "../renderAdImagePayloads";

export function buildMockImageRenderExample(): RenderAdImagePayloadResult {
  const input = createMockRenderAdImageInput();
  return renderAdImagePayloads(input);
}
