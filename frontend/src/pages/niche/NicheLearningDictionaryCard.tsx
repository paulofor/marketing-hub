import { useMemo } from "react";
import {
  useNicheLearningDictionary,
  type NicheLearningDictionary,
  type LearningStatement,
} from "../../api/niche/useNicheLearningDictionary";

interface Props {
  nicheId?: number;
}

type DictionaryKey = keyof Pick<
  NicheLearningDictionary,
  "pains" | "results" | "mechanisms" | "proofs" | "offers"
>;

const sections: Array<{ key: DictionaryKey; label: string }> = [
  { key: "pains", label: "Dores validadas" },
  { key: "results", label: "Resultados desejados" },
  { key: "mechanisms", label: "Mecanismos aceitos" },
  { key: "proofs", label: "Provas vencedoras" },
  { key: "offers", label: "Ofertas/âncoras" },
];

function formatDate(value?: string | null) {
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

export function NicheLearningDictionaryCard({ nicheId }: Props) {
  const { data, isLoading, error } = useNicheLearningDictionary(nicheId);
  const resolveEntries = (key: DictionaryKey): LearningStatement[] =>
    (data?.[key] ?? []) as LearningStatement[];

  const hasContent = useMemo(() => {
    if (!data) return false;
    return sections.some((section) => resolveEntries(section.key).length > 0);
  }, [data]);

  if (!isLoading && !error && !hasContent) {
    return null;
  }

  return (
    <section
      className="niche-section"
      aria-label="Banco de aprendizados do nicho"
    >
      <div className="niche-section__header">
        <div>
          <h2 className="niche-section__title">Banco de aprendizados</h2>
          <p className="niche-section__subtitle">
            Consolida os principais aprendizados Dor → Resultado → Mecanismo →
            Prova → Oferta para os próximos testes.
          </p>
          <p className="niche-section__status">
            Atualizado em {formatDate(data?.updatedAt)}
          </p>
        </div>
      </div>
      {error ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar o dicionário do nicho.
        </div>
      ) : isLoading ? (
        <div className="text-muted">Carregando aprendizados...</div>
      ) : !hasContent ? (
        <div className="text-muted">
          Ainda não existem aprendizados vinculados a este nicho. Gere um
          aprendizado a partir do painel do experimento para popular esta
          sessão.
        </div>
      ) : (
        <div className="row g-3">
          {sections.map((section) => {
            const entries = resolveEntries(section.key);
            return (
              <div className="col-12 col-lg-6" key={section.key as string}>
                <div className="border rounded-3 p-3 h-100">
                  <strong className="d-block mb-2">{section.label}</strong>
                  {entries.length === 0 ? (
                    <p className="text-body-secondary mb-0">
                      Sem itens cadastrados.
                    </p>
                  ) : (
                    <ul className="list-unstyled mb-0">
                      {entries.map((statement, index) => (
                        <li key={`${section.key}-${index}`} className="mb-3">
                          <div className="fw-semibold">
                            {statement.statement}
                          </div>
                          {statement.evidence ? (
                            <div className="text-body-secondary small">
                              {statement.evidence}
                            </div>
                          ) : null}
                          {statement.metricSignal ? (
                            <div className="text-body-secondary small">
                              {statement.metricSignal}
                            </div>
                          ) : null}
                          {statement.experimentName ? (
                            <div className="text-body-tertiary small">
                              Fonte: {statement.experimentName}
                            </div>
                          ) : null}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
