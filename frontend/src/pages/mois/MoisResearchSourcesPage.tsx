import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";

type ResearchSource = {
  name: string;
  category: string;
  description: string;
  url: string;
  useCase: string;
};

type WeeklyHighlight = {
  sourceName: string;
  insight: string;
  action: string;
};

const RESEARCH_SOURCES: ResearchSource[] = [
  {
    name: "Meta Ad Library",
    category: "Anúncios",
    description: "Biblioteca pública de anúncios ativos em Facebook e Instagram.",
    url: "https://www.facebook.com/ads/library/",
    useCase: "Mapear ângulos de copy, criativos e ofertas por nicho.",
  },
  {
    name: "TikTok Creative Center",
    category: "Anúncios",
    description: "Repositório de tendências e anúncios com filtros por região e segmento.",
    url: "https://ads.tiktok.com/business/creativecenter/inspiration/topads",
    useCase: "Encontrar padrões de gancho, ritmo e CTA em vídeos curtos.",
  },
  {
    name: "Google Ads Transparency Center",
    category: "Anúncios",
    description: "Consulta de anúncios exibidos no ecossistema Google.",
    url: "https://adstransparency.google.com/",
    useCase: "Validar recorrência de mensagens e posicionamento por marca.",
  },
  {
    name: "YouTube",
    category: "VSL / Conteúdo",
    description: "Plataforma para localizar VSLs, webinars e conteúdos de venda longos.",
    url: "https://www.youtube.com/",
    useCase: "Analisar narrativa de dor, mecanismo, prova e oferta em vídeo.",
  },
  {
    name: "Hotmart Marketplace",
    category: "Produtos digitais",
    description: "Marketplace com ofertas, descrições e formatos de infoprodutos.",
    url: "https://www.hotmart.com/pt-br/marketplace",
    useCase: "Comparar promessa, ticket e estrutura de produto por categoria.",
  },
  {
    name: "Monetizze",
    category: "Produtos digitais",
    description: "Plataforma com campanhas e produtos em múltiplos nichos.",
    url: "https://www.monetizze.com.br/",
    useCase: "Buscar referências de oferta e modelo comercial.",
  },
  {
    name: "Eduzz",
    category: "Produtos digitais",
    description: "Ecossistema de produtos digitais e páginas de venda.",
    url: "https://www.eduzz.com/",
    useCase: "Identificar variações de proposta de valor e diferenciais.",
  },
  {
    name: "Google Trends",
    category: "Demanda",
    description: "Tendência de interesse por termos ao longo do tempo.",
    url: "https://trends.google.com/",
    useCase: "Priorizar temas com demanda crescente para novas coletas.",
  },
  {
    name: "ClickBank Marketplace",
    category: "Produtos digitais internacionais",
    description: "Marketplace global com ofertas de afiliados em diversos nichos.",
    url: "https://www.clickbank.com/marketplace/",
    useCase: "Mapear promessas e estruturas de ofertas internacionais validadas.",
  },
  {
    name: "Digistore24 Marketplace",
    category: "Produtos digitais internacionais",
    description: "Plataforma internacional com produtos digitais e físicos por categoria.",
    url: "https://www.digistore24.com/en/home",
    useCase: "Comparar copy e posicionamento de ofertas em mercados externos.",
  },
  {
    name: "JVZoo",
    category: "Produtos digitais internacionais",
    description: "Ecossistema focado em lançamentos e ofertas digitais internacionais.",
    url: "https://www.jvzoo.com/",
    useCase: "Identificar padrões de lançamento e ângulos de conversão globais.",
  },
];

const WEEKLY_HIGHLIGHTS: WeeklyHighlight[] = [
  {
    sourceName: "TikTok Creative Center",
    insight: "Aumento de criativos curtos com ganchos nos primeiros 3 segundos e CTA explícito.",
    action: "Priorizar coleta de anúncios com estrutura Hook → Prova rápida → CTA direto.",
  },
  {
    sourceName: "Google Trends",
    insight: "Termos de intenção prática seguem com crescimento estável em nichos de produtividade.",
    action: "Validar palavras-chave de demanda crescente antes de iniciar extrações longas.",
  },
  {
    sourceName: "Hotmart Marketplace",
    insight: "Ofertas com promessa de resultado em até 30 dias aparecem com maior recorrência.",
    action: "Mapear promessas temporais e cruzar com mecanismos/provas mais repetidos.",
  },
];

export default function MoisResearchSourcesPage() {
  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Locais de pesquisa MOIS</PageTitle>
          <p className="text-secondary mb-0">
            Lista de fontes para coletar referências de mercado antes da extração guiada.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5">Destaques da semana</h2>
          <p className="text-secondary mb-3">
            Resumo rápido dos sinais mais relevantes para orientar a priorização da coleta.
          </p>
          <div className="row g-3">
            {WEEKLY_HIGHLIGHTS.map((highlight) => (
              <div className="col-12 col-lg-4" key={highlight.sourceName}>
                <article className="h-100 border rounded-3 p-3 bg-light-subtle">
                  <h3 className="h6 mb-2">{highlight.sourceName}</h3>
                  <p className="small mb-2">
                    <strong>Sinal:</strong> {highlight.insight}
                  </p>
                  <p className="small mb-0">
                    <strong>Ação recomendada:</strong> {highlight.action}
                  </p>
                </article>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5">Fontes recomendadas</h2>
          <p className="text-secondary mb-3">
            Use estas fontes para identificar ofertas que já estão sendo veiculadas e validar padrões recorrentes.
          </p>
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Fonte</th>
                  <th>Categoria</th>
                  <th>Quando usar</th>
                  <th>Link</th>
                </tr>
              </thead>
              <tbody>
                {RESEARCH_SOURCES.map((source) => (
                  <tr key={source.name}>
                    <td>
                      <strong>{source.name}</strong>
                      <p className="mb-0 text-secondary small">{source.description}</p>
                    </td>
                    <td>{source.category}</td>
                    <td>{source.useCase}</td>
                    <td>
                      <a href={source.url} target="_blank" rel="noreferrer">
                        Abrir fonte
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}
