import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useJourney } from "../../api/journey/useJourney";
import JourneyStatusBadge from "./JourneyStatusBadge";
import { useDeleteJourney } from "../../api/journey/useDeleteJourney";
import type { Journey, JourneyStatus } from "../../api/journey/types";
import "./JourneyDetailPage.css";

type TimelineStageAccent = "start" | "milestone" | "segment" | "end";

interface TimelineStage {
  id: string;
  title: string;
  description: string;
  accent: TimelineStageAccent;
  detail?: string;
  detailType?: "date" | "text";
}

interface ConstellationNode {
  id: string;
  label: string;
  value: string;
  hint?: string;
  accent: "primary" | "secondary" | "tertiary" | "quaternary";
}

const stageAccentIcons: Record<TimelineStageAccent, string> = {
  start: "🚀",
  milestone: "🧭",
  segment: "🎯",
  end: "🏁",
};

const statusLabels: Record<JourneyStatus, string> = {
  DRAFT: "Rascunho",
  ACTIVE: "Ativa",
  PAUSED: "Pausada",
  COMPLETED: "Concluída",
  ARCHIVED: "Arquivada",
};

function beautifyMetadataKey(key: string, index: number) {
  const cleaned = key
    .replace(/_/g, " ")
    .replace(/-/g, " ")
    .replace(/\s+/g, " ")
    .replace(/[0-9]+$/, "")
    .trim();

  if (!cleaned) {
    return `Etapa ${index}`;
  }

  return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
}

function buildTimelineStages(
  journey: Journey,
  metadataEntries: Array<[string, string]>,
): TimelineStage[] {
  const stages: TimelineStage[] = [];

  stages.push({
    id: "start",
    accent: "start",
    title: "Início da jornada",
    description: journey.startAt
      ? "Momento de largada planejado para ativar o plano de ação."
      : "Defina uma data de início para visualizar a largada desta jornada.",
    detail: journey.startAt || journey.createdAt,
    detailType: "date",
  });

  const stageKeywords = ["fase", "phase", "etapa", "stage", "momento", "step"];
  const milestoneEntries = metadataEntries.filter(([key]) =>
    stageKeywords.some((keyword) => key.toLowerCase().includes(keyword)),
  );

  if (milestoneEntries.length) {
    milestoneEntries.forEach(([key, value], index) => {
      stages.push({
        id: `milestone-${key}-${index}`,
        accent: "milestone",
        title: beautifyMetadataKey(key, index + 1),
        description: value?.trim() || "Adicione detalhes para esta etapa e deixe o mapa ainda mais completo.",
      });
    });
  } else {
    stages.push({
      id: "blueprint",
      accent: "milestone",
      title: `Blueprint "${journey.templateName}"`,
      description:
        "Este template orienta os pontos de contato e garante consistência na jornada. Personalize os marcos usando os metadados.",
    });
  }

  if (journey.segmentReference || journey.segmentFilter || journey.marketNicheId) {
    stages.push({
      id: "segment",
      accent: "segment",
      title: "Segmentação em foco",
      description:
        journey.segmentFilter?.trim() ||
        journey.segmentReference?.trim() ||
        (journey.marketNicheId ? `Foco no nicho ${journey.marketNicheId}.` : "Direcione a jornada para um público específico."),
      detail: journey.segmentReference || journey.segmentFilter || undefined,
      detailType: journey.segmentReference || journey.segmentFilter ? "text" : undefined,
    });
  }

  stages.push({
    id: "end",
    accent: "end",
    title: "Grand finale",
    description:
      journey.endAt
        ? "Momento previsto para consolidar resultados e celebrar os aprendizados."
        : "Defina uma data de conclusão para acompanhar o encerramento com clareza.",
    detail: journey.endAt ?? undefined,
    detailType: journey.endAt ? "date" : undefined,
  });

  return stages;
}

function buildConstellationNodes(journey: Journey): ConstellationNode[] {
  const nodes: ConstellationNode[] = [
    {
      id: "status",
      accent: "primary",
      label: "Status",
      value: statusLabels[journey.status],
      hint: "Situação atual da jornada.",
    },
    {
      id: "template",
      accent: "secondary",
      label: "Template",
      value: journey.templateName,
      hint: "Arquitetura criativa que guia os marcos.",
    },
  ];

  if (journey.experimentId != null) {
    nodes.push({
      id: "experiment",
      accent: "tertiary",
      label: "Experimento",
      value: `Experimento #${journey.experimentId}`,
      hint: "Hipótese testada nesta jornada.",
    });
  }

  if (journey.marketNicheId != null) {
    nodes.push({
      id: "niche",
      accent: "quaternary",
      label: "Nicho",
      value: `Nicho ${journey.marketNicheId}`,
      hint: "Contexto de mercado priorizado.",
    });
  }

  if (journey.segmentReference || journey.segmentFilter) {
    nodes.push({
      id: "segment",
      accent: "primary",
      label: "Segmento",
      value: journey.segmentReference || journey.segmentFilter || "Segmento personalizado",
      hint: "Critério usado para encontrar o público ideal.",
    });
  }

  return nodes;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "long",
      timeStyle: "short",
    }).format(new Date(value));
  } catch (error) {
    return value;
  }
}

export default function JourneyDetailPage() {
  const params = useParams<{ id: string }>();
  const navigate = useNavigate();
  const journeyId = Number(params.id);
  const { data: journey, isLoading } = useJourney(Number.isNaN(journeyId) ? undefined : journeyId);
  const deleteJourney = useDeleteJourney(journeyId);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);

  const metadataEntries = useMemo(
    () => Object.entries(journey?.metadata ?? {}),
    [journey?.metadata],
  );

  const timelineStages = useMemo(
    () => (journey ? buildTimelineStages(journey, metadataEntries) : []),
    [journey, metadataEntries],
  );

  const constellationNodes = useMemo(
    () => (journey ? buildConstellationNodes(journey) : []),
    [journey],
  );

  if (isLoading) {
    return <div className="journey-detail__loading">Carregando jornada...</div>;
  }

  if (!journey) {
    return (
      <div className="journey-detail__loading">
        Jornada não encontrada.
        <div>
          <Link to="/journeys" className="btn btn-link">
            Voltar para jornadas
          </Link>
        </div>
      </div>
    );
  }

  const handleDelete = async () => {
    await deleteJourney.mutateAsync();
    navigate("/journeys");
  };

  return (
    <div className="journey-detail">
      <header className="journey-detail__header">
        <div>
          <PageTitle>{journey.name}</PageTitle>
          {journey.description ? (
            <p className="journey-detail__subtitle">{journey.description}</p>
          ) : null}
          <div className="journey-detail__status">
            <JourneyStatusBadge status={journey.status} />
            <span className="journey-detail__status-meta">
              Atualizada em {formatDateTime(journey.updatedAt)}
            </span>
          </div>
        </div>
        <div className="journey-detail__actions">
          <Link className="btn btn-secondary" to={`/journeys/${journey.id}/edit`}>
            Editar jornada
          </Link>
          <button
            type="button"
            className="btn btn-outline-danger"
            onClick={() => setIsConfirmOpen(true)}
            disabled={deleteJourney.isPending}
          >
            {deleteJourney.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
                Removendo...
              </>
            ) : (
              "Excluir"
            )}
          </button>
        </div>
      </header>

      <section className="journey-detail__grid">
        <article className="journey-detail__card">
          <h2>Resumo</h2>
          <dl>
            <div>
              <dt>Template</dt>
              <dd>{journey.templateName}</dd>
            </div>
            <div>
              <dt>Janela</dt>
              <dd>
                {formatDateTime(journey.startAt)}
                <span className="journey-detail__arrow">→</span>
                {formatDateTime(journey.endAt)}
              </dd>
            </div>
            <div>
              <dt>Criado em</dt>
              <dd>{formatDateTime(journey.createdAt)}</dd>
            </div>
          </dl>
        </article>

        <article className="journey-detail__card">
          <h2>Segmentação</h2>
          <dl>
            <div>
              <dt>Referência externa</dt>
              <dd>{journey.segmentReference ?? "—"}</dd>
            </div>
            <div>
              <dt>Filtro</dt>
              <dd>{journey.segmentFilter ?? "—"}</dd>
            </div>
            <div>
              <dt>Nicho de mercado</dt>
              <dd>{journey.marketNicheId ?? "—"}</dd>
            </div>
            <div>
              <dt>Experimento</dt>
              <dd>{journey.experimentId ?? "—"}</dd>
            </div>
          </dl>
        </article>

        <article className="journey-detail__card journey-detail__card--full">
          <h2>Mapa lúdico da jornada</h2>
          <p className="journey-detail__card-intro">
            Uma visão em estilo storyboard destacando os principais marcos, desde o pontapé inicial até o grande final.
          </p>
          <ol className="journey-detail__timeline">
            {timelineStages.map((stage) => (
              <li key={stage.id} className={`journey-detail__timeline-stage journey-detail__timeline-stage--${stage.accent}`}>
                <span className="journey-detail__timeline-icon" aria-hidden="true">
                  {stageAccentIcons[stage.accent]}
                </span>
                <div className="journey-detail__timeline-content">
                  <h3>{stage.title}</h3>
                  <p>{stage.description}</p>
                  {stage.detail ? (
                    <span className="journey-detail__timeline-detail">
                      {stage.detailType === "date" ? formatDateTime(stage.detail) : stage.detail}
                    </span>
                  ) : null}
                </div>
              </li>
            ))}
          </ol>
        </article>

        <article className="journey-detail__card journey-detail__card--full">
          <h2>Constelação estratégica</h2>
          <p className="journey-detail__card-intro">
            Elementos que orbitam esta jornada e influenciam o roteiro criativo.
          </p>
          <div className="journey-detail__constellation">
            {constellationNodes.map((node) => (
              <div key={node.id} className={`journey-detail__constellation-node journey-detail__constellation-node--${node.accent}`}>
                <span className="journey-detail__constellation-label">{node.label}</span>
                <strong className="journey-detail__constellation-value">{node.value}</strong>
                {node.hint ? <span className="journey-detail__constellation-hint">{node.hint}</span> : null}
              </div>
            ))}
          </div>
        </article>

        <article className="journey-detail__card journey-detail__card--full">
          <h2>Metadados</h2>
          {metadataEntries.length ? (
            <ul className="journey-detail__metadata">
              {metadataEntries.map(([key, value]) => (
                <li key={key}>
                  <span className="journey-detail__metadata-key">{key}</span>
                  <span className="journey-detail__metadata-value">{value || "—"}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="journey-detail__empty">Nenhum metadado cadastrado.</p>
          )}
        </article>
      </section>

      {isConfirmOpen ? (
        <div className="journey-detail__modal" role="dialog" aria-modal="true">
          <div className="journey-detail__modal-content">
            <h3>Confirmar exclusão</h3>
            <p>
              Esta ação removerá a jornada "{journey.name}" e todo o histórico associado.
              Tem certeza de que deseja continuar?
            </p>
            <div className="journey-detail__modal-actions">
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => setIsConfirmOpen(false)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDelete}
                disabled={deleteJourney.isPending}
              >
                {deleteJourney.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Removendo...
                  </>
                ) : (
                  "Excluir"
                )}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
