import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi, test, expect } from "vitest";
import axios from "axios";
import ProductRecordingUpload from "./ProductRecordingUpload";

vi.mock("axios", () => ({
  default: {
    post: vi.fn().mockResolvedValue({ data: {} }),
    isAxiosError: () => false,
  },
}));
URL.createObjectURL = vi.fn(() => "blob:private-capture");
URL.revokeObjectURL = vi.fn();

test("envia os mesmos bytes e manifesto ao projeto sem chamar render ou análise", async () => {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ProductRecordingUpload projectId={91014} />
    </QueryClientProvider>,
  );
  const button = screen.getByRole("button", {
    name: "Vincular gravação privada",
  });
  expect(button).toBeDisabled();
  const video = new File(["fixture"], "capture.mp4", { type: "video/mp4" });
  const manifest = new File(
    ['{"contractVersion":"PRODUCT_FUNCTIONAL_RECORDING_V1"}'],
    "manifest.json",
    { type: "application/json" },
  );
  Object.defineProperty(manifest, "text", {
    value: async () => '{"contractVersion":"PRODUCT_FUNCTIONAL_RECORDING_V1"}',
  });
  fireEvent.change(screen.getByLabelText(/MP4 da gravação/), {
    target: { files: [video] },
  });
  fireEvent.change(screen.getByLabelText(/Manifesto da captura/), {
    target: { files: [manifest] },
  });
  expect(screen.getByLabelText("Prévia da gravação funcional")).toHaveAttribute(
    "src",
    "blob:private-capture",
  );
  fireEvent.click(button);
  await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
  const [url, body] = vi.mocked(axios.post).mock.calls[0];
  expect(url).toBe("/api/sales-videos/projects/91014/product-recording");
  expect((body as FormData).get("file")).toBe(video);
  expect((body as FormData).get("manifest")).toContain(
    "PRODUCT_FUNCTIONAL_RECORDING_V1",
  );
  await screen.findByText(/Nenhuma produção paga foi iniciada/);
});
