import { useMemo, useState } from "react";
import { toast } from "react-toastify";
import {
  useExperimentLearnings,
  type LearningInsight,
  type LearningInsightType,
} from "../../api/experiment/useExperimentLearnings";
import {
  type ExperimentLearningRequest,
  useExperimentLearningRequests,
} from "../../api/experiment/useExperimentLearningRequests";
import { useCreateExperimentLearningRequest } from "../../api/experiment/useCreateExperimentLearningRequest";

interface ExperimentLearningPanelProps {
  experimentId: string;
}

const statusLabels: Record<ExperimentLearningRequest["status"], string> = {
  PENDING: "Na fila",
  PROCESSING: "Processando",
  READY: "Concluído",
  FAILED: "Falhou",
};

const badgeVariants: Record<ExperimentLearningRequest["status"], string> = {
  PENDING: "secondary",
  PROCESSING: "warning",
  READY: "success",
  FAILED: "danger",
};

const insightLabels: Record<LearningInsightType, string> = {
  PAIN: "Dores prioritárias",
  RESULT: "Resultados que importam",
  MECHANISM: "Mecanismos aceitos",
  PROOF: "Provas que funcionaram",
  OFFER: "Ofertas / âncoras",
};

function groupInsights(insights: LearningInsight[] = []) {
  const groups: Partial<Record<LearningInsightType, LearningInsight[]>> = {};
  insights.forEach((insight) => {
    if (!insight || !insight.type) return;
    groups[insight.type] = [...(groups[insight.type] ?? []), insight];
  });
  return groups;
}

function formatDateTime(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ExperimentLearningPanel({
  experimentId,
}: ExperimentLearningPanelProps) {
  const {
    data: requests,
    isLoading: isLoadingRequests,
    error: requestsError,
  } = useExperimentLearningRequests(experimentId);
  const {
    data: learnings,
    isLoading: isLoadingLearnings,
    error: learningsError,
    refetch,
  } = useExperimentLearnings(experimentId);
  const createRequest = useCreateExperimentLearningRequest(experimentId);
  const [requestedBy, setRequestedBy] = useState("");

  const latestLearning = learnings?.[0];
  const insightsByType = useMemo(
    () => groupInsights(latestLearning?.insights ?? []),
    [latestLearning?.insights],
  );
  const hasActiveRequest = useMemo(
    () =>
      (requests ?? []).some((req) =>
        ["PENDING", "PROCESSING"].includes(req.status),
      ),
    [requests],
  );

  const handleCreate = () => {
    if (hasActiveRequest || createRequest.isPending) {
      return;
    }
    createRequest.mutate(requestedBy, {
      onSuccess: () => {
        toast.success("Solicitação registrada. O worker vai gerar o aprendizado automáticamente.");
        setRequestedBy("");
        refetch();
      },
      onError: () => {
        toast.error("Não foi possível registrar a solicitação agora.");
      },
    });
  };

  return (
    <div className="card">
      <div className="card-body d-flex flex-column gap-4">
        <div className="d-flex flex-column flex-lg-row justify-content-between gap-3">
          <div>
            <h5 className="card-title mb-1">Aprendizado automatizado do experimento</h5>
            <p className="text-muted mb-0">
              Gera um resumo acionável (dor → resultado → mecanismo → prova → oferta)
              com base nas métricas e ativos vinculados ao experimento.
            </p>
          </div>
          <div className="d-flex flex-column flex-sm-row gap-2">
            <input
              type="text"
              className="form-control"
              placeholder="Quem está solicitando? (opcional)"
              value={requestedBy}
              onChange={(event) => setRequestedBy(event.target.value)}
              disabled={createRequest.isPending}
            />
            <button
              type="button"
              className="btn btn-primary"
              disabled={hasActiveRequest || createRequest.isPending}
              onClick={handleCreate}
            >
              {hasActiveRequest
                ? "Processamento em andamento"
                : createRequest.isPending
                  ? "Registrando..."
                  : "Solicitar leitura"}
            </button>
          </div>
        </div>
        {requestsError ? (
          <div className="alert alert-danger" role="alert">
            Não foi possível carregar o status das solicitações.
          </div>
        ) : null}
        <div>
          <div className="d-flex justify-content-between align-items-center mb-2">
            <h6 className="text-uppercase text-muted fw-semibold mb-0">
              Solicitações recentes
            </h6>
          </div>
          {isLoadingRequests ? (
            <div className="text-muted small">Carregando solicitações...</div>
          ) : (requests?.length ?? 0) === 0 ? (
            <div className="text-muted small">
              Nenhuma solicitação registrada ainda.
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Solicitado em</th>
                    <th>Concluído em</th>
                    <th>Solicitado por</th>
                  </tr>
                </thead>
                <tbody>
                  {requests?.map((request) => (
                    <tr key={request.id}>
                      <td>
                        <span
                          className={`badge text-bg-${badgeVariants[request.status]} me-2`}
                        >
                          {statusLabels[request.status]}
                        </span>
                        {request.status === "FAILED" && request.failureReason ? (
                          <span className="d-block text-danger small mt-1">
                            {request.failureReason}
                          </span>
                        ) : null}
                      </td>
                      <td>{formatDateTime(request.requestedAt)}</td>
                      <td>{formatDateTime(request.completedAt)}</td>
                      <td>{request.requestedBy ?? "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        <div>
          <div className="d-flex justify-content-between align-items-center mb-2">
            <h6 className="text-uppercase text-muted fw-semibold mb-0">
              Último aprendizado consolidado
            </h6>
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={() => refetch()}
              disabled={isLoadingLearnings}
            >
              Atualizar
            </button>
          </div>
          {learningsError ? (
            <div className="alert alert-danger" role="alert">
              Não foi possível carregar os aprendizados do experimento.
            </div>
          ) : isLoadingLearnings ? (
            <div className="text-muted small">Carregando resumo...</div>
          ) : !latestLearning ? (
            <div className="text-muted small">
              Solicite uma leitura para destravar o banco de aprendizados deste experimento.
            </div>
          ) : (
            <div className="row g-3">
              <div className="col-12 col-lg-6">
                <div className="border rounded-3 p-3 h-100">
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <strong>Resumo executivo</strong>
                    <span className="badge text-bg-light">
                      {latestLearning.stage ?? "—"}
                    </span>
                  </div>
                  <p className="mb-2 text-body-secondary small">
                    Métrica primária: {latestLearning.primaryMetric ?? "—"}
                  </p>
                  <p className="mb-2 text-body-secondary small">
                    Sinal da métrica: {latestLearning.metricSignal ?? "—"}
                  </p>
                  <p className="mb-0">{latestLearning.summary ?? "Sem resumo."}</p>
                </div>
              </div>
              <div className="col-12 col-lg-6">
                <div className="border rounded-3 p-3 h-100">
                  <strong>O que funcionou</strong>
                  <p className="mb-0 text-body-secondary">
                    {latestLearning.whatWorked ?? "Ainda não documentado."}
                  </p>
                </div>
              </div>
              <div className="col-12 col-lg-6">
                <div className="border rounded-3 p-3 h-100">
                  <strong>Bloqueios</strong>
                  <p className="mb-0 text-body-secondary">
                    {latestLearning.whatBlocked ?? "Nenhum bloqueio registrado."}
                  </p>
                </div>
              </div>
              <div className="col-12 col-lg-6">
                <div className="border rounded-3 p-3 h-100">
                  <strong>Próximo teste recomendado</strong>
                  <p className="mb-0 text-body-secondary">
                    {latestLearning.nextTest ?? "Solicite uma nova leitura para descobrir o próximo passo."}
                  </p>
                </div>
              </div>
              <div className="col-12">
                <div className="border rounded-3 p-3">
                  <strong>Dicionário do experimento</strong>
                  <div className="row g-3 mt-1">
                    {(Object.entries(insightLabels) as [LearningInsightType, string][]).map(
                      ([type, label]) => (
                        <div className="col-12 col-md-6" key={type}>
                          <div className="bg-light rounded-3 p-3 h-100">
                            <span className="text-uppercase text-muted small fw-semibold">
                              {label}
                            </span>
                            <ul className="list-unstyled mb-0 mt-2">
                              {(insightsByType[type] ?? []).length === 0 ? (
                                <li className="text-body-secondary small">
                                  Nenhum insight registrado.
                                </li>
                              ) : (
                                insightsByType[type]!.map((insight, index) => (
                                  <li key={`${type}-${index}`} className="mb-2">
                                    <div className="fw-semibold small">
                                      {insight.statement}
                                    </div>
                                    {insight.evidence ? (
                                      <div className="text-body-secondary small">
                                        {insight.evidence}
                                      </div>
                                    ) : null}
                                  </li>
                                ))
                              )}
                            </ul>
                          </div>
                        </div>
                      ),
                    )}
                  </div>
                </div>
              </div>
              <div className="col-12">
                <div className="border rounded-3 p-3">
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <strong>Recomendações para o backlog</strong>
                    <span className="text-muted small">
                      Última atualização: {formatDateTime(latestLearning.completedAt)}
                    </span>
                  </div>
                  {(latestLearning.suggestions ?? []).length === 0 ? (
                    <p className="text-body-secondary mb-0">
                      Nenhuma sugestão registrada nesta leitura.
                    </p>
                  ) : (
                    <div className="d-flex flex-column gap-2">
                      {latestLearning.suggestions.map((suggestion, index) => (
                        <div
                          key={`${suggestion.title}-${index}`}
                          className="border rounded-3 p-3"
                        >
                          <div className="d-flex flex-wrap gap-2 align-items-center mb-1">
                            <span className="fw-semibold">{suggestion.title}</span>
                            {suggestion.stage ? (
                              <span className="badge text-bg-light">
                                {suggestion.stage}
                              </span>
                            ) : null}
                            {suggestion.priority ? (
                              <span className="badge text-bg-secondary">
                                {suggestion.priority}
                              </span>
                            ) : null}
                          </div>
                          {suggestion.rationale ? (
                            <p className="mb-0 text-body-secondary">
                              {suggestion.rationale}
                            </p>
                          ) : null}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
              <div className="col-12">
                <div className="border rounded-3 p-3">
                  <strong>Payload bruto enviado para OpenAI</strong>
                  {latestLearning.openAiRequestPayload &&
                  Object.keys(latestLearning.openAiRequestPayload).length > 0 ? (
                    <details className="mt-2">
                      <summary className="small text-muted">
                        Exibir JSON (system, temperature, model e demais campos)
                      </summary>
                      <pre className="bg-light border rounded-3 p-3 mt-2 mb-0 small overflow-auto">
                        {JSON.stringify(latestLearning.openAiRequestPayload, null, 2)}
                      </pre>
                    </details>
                  ) : (
                    <p className="text-body-secondary mt-2 mb-0 small">
                      Este aprendizado não possui payload bruto salvo.
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
