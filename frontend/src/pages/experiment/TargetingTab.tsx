import { Activity, Briefcase, Target } from "lucide-react";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import type { TargetingElement, TargetingElementType } from "../../api/targeting/types";
import { TargetingElementCard } from "../../components/TargetingElementCard";

interface TargetingTabProps {
  nicheId?: number;
  hypothesisId?: string;
  nicheName?: string | null;
  hypothesisTitle?: string | null;
}

const TYPE_CONFIGS: Array<{
  type: TargetingElementType;
  title: string;
  description: string;
  icon: typeof Target;
}> = [
  {
    type: "INTEREST",
    title: "Interesses",
    description: "Tópicos, paixões e temas salvos no Meta Ads.",
    icon: Target,
  },
  {
    type: "JOB_TITLE",
    title: "Cargos",
    description: "Funções profissionais ideais para o experimento.",
    icon: Briefcase,
  },
  {
    type: "BEHAVIOR",
    title: "Comportamentos",
    description: "Ações e hábitos relevantes detectados no Meta.",
    icon: Activity,
  },
];

export default function TargetingTab({
  nicheId,
  hypothesisId,
  nicheName,
  hypothesisTitle,
}: TargetingTabProps) {
  const nicheIdAsString = nicheId != null ? String(nicheId) : undefined;
  const {
    data,
    isLoading,
    isFetching,
    isError,
  } = useTargetingElementsByNiche(nicheIdAsString);

  if (nicheIdAsString == null || !hypothesisId) {
    return (
      <div className="mt-3">
        <p className="text-muted">
          Este experimento não possui nicho ou hipótese associados para exibir a segmentação.
        </p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="mt-3">
        <p>Carregando elementos de segmentação...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mt-3">
        <p className="text-danger">Não foi possível carregar os elementos de segmentação.</p>
      </div>
    );
  }

  const list = Array.isArray(data) ? data : [];
  const updating = isFetching && !isLoading;

  return (
    <div className="mt-3">
      {updating && (
        <p className="text-muted small">Atualizando elementos...</p>
      )}
      {TYPE_CONFIGS.map(({ type, title, description, icon: Icon }) => {
        const ofType = list.filter((element) => element.type === type);
        const relatedToHypothesis = ofType.filter(
          (element) => element.hypothesisId === hypothesisId,
        );
        const relatedToNiche = ofType.filter(
          (element) => element.hypothesisId !== hypothesisId,
        );

        return (
          <section key={type} className="mb-5">
            <div className="d-flex align-items-start gap-2 mb-2">
              <Icon size={18} className="text-primary mt-1" />
              <div>
                <h5 className="mb-0">{title}</h5>
                <p className="text-body-secondary small mb-0">{description}</p>
              </div>
              <span className="badge text-bg-secondary ms-auto">
                {ofType.length}
              </span>
            </div>
            <p className="text-body-secondary small">
              Elementos aprovados para {nicheName ?? "o nicho"}. Priorize os associados à hipótese
              {hypothesisTitle ? ` “${hypothesisTitle}”` : ""}.
            </p>

            <TargetingList
              title="Relacionados à hipótese"
              elements={relatedToHypothesis}
              emptyMessage="Nenhum elemento vinculado diretamente a esta hipótese."
              badgeLabel="Hipótese"
            />
            <TargetingList
              title="Disponíveis no nicho"
              elements={relatedToNiche}
              emptyMessage="Nenhum outro elemento cadastrado para este nicho."
              badgeLabel="Nicho"
            />
          </section>
        );
      })}
    </div>
  );
}

interface TargetingListProps {
  title: string;
  elements: TargetingElement[];
  emptyMessage: string;
  badgeLabel: string;
}

function TargetingList({
  title,
  elements,
  emptyMessage,
  badgeLabel,
}: TargetingListProps) {
  const list = Array.isArray(elements) ? elements : [];

  return (
    <div className="mb-4">
      <div className="d-flex align-items-center mb-2">
        <h6 className="mb-0">{title}</h6>
        <span className="badge text-bg-light ms-2">{list.length}</span>
      </div>
      {list.length === 0 ? (
        <p className="text-muted">{emptyMessage}</p>
      ) : (
        <div className="row row-cols-1 row-cols-md-2 g-3">
          {list.map((element) => (
            <div key={element.id} className="col">
              <TargetingElementCard element={element} badgeLabel={badgeLabel} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
