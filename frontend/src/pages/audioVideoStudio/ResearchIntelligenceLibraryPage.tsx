import { useMemo, useState } from "react";
import { BookOpen, Filter, Search, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { useResearchIntelligenceCatalog } from "../../api/salesVideo/useResearchIntelligenceCatalog";
import type {
  ResearchIntelligenceAgentPolicy,
  ResearchIntelligenceCard,
} from "../../api/salesVideo/types";
import PageTitle from "../../components/PageTitle";
import "./AudioVideoStudioPage.css";

const INITIAL_CARD_LIMIT = 12;

function normalized(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

function isActive(card: ResearchIntelligenceCard, evaluatedOn: string) {
  return (
    (!card.publishedOn || card.publishedOn <= evaluatedOn) &&
    (!card.validUntil || card.validUntil >= evaluatedOn)
  );
}

function authorityLabel(authority: string) {
  const labels: Record<string, string> = {
    PRODUCTION_ADVISORY: "orienta produção",
    COMMUNICATION_ADVISORY: "orienta comunicação",
    REVIEW_CRITERIA_ONLY: "somente revisão",
    FACTUAL_ADVISORY: "orienta pesquisa factual",
    STRATEGY_ADVISORY: "orienta estratégia",
    PRODUCT_ADVISORY: "orienta experiência do produto",
    MEASUREMENT_ADVISORY: "orienta medição",
  };
  return labels[authority] ?? authority;
}

function matchesAgent(
  card: ResearchIntelligenceCard,
  selectedAgent: string,
  policies: ResearchIntelligenceAgentPolicy[],
) {
  if (!selectedAgent) return true;
  const policy = policies.find((item) => item.agentKey === selectedAgent);
  return Boolean(
    policy?.collections.includes(card.collection) ||
    policy?.assignments?.some((item) => item.cardId === card.cardId),
  );
}

export default function ResearchIntelligenceLibraryPage() {
  const catalogQuery = useResearchIntelligenceCatalog();
  const [search, setSearch] = useState("");
  const [collection, setCollection] = useState("");
  const [agent, setAgent] = useState("");
  const [status, setStatus] = useState<"ACTIVE" | "ALL" | "EXPIRED">("ACTIVE");
  const [visibleCards, setVisibleCards] = useState(INITIAL_CARD_LIMIT);
  const catalog = catalogQuery.data;

  const collections = useMemo(
    () =>
      Array.from(
        new Set((catalog?.cards ?? []).map((card) => card.collection)),
      ).sort((left, right) => left.localeCompare(right, "pt-BR")),
    [catalog?.cards],
  );

  const filteredCards = useMemo(() => {
    if (!catalog) return [];
    const query = normalized(search.trim());
    return catalog.cards.filter((card) => {
      const active = isActive(card, catalog.evaluatedOn);
      const searchable = normalized(
        [
          card.title,
          card.finding,
          card.mechanism,
          card.commercialApplication,
          card.experimentHypothesis,
          card.collection,
        ].join(" "),
      );
      return (
        (!query || searchable.includes(query)) &&
        (!collection || card.collection === collection) &&
        matchesAgent(card, agent, catalog.agentPolicies) &&
        (status === "ALL" ||
          (status === "ACTIVE" && active) ||
          (status === "EXPIRED" && !active))
      );
    });
  }, [agent, catalog, collection, search, status]);

  const resetVisibleCards = () => setVisibleCards(INITIAL_CARD_LIMIT);

  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title="Biblioteca de Inteligência do Harness"
        subtitle="Pesquisas para orientar agentes, tarefas da Cadeia de Valor e projetos audiovisuais."
      />

      {catalogQuery.isLoading ? (
        <section className="audio-video-studio-page__section">
          Carregando biblioteca global...
        </section>
      ) : catalogQuery.isError || !catalog ? (
        <section className="audio-video-studio-page__section">
          Não foi possível carregar a biblioteca agora. Nenhuma geração deve
          assumir cartões ausentes como pesquisa válida.
        </section>
      ) : (
        <>
          <section className="audio-video-studio-page__section audio-video-studio-page__research-library-summary">
            <div>
              <span>Fonte global única</span>
              <strong>{catalog.totalCompiledCards}</strong>
              <small>cartões versionados no catálogo global</small>
            </div>
            <div>
              <span>Elegíveis hoje</span>
              <strong>{catalog.activeCards}</strong>
              <small>fontes dentro da validade</small>
            </div>
            <div>
              <span>Cobertura</span>
              <strong>Todos</strong>
              <small>projetos atuais e futuros</small>
            </div>
          </section>

          <section className="audio-video-studio-page__section audio-video-studio-page__research-intelligence">
            <div className="audio-video-studio-page__research-heading">
              <div>
                <p className="audio-video-studio-page__eyebrow">
                  Catálogo global, seleção contextual
                </p>
                <h2>Como a biblioteca chega aos agentes</h2>
              </div>
              <span>{catalog.contractVersion}</span>
            </div>
            <p>
              O catálogo não pertence ao Vega #91. Ao abrir, criar ou executar
              qualquer projeto audiovisual, o backend seleciona até quatro
              cartões por agente conforme o contexto. O job persiste a seleção e
              os hashes usados para auditoria. Fontes Markdown e cartões
              aprovados pela API externa usam a mesma seleção canônica.
            </p>
            <p>
              Referências cadastradas nas entradas do agente também chegam às
              suas tarefas, com orientação de aplicação e limite de quatro
              cartões. Os vínculos abaixo vêm do cadastro versionado. A pesquisa
              orienta hipóteses; vendas e entrega precisam de evidência real.
            </p>
            <div className="audio-video-studio-page__research-policy-grid">
              {catalog.agentPolicies.map((policy) => (
                <article key={policy.agentKey}>
                  <Sparkles size={18} aria-hidden="true" />
                  <strong>{policy.agentName}</strong>
                  <span>{authorityLabel(policy.authority)}</span>
                  <p>{policy.purpose}</p>
                  <small>
                    {policy.collections.join(" · ") || "Curadoria para tarefas"}{" "}
                    · até {policy.maxCardsPerContext}
                  </small>
                  {(policy.assignments?.length ?? 0) > 0 ? (
                    <p>{policy.assignments!.length} referências direcionadas</p>
                  ) : null}
                  {policy.assignments
                    ?.filter((item) => !item.available)
                    .map((item) => (
                      <p key={item.cardId}>
                        Referência indisponível: {item.cardId}
                      </p>
                    ))}
                  {policy.agentId ? (
                    <Link to={`/agents/${policy.agentId}/edit`}>
                      Editar referências de {policy.agentName}
                    </Link>
                  ) : null}
                </article>
              ))}
            </div>
            <Link
              className="audio-video-studio-page__secondary-action audio-video-studio-page__research-library-link"
              to="/audio-video-studio/projects"
            >
              Ver seleções nos projetos
            </Link>
          </section>

          <section className="audio-video-studio-page__section">
            <div className="audio-video-studio-page__section-heading">
              <div>
                <p className="audio-video-studio-page__eyebrow">
                  <BookOpen size={16} aria-hidden="true" /> Cartões versionados
                </p>
                <h2>Pesquisar no catálogo</h2>
                <p>
                  A lista mostra o acervo global. Dentro de um projeto, aparece
                  somente a seleção realmente entregue aos agentes.
                </p>
              </div>
            </div>

            <div className="audio-video-studio-page__research-filters">
              <label>
                <span>
                  <Search size={16} aria-hidden="true" /> Buscar
                </span>
                <input
                  type="search"
                  value={search}
                  onChange={(event) => {
                    setSearch(event.target.value);
                    resetVisibleCards();
                  }}
                  placeholder="Achado, mecanismo, hipótese ou aplicação"
                />
              </label>
              <label>
                <span>
                  <Filter size={16} aria-hidden="true" /> Coleção
                </span>
                <select
                  value={collection}
                  onChange={(event) => {
                    setCollection(event.target.value);
                    resetVisibleCards();
                  }}
                >
                  <option value="">Todas as coleções</option>
                  {collections.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>Agente</span>
                <select
                  value={agent}
                  onChange={(event) => {
                    setAgent(event.target.value);
                    resetVisibleCards();
                  }}
                >
                  <option value="">Todos os agentes</option>
                  {catalog.agentPolicies.map((policy) => (
                    <option key={policy.agentKey} value={policy.agentKey}>
                      {policy.agentName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>Validade</span>
                <select
                  value={status}
                  onChange={(event) => {
                    setStatus(event.target.value as typeof status);
                    resetVisibleCards();
                  }}
                >
                  <option value="ACTIVE">Elegíveis hoje</option>
                  <option value="ALL">Todos</option>
                  <option value="EXPIRED">Fora da validade</option>
                </select>
              </label>
            </div>

            <p className="audio-video-studio-page__research-result-count">
              {filteredCards.length} cartões encontrados · exibindo{" "}
              {Math.min(visibleCards, filteredCards.length)}
            </p>

            <div className="audio-video-studio-page__research-library-cards">
              {filteredCards.slice(0, visibleCards).map((card) => {
                const active = isActive(card, catalog.evaluatedOn);
                return (
                  <article key={card.cardId}>
                    <header>
                      <span>{card.collection}</span>
                      <small data-status={active ? "active" : "expired"}>
                        {active
                          ? "Elegível"
                          : card.publishedOn &&
                              card.publishedOn > catalog.evaluatedOn
                            ? "Fonte futura"
                            : "Vencido"}
                      </small>
                    </header>
                    <strong>{card.title}</strong>
                    <p>{card.finding}</p>
                    {catalog.agentPolicies.flatMap((policy) =>
                      (policy.assignments ?? [])
                        .filter((item) => item.cardId === card.cardId)
                        .map((item) => (
                          <div key={policy.agentKey}>
                            <strong>Direcionado a {policy.agentName}</strong>
                            <p>{item.guidance}</p>
                          </div>
                        )),
                    )}
                    <details>
                      <summary>Ver mecanismo e aplicação</summary>
                      <dl>
                        <div>
                          <dt>Mecanismo</dt>
                          <dd>{card.mechanism}</dd>
                        </div>
                        <div>
                          <dt>Aplicação comercial</dt>
                          <dd>{card.commercialApplication}</dd>
                        </div>
                        <div>
                          <dt>Hipótese de experimento</dt>
                          <dd>{card.experimentHypothesis}</dd>
                        </div>
                        <div>
                          <dt>Riscos e limites</dt>
                          <dd>
                            {card.risks} {card.limits}
                          </dd>
                        </div>
                      </dl>
                    </details>
                    <small>
                      {card.cardId} · {card.sourcePath} · SHA{" "}
                      {card.sourceSha256.slice(0, 12)}
                      {card.validUntil ? ` · validade ${card.validUntil}` : ""}
                    </small>
                  </article>
                );
              })}
            </div>

            {visibleCards < filteredCards.length ? (
              <button
                className="audio-video-studio-page__secondary-action audio-video-studio-page__research-load-more"
                type="button"
                onClick={() =>
                  setVisibleCards((current) => current + INITIAL_CARD_LIMIT)
                }
              >
                Exibir mais cartões
              </button>
            ) : null}
          </section>

          <section className="audio-video-studio-page__section">
            <h2>Limites obrigatórios</h2>
            <ul>
              {catalog.limitations.map((limitation) => (
                <li key={limitation}>{limitation}</li>
              ))}
            </ul>
          </section>
        </>
      )}
    </div>
  );
}
