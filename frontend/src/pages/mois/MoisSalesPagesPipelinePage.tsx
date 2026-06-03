import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";

export default function MoisSalesPagesPipelinePage() {
  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap align-items-start justify-content-between gap-3">
        <div>
          <PageTitle>Pipeline de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">
            Área reservada para construirmos juntos a visão de pipeline da biblioteca de páginas de vendas.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois/sales-pages-library">
          Voltar à biblioteca
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5">Tela em construção</h2>
          <p className="text-secondary mb-0">
            O botão Pipeline já direciona para esta rota. Os próximos blocos, fases e ações serão definidos na próxima etapa.
          </p>
        </div>
      </section>
    </div>
  );
}
