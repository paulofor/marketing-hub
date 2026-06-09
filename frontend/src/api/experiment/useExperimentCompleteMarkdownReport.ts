import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";

export interface ExperimentCompleteMarkdownReport {
  filename: string;
  markdown: string;
}

export function useExperimentCompleteMarkdownReport(experimentId?: string) {
  return useMutation({
    mutationFn: async () => {
      if (!experimentId) {
        throw new Error("Experiment id is required");
      }
      const { data } = await axios.get<ExperimentCompleteMarkdownReport>(
        `/api/experiments/${experimentId}/report-material/complete-markdown`,
      );
      return data;
    },
    onSuccess: (data) => {
      downloadMarkdown(data.markdown, data.filename);
      toast.success("Relatório completo em Markdown gerado");
    },
    onError: (error: unknown) => {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ?? error.message)
        : (error as Error)?.message;
      toast.error(
        message ?? "Não foi possível gerar o relatório completo agora",
      );
    },
  });
}

function downloadMarkdown(markdown: string, filename: string) {
  const blob = new Blob([markdown], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename || "relatorio-completo-experimento.md";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
