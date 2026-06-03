import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";

const htmlAcquisitionChecklist = [
  "URLs pendentes para captura",
  "Snapshots com HTML bruto salvo",
  "Falhas de acesso, bloqueio ou timeout",
  "Hash, tamanho e data da última captura",
];

export default function MoisSalesPagesPipelinePage() {
  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap align-items-start justify-content-between gap-3">
        <div>
          <PageTitle>Pipeline de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">
            Área reservada para construirmos juntos a visão de pipeline da
            biblioteca de páginas de vendas.
          </p>
        </div>
        <Link
          className="btn btn-outline-secondary"
          to="/mois/sales-pages-library"
        >
          Voltar à biblioteca
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-4">
          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <span className="badge text-bg-primary mb-2">Etapa 1</span>
              <h2 className="h4 mb-2">Obtenção dos HTML</h2>
              <p className="text-secondary mb-0">
                Primeiro bloco operacional do pipeline: transformar URLs
                ingeridas em snapshots brutos rastreáveis para permitir análise
                de copy, estrutura, prova, oferta e padrões visuais com base em
                evidência real.
              </p>
            </div>
            <span className="badge text-bg-warning align-self-start">
              Em implantação
            </span>
          </div>

          <div className="row g-3">
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Entrada
                </p>
                <h3 className="h6 mb-2">URLs normalizadas da biblioteca</h3>
                <p className="text-secondary small mb-0">
                  A etapa deve partir das páginas já ingeridas, priorizando URLs
                  sem snapshot ou com captura desatualizada.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Saída esperada
                </p>
                <h3 className="h6 mb-2">HTML bruto versionado</h3>
                <p className="text-secondary small mb-0">
                  Cada captura precisa gerar snapshot com hash, tamanho,
                  data/hora e status para auditoria e reprocessamento.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Critério de qualidade
                </p>
                <h3 className="h6 mb-2">HTML útil para análise</h3>
                <p className="text-secondary small mb-0">
                  O conteúdo deve conter corpo relevante da página, sem marcador
                  técnico interno nem payload contaminado.
                </p>
              </div>
            </div>
          </div>

          <div>
            <h3 className="h6 mb-2">
              Informações que este card deve acompanhar
            </h3>
            <ul className="mb-0 text-secondary">
              {htmlAcquisitionChecklist.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        </div>
      </section>
    </div>
  );
}
