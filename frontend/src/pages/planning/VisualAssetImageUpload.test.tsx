import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import VisualAssetImageUpload from "./VisualAssetImageUpload";

vi.mock("axios", () => ({
  default: { post: vi.fn(), isAxiosError: vi.fn(() => false) },
}));
afterEach(cleanup);
beforeEach(() => {
  vi.clearAllMocks();
});
const select = (file: File) =>
  fireEvent.change(screen.getByLabelText("Importar imagem composta"), {
    target: { files: [file] },
  });

describe("importação de composição sem fila de IA", () => {
  it("usa o upload existente, entrega a URL e limpa o arquivo", async () => {
    const onUploaded = vi.fn();
    vi.mocked(axios.post).mockResolvedValue({
      data: { url: "https://assets.example/post.png" },
    });
    render(<VisualAssetImageUpload onUploaded={onUploaded} />);
    const file = new File(["image"], "demo.png", { type: "image/png" });
    select(file);
    fireEvent.click(screen.getByRole("button", { name: "Enviar imagem" }));
    await waitFor(() =>
      expect(onUploaded).toHaveBeenCalledWith(
        "https://assets.example/post.png",
      ),
    );
    const [url, form] = vi.mocked(axios.post).mock.calls[0];
    expect(url).toBe("/api/assets");
    expect((form as FormData).get("file")).toBe(file);
    expect((form as FormData).get("model")).toBe("DETERMINISTIC_COMPOSITE_V1");
    expect(
      screen.getByRole("button", { name: "Enviar imagem" }),
    ).toBeDisabled();
  });
  it.each([
    new File(["mp4"], "bad.mp4", { type: "video/mp4" }),
    new File([], "empty.png", { type: "image/png" }),
    new File([new Uint8Array(10 * 1024 * 1024 + 1)], "huge.png", {
      type: "image/png",
    }),
  ])("rejeita arquivo inválido antes de enviar ($name)", async (file) => {
    render(<VisualAssetImageUpload onUploaded={vi.fn()} />);
    select(file);
    fireEvent.click(screen.getByRole("button", { name: "Enviar imagem" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("até 10 MB");
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("não repete o upload pendente e permite retomar após falha", async () => {
    let reject!: (reason: Error) => void;
    vi.mocked(axios.post).mockImplementationOnce(
      () =>
        new Promise((_resolve, r) => {
          reject = r;
        }),
    );
    vi.mocked(axios.post).mockResolvedValueOnce({
      data: { url: "https://assets.example/recovered.png" },
    });
    const onUploaded = vi.fn();
    render(<VisualAssetImageUpload onUploaded={onUploaded} />);
    select(new File(["png"], "demo.png", { type: "image/png" }));
    const send = screen.getByRole("button", { name: "Enviar imagem" });
    fireEvent.click(send);
    fireEvent.click(send);
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Enviando..." })).toBeDisabled();
    reject(new Error("Falha transitória de upload"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Falha transitória",
    );
    fireEvent.click(screen.getByRole("button", { name: "Enviar imagem" }));
    await waitFor(() =>
      expect(onUploaded).toHaveBeenCalledWith(
        "https://assets.example/recovered.png",
      ),
    );
  });
  it("não apresenta sucesso quando o retorno não contém a URL", async () => {
    const onUploaded = vi.fn();
    vi.mocked(axios.post).mockResolvedValue({ data: {} });
    render(<VisualAssetImageUpload onUploaded={onUploaded} />);
    select(new File(["png"], "demo.png", { type: "image/png" }));
    fireEvent.click(screen.getByRole("button", { name: "Enviar imagem" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "não retornou a URL",
    );
    expect(onUploaded).not.toHaveBeenCalled();
  });
});
