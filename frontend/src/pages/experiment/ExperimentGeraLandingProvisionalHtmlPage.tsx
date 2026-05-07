import { useMemo } from "react";
import { useParams } from "react-router-dom";
import { useGeraLandingStageExecutionDetail } from "../../api/experiment/useGeraLandingStageExecutions";

export default function ExperimentGeraLandingProvisionalHtmlPage() {
  const { id: experimentId, jobId } = useParams();
  const detailQuery = useGeraLandingStageExecutionDetail(experimentId, jobId);

  const renderedHtml = useMemo(() => {
    if (!detailQuery.data?.provisionalHtml?.trim()) return "";
    return detailQuery.data.provisionalHtml;
  }, [detailQuery.data?.provisionalHtml]);

  if (detailQuery.isLoading) {
    return <p className="p-3 mb-0 text-muted">Carregando HTML provisório...</p>;
  }

  if (detailQuery.isError) {
    return <p className="p-3 mb-0 text-danger">Não foi possível carregar o HTML provisório.</p>;
  }

  if (!renderedHtml) {
    return <p className="p-3 mb-0 text-muted">Nenhum HTML provisório disponível para este registro.</p>;
  }

  return <iframe title="HTML provisório" srcDoc={renderedHtml} style={{ width: "100%", minHeight: "100vh", border: "0" }} />;
}
