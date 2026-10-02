import { useRef, useState } from "react";
import axios from "axios";

/** Usa o upload oficial para vincular imagens compostas sem consumir um modelo de IA. */
export default function VisualAssetImageUpload({
  onUploaded,
}: {
  onUploaded: (url: string) => void;
}) {
  const [file, setFile] = useState<File>();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const uploading = useRef(false);
  const fileInput = useRef<HTMLInputElement>(null);
  async function upload() {
    if (!file || uploading.current) return;
    setError("");
    if (
      !["image/png", "image/jpeg", "image/webp"].includes(file.type) ||
      file.size > 10 * 1024 * 1024 ||
      !file.size
    ) {
      setError("Selecione PNG, JPEG ou WebP de até 10 MB.");
      return;
    }
    uploading.current = true;
    setPending(true);
    try {
      const form = new FormData();
      form.append("file", file);
      form.append("category", "GENERIC");
      form.append("model", "DETERMINISTIC_COMPOSITE_V1");
      const { data } = await axios.post<{ url: string }>("/api/assets", form);
      if (!data.url)
        throw new Error("O backend não retornou a URL do arquivo.");
      onUploaded(data.url);
      setFile(undefined);
      if (fileInput.current) fileInput.current.value = "";
    } catch (cause) {
      setError(
        axios.isAxiosError(cause)
          ? (cause.response?.data?.message ??
              "Não foi possível enviar a imagem.")
          : cause instanceof Error
            ? cause.message
            : "Não foi possível enviar a imagem.",
      );
    } finally {
      uploading.current = false;
      setPending(false);
    }
  }
  return (
    <div className="border rounded p-3 my-3">
      <label htmlFor="visual-asset-file" className="form-label">
        Importar imagem composta
      </label>
      <input
        ref={fileInput}
        id="visual-asset-file"
        type="file"
        accept="image/png,image/jpeg,image/webp"
        className="form-control"
        disabled={pending}
        onChange={(event) => {
          setFile(event.target.files?.[0]);
          setError("");
        }}
      />
      <p className="small text-body-secondary mt-2">
        PNG, JPEG ou WebP, até 10 MB. Informe a origem e os direitos abaixo e
        anexe como rascunho para revisão.
      </p>
      <button
        type="button"
        className="btn btn-outline-primary"
        disabled={!file || pending}
        onClick={() => void upload()}
      >
        {pending && <span className="spinner-border spinner-border-sm me-2" />}
        {pending ? "Enviando..." : "Enviar imagem"}
      </button>
      {error && (
        <p role="alert" className="text-danger mt-2">
          {error}
        </p>
      )}
    </div>
  );
}
