import { Link, useParams } from "react-router-dom";
import {
  type OprmNicheResearchSeedDetail,
  useOprmNicheResearchSeedBuilderDetail,
} from "../../api/oprm/useOprmNicheResearchSeedBuilderDetail";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";

const OPENAI_RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";
const OPENAI_MODEL = "gpt-4.1-mini";

function formatProcessedAt(value?: string | null) {
  if (!value) {
    return "Não informado";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Horário indisponível";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function formatQueryGoal(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function safe(value?: string | null) {
  return value == null || value.trim() === "" ? "não informado" : value;
}

function buildPrompt(detail: {
  researchCycleId: number;
  seed?: {
    cnaeCode: string;
    cnaeDescription: string;
    nicheName: string;
  } | null;
}) {
  const lines = [
    "Você é o construtor da etapa 2 do pipeline OPRM nichocnae.",
    "Objetivo: conhecer como o nicho funciona na rotina, sem criar oferta, produto, campanha ou landing page.",
    "Use o eixo Dor → Resultado → Mecanismo → Prova → Oferta apenas como referência distante; nesta etapa gere apenas seed e frases de pesquisa.",
    "",
    "Dados do ciclo:",
    `researchCycleId: ${detail.researchCycleId}`,
    `cnaeCode: ${safe(detail.seed?.cnaeCode)}`,
    `cnaeDescription: ${safe(detail.seed?.cnaeDescription)}`,
    `nicheName: ${safe(detail.seed?.nicheName)}`,
    "sourceScore: não disponível no detalhe persistido",
    "",
    "Regras obrigatórias:",
    "1. Responda somente JSON válido aderente ao schema solicitado.",
    "2. Gere um seed que responda quem é o profissional MEI/autônomo pesquisado no Brasil.",
    "3. Gere de 12 a 15 queries, cada uma em linha lógica própria no array queries.",
    "4. Cada query deve conter o nicho/CNAE e marcador explícito de pessoa: MEI, autônomo, trabalhador por conta própria, profissional autônomo ou dono-operador.",
    "5. Cada query deve conter marcador Brasil/brasileiro/pt-BR/estado/cidade ou fonte brasileira recente quando fizer sentido.",
    "6. Cubra rotina, modo de trabalho, aquisição de clientes, atendimento, cobrança, agenda, materiais, entrega, retrabalho, dores, sonhos, medos, canais e linguagem real.",
    "7. Não gere query genérica como 'como vender mais' e não direcione pesquisa para solução, produto, oferta, IA, automação, software, curso ou campanha.",
    "8. Todas as queries devem sair com status PENDING e createdBy AI.",
    "9. Use queryGoal somente entre MEI_ROUTINE_DISCOVERY, AUTONOMOUS_WORK_MODE_DISCOVERY, CUSTOMER_ACQUISITION_BEHAVIOR_DISCOVERY, DAILY_OPERATION_PAIN_DISCOVERY, EMOTIONAL_PAIN_DISCOVERY, DREAM_DISCOVERY, FEAR_DISCOVERY, CHANNEL_BEHAVIOR_DISCOVERY, LANGUAGE_DISCOVERY e SOURCE_FRESHNESS_DISCOVERY.",
    "10. Não inclua metadado técnico, comentário operacional, debugInfo ou JSON serializado dentro de texto funcional.",
  ];
  return lines.join("\n");
}

function buildStrictSchema() {
  const string = { type: "string" };
  const integer = { type: "integer" };
  return {
    type: "object",
    additionalProperties: false,
    required: ["researchCycleId", "seed", "queries"],
    properties: {
      researchCycleId: integer,
      seed: {
        type: "object",
        additionalProperties: false,
        required: [
          "researchCycleId",
          "cnaeCode",
          "cnaeDescription",
          "nicheName",
          "businessType",
          "operationType",
          "customerType",
          "commercialObjects",
          "initialAssumptions",
          "confidenceLevel",
          "createdBy",
        ],
        properties: {
          researchCycleId: integer,
          cnaeCode: string,
          cnaeDescription: string,
          nicheName: string,
          businessType: string,
          operationType: string,
          customerType: string,
          commercialObjects: string,
          initialAssumptions: string,
          confidenceLevel: {
            type: "string",
            enum: ["INFERRED_FROM_CNAE", "LOW_CONFIDENCE", "NEEDS_RESEARCH"],
          },
          createdBy: { type: "string", enum: ["AI"] },
        },
      },
      queries: {
        type: "array",
        minItems: 12,
        maxItems: 15,
        items: {
          type: "object",
          additionalProperties: false,
          required: [
            "researchCycleId",
            "queryText",
            "queryGoal",
            "sourceGroup",
            "priority",
            "status",
            "createdBy",
          ],
          properties: {
            researchCycleId: integer,
            queryText: string,
            queryGoal: {
              type: "string",
              enum: [
                "MEI_ROUTINE_DISCOVERY",
                "AUTONOMOUS_WORK_MODE_DISCOVERY",
                "CUSTOMER_ACQUISITION_BEHAVIOR_DISCOVERY",
                "DAILY_OPERATION_PAIN_DISCOVERY",
                "EMOTIONAL_PAIN_DISCOVERY",
                "DREAM_DISCOVERY",
                "FEAR_DISCOVERY",
                "CHANNEL_BEHAVIOR_DISCOVERY",
                "LANGUAGE_DISCOVERY",
                "SOURCE_FRESHNESS_DISCOVERY",
              ],
            },
            sourceGroup: string,
            priority: integer,
            status: { type: "string", enum: ["PENDING"] },
            createdBy: { type: "string", enum: ["AI"] },
          },
        },
      },
    },
  };
}

function buildAiRequestPreview(detail: {
  researchCycleId: number;
  seed?: {
    cnaeCode: string;
    cnaeDescription: string;
    nicheName: string;
  } | null;
}) {
  return {
    method: "POST",
    endpoint: OPENAI_RESPONSES_ENDPOINT,
    headers: {
      Authorization: "Bearer ***",
      "Content-Type": "application/json",
    },
    body: {
      model: OPENAI_MODEL,
      input: buildPrompt(detail),
      text: {
        format: {
          type: "json_schema",
          name: "oprm_niche_research_seed_builder",
          schema: buildStrictSchema(),
          strict: true,
        },
      },
    },
  };
}

function buildGeneratedJson(
  detail: NonNullable<ReturnType<typeof buildGeneratedPayload>>,
) {
  return JSON.stringify(detail, null, 2);
}

function buildGeneratedPayload(seed?: OprmNicheResearchSeedDetail | null) {
  if (!seed) {
    return null;
  }
  return {
    researchCycleId: seed.researchCycleId,
    seed: {
      researchCycleId: seed.researchCycleId,
      cnaeCode: seed.cnaeCode,
      cnaeDescription: seed.cnaeDescription,
      nicheName: seed.nicheName,
      businessType: seed.businessType,
      operationType: seed.operationType,
      customerType: seed.customerType,
      commercialObjects: seed.commercialObjects,
      initialAssumptions: seed.initialAssumptions,
      confidenceLevel: seed.confidenceLevel,
      createdBy: seed.createdBy,
    },
    queries: seed.queries.map((query) => ({
      researchCycleId: query.researchCycleId,
      queryText: query.queryText,
      queryGoal: query.queryGoal,
      sourceGroup: query.sourceGroup,
      priority: query.priority,
      status: query.status,
      createdBy: query.createdBy,
    })),
  };
}

export default function OprmNicheResearchSeedBuilderDetailPage() {
  const { researchCycleId } = useParams();
  const cycleId = Number(researchCycleId);
  const isValidCycleId = Number.isInteger(cycleId) && cycleId > 0;
  const { data, isLoading, isError, error } =
    useOprmNicheResearchSeedBuilderDetail(isValidCycleId ? cycleId : undefined);
  const generatedPayload = buildGeneratedPayload(data?.seed ?? null);
  const aiRequestPreview = data ? buildAiRequestPreview(data) : null;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Detalhe da IA — Seed de Pesquisa do Nicho</PageTitle>
        <p className="text-secondary mb-0">
          Mostra para o usuário como a etapa 2 foi solicitada à IA e qual JSON
          estruturado foi gravado para alimentar as próximas etapas do pipeline.
        </p>
      </header>

      <OprmModuleNavigation />

      <div>
        <Link className="btn btn-outline-secondary btn-sm" to="/oprm/pipeline">
          Voltar para o pipeline
        </Link>
      </div>

      {!isValidCycleId ? (
        <div className="alert alert-warning" role="alert">
          Ciclo inválido na URL. Volte ao pipeline e escolha uma execução
          válida.
        </div>
      ) : isLoading ? (
        <div className="card border-0 shadow-sm">
          <div className="card-body text-secondary">
            Carregando detalhe da IA...
          </div>
        </div>
      ) : isError ? (
        <div className="alert alert-warning" role="alert">
          Não foi possível carregar o detalhe da etapa 2:{" "}
          {error instanceof Error ? error.message : "erro sem detalhe"}.
        </div>
      ) : data ? (
        <>
          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex flex-column flex-lg-row justify-content-between gap-3">
                <div>
                  <span className="badge text-bg-primary mb-2">
                    oprmNicheResearchSeedBuilder
                  </span>
                  <h2 className="h5 mb-2">Ciclo #{data.researchCycleId}</h2>
                  <p className="text-secondary mb-0">
                    Status do ciclo: <strong>{data.cycleStatus}</strong> ·
                    Queries gravadas: <strong>{data.cycleTotalQueries}</strong>
                  </p>
                </div>
                {data.seed ? (
                  <div className="text-lg-end small text-secondary">
                    <span className="d-block">
                      Seed #{data.seed.nicheResearchSeedId}
                    </span>
                    <span className="d-block">
                      Gerado em {formatProcessedAt(data.seed.createdAt)}
                    </span>
                    <span className="d-block">
                      Criado por {data.seed.createdBy}
                    </span>
                  </div>
                ) : null}
              </div>
              {data.cycleErrorMessage ? (
                <div className="alert alert-danger mt-3 mb-0" role="alert">
                  {data.cycleErrorMessage}
                </div>
              ) : null}
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-2">Requisição enviada para a IA</h2>
              <p className="text-secondary">
                A chave de API é mascarada. Esta prévia reconstrói a requisição
                a partir do contrato atual da etapa 2 e dos dados persistidos do
                ciclo.
              </p>
              {aiRequestPreview ? (
                <pre className="bg-dark text-light rounded p-3 small overflow-auto mb-0">
                  {JSON.stringify(aiRequestPreview, null, 2)}
                </pre>
              ) : null}
            </div>
          </section>

          <section className="card border-0 shadow-sm">
            <div className="card-body">
              <h2 className="h5 mb-3">JSON gerado e gravado</h2>
              {generatedPayload ? (
                <pre className="bg-body-tertiary border rounded p-3 small overflow-auto mb-0">
                  {buildGeneratedJson(generatedPayload)}
                </pre>
              ) : (
                <div className="alert alert-info mb-0" role="status">
                  Este ciclo ainda não possui seed/queries gravados pela etapa
                  2.
                </div>
              )}
            </div>
          </section>

          {data.seed ? (
            <section className="card border-0 shadow-sm">
              <div className="card-body">
                <h2 className="h5 mb-3">Leitura operacional para o usuário</h2>
                <dl className="row g-3 mb-4">
                  <dt className="col-md-3 text-secondary fw-normal">Nicho</dt>
                  <dd className="col-md-9 mb-0 fw-semibold">
                    {data.seed.nicheName}
                  </dd>
                  <dt className="col-md-3 text-secondary fw-normal">
                    Tipo de negócio
                  </dt>
                  <dd className="col-md-9 mb-0">{data.seed.businessType}</dd>
                  <dt className="col-md-3 text-secondary fw-normal">
                    Objetos comerciais
                  </dt>
                  <dd className="col-md-9 mb-0">
                    {data.seed.commercialObjects}
                  </dd>
                  <dt className="col-md-3 text-secondary fw-normal">
                    Suposições iniciais
                  </dt>
                  <dd className="col-md-9 mb-0">
                    {data.seed.initialAssumptions}
                  </dd>
                </dl>
                <div className="table-responsive">
                  <table className="table table-sm align-middle mb-0">
                    <thead>
                      <tr>
                        <th scope="col">#</th>
                        <th scope="col">Query</th>
                        <th scope="col">Objetivo</th>
                        <th scope="col">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.seed.queries.map((query) => (
                        <tr key={query.queryId}>
                          <td>{query.priority}</td>
                          <td>{query.queryText}</td>
                          <td>{formatQueryGoal(query.queryGoal)}</td>
                          <td>
                            <span className="badge text-bg-light border text-secondary">
                              {query.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
