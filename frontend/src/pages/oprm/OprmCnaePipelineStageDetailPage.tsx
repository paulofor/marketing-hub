import { Link, useParams } from "react-router-dom";
import {
  isOprmCnaePipelineStageCode,
  type OprmCnaePipelineStageCode,
  useOprmCnaePipelineStageDetail,
} from "../../api/oprm/useOprmCnaePipelineStageDetail";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";

interface StageMetadata {
  code: OprmCnaePipelineStageCode;
  title: string;
  technicalName: string;
  description: string;
  populatedTables: string[];
  dataContent: string[];
  usesAiModel: boolean;
  aiModel?: string;
  aiRequestSummary?: string;
  aiResponseSummary?: string;
}

const stageMetadataByCode: Record<OprmCnaePipelineStageCode, StageMetadata> = {
  cycle: {
    code: "cycle",
    title: "1. Ciclo",
    technicalName: "oprmRoutineResearchCycle",
    description:
      "Controla o ciclo pai da pesquisa, mantendo o CNAE, o nicho operacional e os contadores das próximas etapas.",
    populatedTables: ["oprm_routine_research_cycle"],
    dataContent: [
      "CNAE, descrição, nome do nicho, modo de pesquisa e score de origem.",
      "Status do ciclo, totais de queries, fontes, snapshots, sinais, início, fim e erro operacional.",
    ],
    usesAiModel: false,
  },
  seed: {
    code: "seed",
    title: "2. Seed",
    technicalName: "oprmNicheResearchSeedBuilder",
    description:
      "Transforma o CNAE em seed operacional e frases de pesquisa para buscar sinais reais do nicho.",
    populatedTables: ["oprm_niche_research_seed", "oprm_niche_research_query"],
    dataContent: [
      "Seed com tipo de negócio, operação, cliente, objetos comerciais e suposições iniciais.",
      "Queries priorizadas com objetivo, grupo de fonte, status e contador de resultados.",
    ],
    usesAiModel: true,
    aiModel: "gpt-4.1-mini",
    aiRequestSummary:
      "Request para OpenAI Responses API com prompt de pesquisa de rotina real e schema JSON estruturado para seed e queries.",
    aiResponseSummary:
      "Resposta JSON validada e persistida como seed do nicho e lista de queries operacionais.",
  },
  search: {
    code: "search",
    title: "3. Busca",
    technicalName: "oprmSourceSearcher",
    description:
      "Executa as queries em fontes públicas e grava candidatos de páginas para análise posterior.",
    populatedTables: ["oprm_source_candidate", "oprm_niche_research_query"],
    dataContent: [
      "URLs candidatas, domínio, título, snippet, posição, provedor de busca, tipo de fonte e scores iniciais.",
      "Status das queries, quantidade de resultados e último erro de busca quando houver.",
    ],
    usesAiModel: false,
  },
  fetch: {
    code: "fetch",
    title: "4. Coleta",
    technicalName: "oprmSourceFetcher",
    description:
      "Coleta metadados e trecho curto das fontes candidatas selecionadas.",
    populatedTables: ["oprm_source_snapshot", "oprm_source_candidate"],
    dataContent: [
      "Título, domínio, tipo de fonte, data de publicação, snippet, trecho curto e metadados de aderência.",
      "Status da fonte candidata após coleta ou rejeição operacional.",
    ],
    usesAiModel: false,
  },
  signals: {
    code: "signals",
    title: "5. Sinais",
    technicalName: "oprmSignalExtractor",
    description:
      "Extrai sinais estruturados de rotina, dores, linguagem, tarefas e evidências a partir dos snapshots.",
    populatedTables: ["oprm_extracted_signal", "oprm_source_snapshot"],
    dataContent: [
      "Tipo do sinal, texto do sinal, trecho de evidência, domínio de origem e score de confiança.",
      "Contadores de sinais no ciclo e status dos snapshots processados.",
    ],
    usesAiModel: false,
  },
  synthesis: {
    code: "synthesis",
    title: "6. Síntese",
    technicalName: "oprmRoutineSynthesizer",
    description:
      "Monta o cartão de rotina que resume o que o público vive, sofre, deseja e fala.",
    populatedTables: ["oprm_niche_routine_card"],
    dataContent: [
      "Resumo de rotina, comportamento, canais, dores operacionais/emocionais, sonhos, medos e linguagem.",
      "Resumo de evidências, domínios usados e scores de confiança, rotina, dificuldade, diversidade e risco de solução.",
    ],
    usesAiModel: false,
  },
  mei: {
    code: "mei",
    title: "7. MEI",
    technicalName: "oprmMeiAudienceSegmenter",
    description:
      "Define o perfil comportamental MEI/autônomo dono-operador do nicho sem criar produto ou campanha.",
    populatedTables: ["oprm_mei_audience_profile", "oprm_niche_routine_card"],
    dataContent: [
      "Nome do público, termos de ocupação, modo de trabalho, rotina, tarefas, canais, dores, sonhos, medos e linguagem.",
      "Scores de aderência autônoma, evidência comportamental, frescor da fonte e riscos de desvio corporativo/solução.",
    ],
    usesAiModel: true,
    aiModel: "gpt-4.1-mini",
    aiRequestSummary:
      "Request para OpenAI Responses API com cartão de rotina, fontes e sinais para segmentar o público MEI/autônomo real.",
    aiResponseSummary:
      "Resposta JSON validada e persistida como perfil comportamental aprovado ou bloqueado pelo gate de qualidade.",
  },
  quality: {
    code: "quality",
    title: "8. Qualidade",
    technicalName: "oprmRoutineQualityGate",
    description:
      "Valida se a pesquisa ficou específica, recente, diversa e sem contaminação por solução pronta.",
    populatedTables: ["oprm_niche_routine_card", "oprm_routine_research_cycle"],
    dataContent: [
      "Status de qualidade, pronto para hipótese, notas do gate, avaliador e data da checagem.",
      "Scores de especificidade, confiança, duplicação, evidência de rotina, evidência de dificuldade e diversidade.",
    ],
    usesAiModel: false,
  },
  materialization: {
    code: "materialization",
    title: "9. Materialização",
    technicalName: "oprmEnrichedNicheMaterializer",
    description:
      "Grava o nicho enriquecido aprovado para alimentar hipóteses, ofertas e experimentos comerciais.",
    populatedTables: ["market_niche", "market_niche_enrichment_profile"],
    dataContent: [
      "Nicho final, CNAE, resumo de rotina, dores, resultados, oportunidades de mecanismo, evidências e fontes.",
      "IDs do nicho e perfil enriquecido, qualidade final, scores de evidência/diversidade e data de materialização.",
    ],
    usesAiModel: false,
  },
};

function formatJson(value: unknown) {
  return JSON.stringify(value, null, 2);
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function textValue(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "não informado";
  }
  return String(value);
}

function buildAiTelemetry(metadata: StageMetadata, data: unknown) {
  if (!metadata.usesAiModel) {
    return null;
  }
  const payload = asRecord(data);
  const seed = asRecord(payload?.seed) ?? payload;
  return {
    modelo: textValue(seed?.model ?? metadata.aiModel),
    request: metadata.aiRequestSummary,
    response: textValue(seed?.openAiResponseId ?? metadata.aiResponseSummary),
    tokensEntrada: textValue(seed?.inputTokens),
    tokensSaida: textValue(seed?.outputTokens),
    custoUsd: seed?.costUsd == null ? "não informado" : `US$ ${seed.costUsd}`,
    hasPersistedTelemetry:
      seed?.model != null || seed?.inputTokens != null || seed?.outputTokens != null || seed?.costUsd != null,
  };
}

export default function OprmCnaePipelineStageDetailPage() {
  const { cnaeCode, researchCycleId, stageCode } = useParams();
  const decodedCnaeCode = cnaeCode ? decodeURIComponent(cnaeCode) : "CNAE";
  const cycleId = Number(researchCycleId);
  const validStageCode = isOprmCnaePipelineStageCode(stageCode)
    ? stageCode
    : undefined;
  const metadata = validStageCode
    ? stageMetadataByCode[validStageCode]
    : undefined;
  const isValidCycleId = Number.isInteger(cycleId) && cycleId > 0;
  const { data, isLoading, isError, error } = useOprmCnaePipelineStageDetail(
    validStageCode,
    isValidCycleId ? cycleId : undefined,
  );
  const aiTelemetry = metadata ? buildAiTelemetry(metadata, data) : null;

  useBreadcrumbs([
    { label: "OPRM", to: "/oprm" },
    { label: "Detalhe do nicho", to: `/oprm/cnaes/${encodeURIComponent(decodedCnaeCode)}` },
    { label: "Detalhe da etapa" },
  ]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Detalhe da etapa NichoCNAE</PageTitle>
        <p className="text-secondary mb-0">
          Esta página concentra a tabela populada, o conteúdo gravado e a
          auditoria de IA da etapa, sem poluir a tela principal do CNAE.
        </p>
      </header>

      <OprmModuleNavigation />

      <div className="d-flex flex-wrap gap-2">
        <Link
          className="btn btn-outline-secondary btn-sm"
          to={`/oprm/cnaes/${encodeURIComponent(decodedCnaeCode)}`}
        >
          Voltar ao CNAE
        </Link>
        <Link className="btn btn-outline-secondary btn-sm" to="/oprm/pipeline">
          Ver pipeline geral
        </Link>
      </div>

      {!metadata || !validStageCode || !isValidCycleId ? (
        <div className="alert alert-warning" role="alert">
          Etapa ou ciclo inválido na URL. Volte ao detalhe do CNAE e abra os
          detalhes por um card válido.
        </div>
      ) : (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body d-flex flex-column gap-3">
              <div className="d-flex flex-wrap justify-content-between gap-3">
                <div>
                  <span className="badge text-bg-primary mb-2">
                    {metadata.technicalName}
                  </span>
                  <h2 className="h5 mb-2">{metadata.title}</h2>
                  <p className="text-secondary mb-0">{metadata.description}</p>
                </div>
                <div className="text-secondary small text-lg-end">
                  <span className="d-block">CNAE {decodedCnaeCode}</span>
                  <span className="d-block">Ciclo #{cycleId}</span>
                </div>
              </div>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-3">Tabelas e conteúdo populado</h2>
              <div className="table-responsive">
                <table className="table table-sm align-middle mb-0">
                  <thead>
                    <tr>
                      <th scope="col">Tabela</th>
                      <th scope="col">Conteúdo dos dados</th>
                    </tr>
                  </thead>
                  <tbody>
                    {metadata.populatedTables.map((tableName, index) => (
                      <tr key={tableName}>
                        <td className="fw-semibold text-nowrap">{tableName}</td>
                        <td>{metadata.dataContent[index] ?? metadata.dataContent[0]}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-3">Acesso a modelo de IA</h2>
              {aiTelemetry ? (
                <div className="d-flex flex-column gap-3">
                  <dl className="row g-2 mb-0">
                    <dt className="col-md-3 text-secondary fw-normal">Modelo</dt>
                    <dd className="col-md-9 mb-0 fw-semibold">
                      {aiTelemetry.modelo}
                    </dd>
                    <dt className="col-md-3 text-secondary fw-normal">Request</dt>
                    <dd className="col-md-9 mb-0">{aiTelemetry.request}</dd>
                    <dt className="col-md-3 text-secondary fw-normal">Response</dt>
                    <dd className="col-md-9 mb-0">{aiTelemetry.response}</dd>
                    <dt className="col-md-3 text-secondary fw-normal">Tokens</dt>
                    <dd className="col-md-9 mb-0">
                      Entrada: {aiTelemetry.tokensEntrada} · Saída:{" "}
                      {aiTelemetry.tokensSaida}
                    </dd>
                    <dt className="col-md-3 text-secondary fw-normal">Custo</dt>
                    <dd className="col-md-9 mb-0">{aiTelemetry.custoUsd}</dd>
                  </dl>
                  {!aiTelemetry.hasPersistedTelemetry ? (
                    <div className="alert alert-info mb-0 small" role="status">
                      Esta execução ainda não possui telemetria persistida; novas execuções da etapa seed passam a gravar modelo, tokens e custo.
                    </div>
                  ) : null}
                </div>
              ) : (
                <div className="alert alert-secondary mb-0" role="status">
                  Esta etapa não acessa modelo de IA diretamente; ela usa dados
                  já gravados por etapas anteriores ou coleta determinística.
                </div>
              )}
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-3">Dados retornados pelo backend</h2>
              {isLoading ? (
                <div className="text-secondary">Carregando dados da etapa...</div>
              ) : isError ? (
                <div className="alert alert-warning mb-0" role="alert">
                  {error instanceof Error
                    ? error.message
                    : "Não foi possível carregar os dados."}
                </div>
              ) : data ? (
                <pre className="bg-body-tertiary border rounded p-3 small overflow-auto mb-0">
                  {formatJson(data)}
                </pre>
              ) : (
                <div className="alert alert-info mb-0" role="status">
                  Ainda não há dados públicos gravados para esta etapa neste
                  ciclo, ou o registro existe mas ainda não foi aprovado para
                  consumo.
                </div>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
