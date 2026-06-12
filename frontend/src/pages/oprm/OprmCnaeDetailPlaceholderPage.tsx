import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import { useBreadcrumbs } from "../../app/breadcrumbs";

export default function OprmCnaeDetailPlaceholderPage() {
  const { cnaeCode } = useParams();
  const decodedCnaeCode = cnaeCode ? decodeURIComponent(cnaeCode) : "CNAE";

  useBreadcrumbs([
    { label: "OPRM", to: "/oprm" },
    { label: "Detalhe do nicho" },
  ]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Detalhe do nicho CNAE</PageTitle>
        <p className="text-secondary mb-0">
          Esta tela será construída na próxima etapa para concentrar a visão do
          nicho, rotina, dores, oportunidades e próximos comandos do fluxo OPRM.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <span className="badge text-bg-primary mb-3">
            CNAE {decodedCnaeCode}
          </span>
          <h2 className="h5 mb-2">Detalhe em construção</h2>
          <p className="text-secondary mb-3">
            O link do nicho já está preparado para navegação. Quando a tela de
            detalhe for evoluída, ela deverá mostrar os dados mais importantes
            para decidir se o nicho merece pesquisa, oferta e experimento.
          </p>
          <Link className="btn btn-outline-secondary" to="/oprm">
            Voltar para CNAEs
          </Link>
        </div>
      </section>
    </div>
  );
}
