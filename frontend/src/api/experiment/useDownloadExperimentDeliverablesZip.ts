import { useMutation } from "@tanstack/react-query";
import { toast } from "react-toastify";
import { buildApiUrl } from "../../utils/buildApiUrl";

/** Baixa o ZIP de entregáveis do experimento sem navegar para fora da tela atual. */
export function useDownloadExperimentDeliverablesZip(experimentId: string) {
  return useMutation({
    mutationFn: async () => {
      const response = await fetch(
        buildApiUrl(`/api/experiments/${experimentId}/deliverables.zip`),
      );

      if (!response.ok) {
        throw new Error(
          `Não foi possível baixar o ZIP de entregáveis (status ${response.status}).`,
        );
      }

      const blob = await response.blob();
      const filename =
        resolveFilename(response.headers.get("content-disposition")) ??
        `experimento-${experimentId}-entregaveis.zip`;
      downloadBlob(blob, filename);
    },
    onError: (error: unknown) => {
      const message = error instanceof Error
        ? error.message
        : "Não foi possível baixar o ZIP de entregáveis agora.";
      toast.error(message);
    },
  });
}

/** Extrai o nome do arquivo informado pelo backend no header Content-Disposition. */
function resolveFilename(contentDisposition: string | null) {
  if (!contentDisposition) {
    return null;
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1].replace(/"/g, "").trim());
  }

  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return filenameMatch?.[1]?.trim() || null;
}

/** Dispara o download de um blob gerado no navegador. */
function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
