import { useMemo } from "react";
import { toast } from "react-toastify";
import type {
  TargetingCandidate,
  TargetingCandidateStatus,
  TargetingOption,
  TargetingRequest,
} from "../api/targeting/types";
import { useTargetingRequests } from "../api/targeting/useTargetingRequests";
import type { TargetingRequestQueryFilters } from "../api/targeting/useTargetingRequests";
import { useReprocessTargetingCandidate } from "../api/targeting/useReprocessTargetingCandidate";
import "./TargetingRequestStatusPanel.css";

interface TargetingRequestStatusPanelProps {
  limit?: number;
  className?: string;
  nicheId?: number;
  hypothesisId?: string;
}

const STATUS_COLORS: Record<TargetingCandidateStatus, string> = {
  PENDING_FACEBOOK_MATCH: "text-bg-secondary",
  VALIDATED: "text-bg-success",
  NO_MATCH: "text-bg-warning",
};

const SOURCE_LABEL: Record<string, string> = {
  SEARCH: "Busca",
  SUGGESTION: "Sugestão",
  BROWSE: "Browse",
};

type FunnelStageKey = "top" | "mid" | "bottom";

const FUNNEL_STAGE_MAP: Record<string, FunnelStageKey> = {
  AWARENESS: "top",
  CONSIDERATION: "mid",
  DECISION: "bottom",
};

export function TargetingRequestStatusPanel({ limit = 10, className, nicheId, hypothesisId }: TargetingRequestStatusPanelProps) {
  const filters: TargetingRequestQueryFilters = { limit, nicheId, hypothesisId };
  const { data, isFetching, refetch } = useTargetingRequests(filters);
  const reprocessMutation = useReprocessTargetingCandidate(filters);
  const requests = Array.isArray(data) ? data : [];

  const audienceFormatter = useMemo(() => new Intl.NumberFormat("pt-BR"), []);
  const pendingCandidateId =
    (reprocessMutation.variables as { candidateId: number } | undefined)?.candidateId ?? null;

  const handleReprocess = async (candidate: TargetingCandidate) => {
    try {
      await reprocessMutation.mutateAsync({ candidateId: candidate.id });
      toast.success("Candidato reenviado para resolução na Meta.");
    } catch (error) {
      console.error("Erro ao reenfileirar candidato", error);
      toast.error("Não foi possível reprocessar agora. Tente novamente em instantes.");
    }
  };

  return (
    <div className={`card ${className ?? ""}`}>
      <div className="card-header d-flex justify-content-between align-items-center flex-wrap gap-2">
        <div>
          <h5 className="mb-1">Solicitações recentes de targeting</h5>
          <p className="text-body-secondary small mb-0">
            Apenas opções validadas pela Graph API são exibidas ao cliente. Acompanhe rejeições e reprocessamentos.
          </p>
        </div>
        <button className="btn btn-outline-secondary btn-sm" type="button" onClick={() => refetch()} disabled={isFetching}>
          {isFetching ? "Atualizando..." : "Atualizar"}
        </button>
      </div>
      <div className="card-body">
        {requests.length === 0 && !isFetching && (
          <p className="text-body-secondary mb-0">
            Nenhuma solicitação registrada ainda. Envie uma hipótese para que o AI Worker gere candidatos.
          </p>
        )}
        {isFetching && requests.length === 0 && (
          <p className="text-body-secondary mb-0">Carregando solicitações...</p>
        )}
        {requests.map((request) => (
          <RequestCard
            key={request.id}
            request={request}
            onReprocess={handleReprocess}
            audienceFormatter={audienceFormatter}
            pendingCandidateId={pendingCandidateId}
            isMutationPending={reprocessMutation.isPending}
          />
        ))}
      </div>
    </div>
  );
}

interface RequestCardProps {
  request: TargetingRequest;
  onReprocess: (candidate: TargetingCandidate) => void;
  audienceFormatter: Intl.NumberFormat;
  pendingCandidateId: number | null;
  isMutationPending: boolean;
}

function RequestCard({ request, onReprocess, audienceFormatter, pendingCandidateId, isMutationPending }: RequestCardProps) {
  const candidates = Array.isArray(request.candidates) ? request.candidates : [];
  return (
    <div className="border rounded-3 p-3 mb-3">
      <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
        <div>
          <strong className="d-block">{request.descricao || "-"}</strong>
          <span className="text-body-secondary small">
            {`Idioma ${request.idioma ?? "-"} · País ${request.pais ?? "-"}`}
          </span>
        </div>
        <span className="badge text-bg-light">{request.status}</span>
      </div>
      <div className="mt-3">
        {candidates.length === 0 && (
          <p className="text-body-secondary small mb-0">Nenhum candidato recebido desta solicitação.</p>
        )}
        {candidates.map((candidate) => (
          <CandidateCard
            key={candidate.id}
            candidate={candidate}
            onReprocess={onReprocess}
            audienceFormatter={audienceFormatter}
            isProcessing={isMutationPending && pendingCandidateId === candidate.id}
          />
        ))}
      </div>
    </div>
  );
}

interface CandidateCardProps {
  candidate: TargetingCandidate;
  onReprocess: (candidate: TargetingCandidate) => void;
  audienceFormatter: Intl.NumberFormat;
  isProcessing: boolean;
}

function CandidateCard({ candidate, onReprocess, audienceFormatter, isProcessing }: CandidateCardProps) {
  const options = Array.isArray(candidate.options) ? candidate.options : [];
  const variants = Array.isArray(candidate.seed_variants) ? candidate.seed_variants : [];
  const statusClass = STATUS_COLORS[candidate.status] ?? "text-bg-secondary";
  const primarySeed = candidate.seed || candidate.texto_sugerido || "-";
  const decisionRationale = buildDecisionRationale(candidate);

  return (
    <div className="bg-light rounded-3 p-3 mb-2">
      <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
        <div className="flex-grow-1">
          <div className="fw-semibold">{primarySeed}</div>
          <div className="text-body-secondary small">{candidate.tipo ?? "-"}</div>
          <div className="d-flex flex-wrap gap-2 mt-2">
            {candidate.idioma_hint && <span className="badge text-bg-light">{candidate.idioma_hint}</span>}
            {candidate.intent_tag && <FunnelStageBadge tag={candidate.intent_tag} />}
            {candidate.origem && <span className="badge text-bg-secondary">{candidate.origem}</span>}
          </div>
          {decisionRationale && <DecisionRationaleCard rationale={decisionRationale} />}
          {variants.length > 1 && (
            <div className="mt-2 d-flex flex-wrap gap-2">
              {variants.map((variant) => (
                <span key={`${candidate.id}-${variant}`} className="badge rounded-pill text-bg-light">
                  {variant}
                </span>
              ))}
            </div>
          )}
        </div>
        <span className={`badge ${statusClass}`}>{candidate.status}</span>
      </div>

      {candidate.status === "VALIDATED" && options.length > 0 && (
        <div className="mt-3">
          <p className="text-body-secondary small fw-semibold mb-2">
            Opções validadas pelo Facebook
          </p>
          <ul className="list-unstyled mb-0">
            {options.map((option) => (
              <OptionRow key={`${candidate.id}-${option.facebook_id}`} option={option} formatter={audienceFormatter} />
            ))}
          </ul>
        </div>
      )}

      {candidate.status === "PENDING_FACEBOOK_MATCH" && (
        <p className="text-body-secondary small mt-3 mb-0">
          Aguardando resolução pelo Facebook Ads Worker. Assim que houver opções válidas, elas aparecerão aqui.
        </p>
      )}

      {candidate.status === "NO_MATCH" && (
        <div className="alert alert-warning mt-3 mb-0" role="alert">
          <div className="small">
            {candidate.rejection_reason ?? "Nenhuma opção retornada pela Meta para este seed."}
          </div>
          <button
            type="button"
            className="btn btn-outline-dark btn-sm mt-2"
            onClick={() => onReprocess(candidate)}
            disabled={isProcessing}
          >
            {isProcessing ? "Reprocessando..." : "Reprocessar candidato"}
          </button>
        </div>
      )}
    </div>
  );
}

function buildDecisionRationale(candidate: TargetingCandidate) {
  if (candidate.status === "NO_MATCH") {
    return candidate.rejection_reason
      ? `Bloqueado: ${candidate.rejection_reason}`
      : "Bloqueado: a Meta não retornou opção válida para este seed.";
  }

  if (candidate.rationale) {
    return candidate.rationale;
  }

  if (candidate.status === "VALIDATED") {
    return "Aprovado: existem opções validadas pela Graph API para ativação operacional.";
  }

  if (candidate.status === "PENDING_FACEBOOK_MATCH") {
    return "Aguardando: o Facebook Ads Worker ainda precisa validar se há público acionável.";
  }

  return null;
}

function DecisionRationaleCard({ rationale }: { rationale: string }) {
  return (
    <div className="targeting-decision-rationale mt-2" aria-label="Motivo da decisão operacional">
      <span className="targeting-decision-rationale__label">Motivo da decisão</span>
      <span>{rationale}</span>
    </div>
  );
}

function FunnelStageBadge({ tag }: { tag: string }) {
  const normalized = tag.trim().toUpperCase();
  const activeStage = FUNNEL_STAGE_MAP[normalized];

  if (!activeStage) {
    return <span className="badge text-bg-info text-uppercase">{tag}</span>;
  }

  return (
    <div className="funnel-stage" aria-label={`Estágio do funil: ${tag}`}>
      <div className={`funnel-stage__segment funnel-stage__segment--top${activeStage === "top" ? " is-active" : ""}`}>
        Top
      </div>
      <div className={`funnel-stage__segment funnel-stage__segment--mid${activeStage === "mid" ? " is-active" : ""}`}>
        Mid
      </div>
      <div className={`funnel-stage__segment funnel-stage__segment--bottom${activeStage === "bottom" ? " is-active" : ""}`}>
        Bottom
      </div>
    </div>
  );
}

interface OptionRowProps {
  option: TargetingOption;
  formatter: Intl.NumberFormat;
}

function OptionRow({ option, formatter }: OptionRowProps) {
  const audience = typeof option.audience_size === "number" ? formatter.format(option.audience_size) : "-";
  const path = Array.isArray(option.path) && option.path.length > 0 ? option.path.join(" › ") : null;
  const sourceLabel = option.source ? SOURCE_LABEL[option.source] ?? option.source : null;
  const finalScore = typeof option.final_score === "number" ? `${Math.round(option.final_score * 100)}%` : null;

  return (
    <li className="py-2 border-top">
      <div className="d-flex justify-content-between gap-3 flex-wrap">
        <div>
          <div className="fw-semibold">{option.name}</div>
          <div className="text-body-secondary small">ID {option.facebook_id}</div>
          {path && <div className="text-body-secondary small">{path}</div>}
          <div className="d-flex flex-wrap gap-2 mt-1">
            <span className="badge text-bg-primary">Facebook</span>
            {sourceLabel && <span className="badge text-bg-light">{sourceLabel}</span>}
            {option.seed_variant && <span className="badge text-bg-secondary">Seed: {option.seed_variant}</span>}
          </div>
        </div>
        <div className="text-body-secondary small text-end">
          <div>{audience} pessoas</div>
          {typeof option.match_score === "number" && (
            <div>Match {(option.match_score * 100).toFixed(0)}%</div>
          )}
          {finalScore && <div>Score {finalScore}</div>}
        </div>
      </div>
    </li>
  );
}
