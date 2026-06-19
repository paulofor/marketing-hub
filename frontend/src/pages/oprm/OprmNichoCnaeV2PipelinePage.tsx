import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";

const v2Stages = [
  {
    number: 1,
    title: "Candidate Generator",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Gerar candidatos neutros de subnicho sem contaminar nome, dor, canal ou promessa antes da evidência.",
    output:
      "4 a 6 candidatos comparáveis, com identidade do executor, job operacional e hipóteses separadas.",
    businessGate:
      "Nenhum vencedor obrigatório quando não houver candidato viável.",
  },
  {
    number: 2,
    title: "Source Safety Filter",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Bloquear domínios inseguros, conteúdo inadequado e resultados fora do contexto antes de gastar IA.",
    output: "Lista segura e deduplicada de URLs candidatas para pesquisa.",
    businessGate: "Conteúdo inseguro ou contaminado não entra no pipeline.",
  },
  {
    number: 3,
    title: "Adaptive Query Planner",
    status: "Design aprovado · implementação inicial",
    purpose:
      "Planejar buscas pelos gaps reais de conhecimento, reaproveitando queries, fontes e falhas anteriores.",
    output:
      "Plano de pesquisa curto, natural e orientado a lacunas de evidência.",
    businessGate:
      "A pesquisa aprofunda apenas o que pode mudar a decisão comercial.",
  },
  {
    number: 4,
    title: "Candidate Tournament",
    status: "Design",
    purpose:
      "Comparar candidatos por densidade e qualidade de evidências antes de escolher finalistas.",
    output: "Até dois finalistas ou decisão NO_VIABLE_SUBNICHE.",
    businessGate:
      "O vencedor nasce de evidência observada, não de opinião prévia do modelo.",
  },
  {
    number: 5,
    title: "Source Fetcher + Reranker",
    status: "Design",
    purpose:
      "Coletar páginas úteis e priorizar fontes diretas, independentes e alinhadas ao objetivo do gate.",
    output:
      "Snapshots rastreáveis com origem, trecho curto, metadados e custo.",
    businessGate:
      "Fonte adjacente não substitui prova direta do executor específico.",
  },
  {
    number: 6,
    title: "Signal Extractor",
    status: "Design aprovado · implementação parcial",
    purpose:
      "Extrair claims somente quando houver trecho exato, ator correto, contexto compatível e relação sustentada.",
    output:
      "Claims auditáveis com trecho literal, fonte e diagnóstico semântico.",
    businessGate: "Nenhum claim sem trecho exato pode avançar para síntese.",
  },
  {
    number: 7,
    title: "Semantic Judge + Entailment",
    status: "Design",
    purpose:
      "Validar se o trecho realmente sustenta a afirmação sobre o executor e o contexto pesquisado.",
    output:
      "Claims aprovados, rejeitados, contraditórios ou pendentes por nível de evidência.",
    businessGate: "Proximidade lexical não é prova de mercado.",
  },
  {
    number: 8,
    title: "Knowledge Accumulator",
    status: "Design aprovado · implementação parcial",
    purpose:
      "Consolidar conhecimento versionado do ciclo, preservando fontes aceitas, rejeições, gaps e aprendizados.",
    output:
      "Snapshot de conhecimento com versão, linhagem e lacunas acionáveis.",
    businessGate:
      "Reprocessar sem repetir erro, fonte rejeitada ou custo desnecessário.",
  },
  {
    number: 9,
    title: "Reprocess Controller",
    status: "Design aprovado · implementação parcial",
    purpose:
      "Decidir o menor rewind necessário quando um gate falhar por qualidade, mantendo o mesmo job quando aplicável.",
    output:
      "Retry técnico ou reprocessamento cognitivo com motivo, estágio de retorno e versão de conhecimento.",
    businessGate:
      "Falha técnica não vira reprovação de mercado; falta de evidência não reinicia tudo.",
  },
  {
    number: 10,
    title: "Routine Synthesizer",
    status: "Design",
    purpose:
      "Sintetizar rotina, dores e resultados apenas a partir de claims aprovados e evidências rastreáveis.",
    output: "Rotina funcional do executor com evidências e limites explícitos.",
    businessGate: "Síntese não pode inventar dor, canal ou impacto econômico.",
  },
  {
    number: 11,
    title: "Evidence Level Gate E0–E5",
    status: "Design",
    purpose:
      "Separar existência da atividade, dor prática, impacto econômico e intenção de compra por nível de evidência.",
    output:
      "Nível E0 a E5, confiança explicável, motivos de reprovação e próximos movimentos.",
    businessGate:
      "Materialização automática só avança com evidência comercial mínima.",
  },
  {
    number: 12,
    title: "Enriched Niche Materializer",
    status: "Bloqueado por feature flag",
    purpose:
      "Materializar o nicho enriquecido somente depois dos gates de evidência, qualidade e segurança.",
    output:
      "Nicho pronto para decisão de produto: executor, dor, resultado, mecanismo plausível e fontes.",
    businessGate: "A v2 calibra antes de publicar automaticamente para vendas.",
  },
];

export default function OprmNichoCnaeV2PipelinePage() {
  const { cnaeCode } = useParams();
  const decodedCnaeCode = cnaeCode ? decodeURIComponent(cnaeCode) : undefined;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <PageTitle>Pipeline NichoCNAE v2</PageTitle>
            <p className="text-secondary mb-0">
              Design das etapas da v2 para transformar CNAEs em nichos vendáveis
              com evidência auditável, reprocessamento inteligente e gates de
              qualidade antes da materialização.
            </p>
          </div>
          <Link className="btn btn-outline-secondary" to="/oprm">
            Voltar para CNAEs
          </Link>
        </div>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between gap-3">
            <div>
              <h2 className="h5 mb-1">
                {decodedCnaeCode
                  ? `CNAE ${decodedCnaeCode}`
                  : "Visão geral da v2"}
              </h2>
              <p className="text-secondary mb-0">
                A tela é um mapa de produto: mostra a sequência planejada mesmo
                quando uma etapa ainda está em design ou protegida por feature
                flag.
              </p>
            </div>
            <span className="badge text-bg-primary align-self-start">
              v2 · qualidade antes de escala
            </span>
          </div>
        </div>
      </section>

      <section className="row g-3" aria-label="Etapas do pipeline NichoCNAE v2">
        {v2Stages.map((stage) => (
          <article className="col-12 col-xl-6" key={stage.number}>
            <div className="card h-100 border-0 shadow-sm">
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex justify-content-between gap-3">
                  <div>
                    <span className="badge text-bg-light border mb-2">
                      Etapa {stage.number}
                    </span>
                    <h3 className="h5 mb-1">{stage.title}</h3>
                    <span className="small text-primary fw-semibold">
                      {stage.status}
                    </span>
                  </div>
                </div>
                <p className="mb-0">{stage.purpose}</p>
                <dl className="row small mb-0 g-2">
                  <dt className="col-sm-4 text-secondary fw-normal">Saída</dt>
                  <dd className="col-sm-8 mb-0">{stage.output}</dd>
                  <dt className="col-sm-4 text-secondary fw-normal">Gate</dt>
                  <dd className="col-sm-8 mb-0">{stage.businessGate}</dd>
                </dl>
              </div>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
