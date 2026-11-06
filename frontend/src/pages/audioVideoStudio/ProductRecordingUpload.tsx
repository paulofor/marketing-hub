import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

/** Vincula uma captura temporal privada pelo contrato do Estúdio, sem pedir análise paga. */
export default function ProductRecordingUpload({
  projectId,
}: {
  projectId: number;
}) {
  const [file, setFile] = useState<File>();
  const [manifest, setManifest] = useState<File>();
  const [preview, setPreview] = useState("");
  const query = useQueryClient();
  useEffect(() => {
    if (!file) {
      setPreview("");
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);
  const upload = useMutation({
    mutationFn: async () => {
      if (!file || !manifest)
        throw new Error("Selecione o MP4 e o manifesto da captura.");
      if (file.size > 8 * 1024 * 1024)
        throw new Error("O MP4 deve ter até 8 MiB.");
      const body = new FormData();
      body.append("file", file);
      body.append("manifest", await manifest.text());
      return axios.post(
        `/api/sales-videos/projects/${projectId}/product-recording`,
        body,
      );
    },
    onSuccess: () => {
      query.invalidateQueries({ queryKey: ["sales-video-project", projectId] });
      query.invalidateQueries({
        queryKey: ["video-production-cycles", projectId],
      });
    },
  });
  return (
    <section aria-label="Gravação funcional privada">
      <h4>Mostrar o funcionamento real</h4>
      <p>
        Vincule a gravação local com dados de teste e seu manifesto. Salvar este
        material não inicia geração nem revisão paga e não aprova o vídeo final.
      </p>
      <label>
        MP4 da gravação (até 8 MiB)
        <input
          type="file"
          accept="video/mp4,.mp4"
          onChange={(event) => {
            setFile(event.target.files?.[0]);
            upload.reset();
          }}
        />
      </label>
      <label>
        Manifesto da captura (JSON)
        <input
          type="file"
          accept="application/json,.json"
          onChange={(event) => {
            setManifest(event.target.files?.[0]);
            upload.reset();
          }}
        />
      </label>
      {preview ? (
        <video
          controls
          playsInline
          src={preview}
          aria-label="Prévia da gravação funcional"
          style={{ maxWidth: "100%", maxHeight: 420 }}
        />
      ) : null}
      <button
        type="button"
        disabled={!file || !manifest || upload.isPending}
        onClick={() => upload.mutate()}
      >
        {upload.isPending
          ? "Vinculando gravação…"
          : "Vincular gravação privada"}
      </button>
      {upload.isSuccess ? (
        <p role="status">
          Gravação privada vinculada ao projeto. Nenhuma produção paga foi
          iniciada.
        </p>
      ) : null}
      {upload.isError ? (
        <p role="alert">
          {axios.isAxiosError(upload.error)
            ? upload.error.response?.data?.detail || upload.error.message
            : upload.error.message}
        </p>
      ) : null}
    </section>
  );
}
