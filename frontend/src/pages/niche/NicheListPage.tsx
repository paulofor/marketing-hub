import { Link } from "react-router-dom";
import { useState } from "react";
import { useNicheSummary } from "../../api/niche/useNicheSummary";
import PageTitle from "../../components/PageTitle";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { useBreadcrumbs } from "../../app/breadcrumbs";

const PAGE_SIZE = 30;

export default function NicheListPage() {
  useBreadcrumbs([]);
  const [page, setPage] = useState(0);
  const { data, isLoading } = useNicheSummary(page, PAGE_SIZE);
  const niches = data?.items ?? [];
  const totalPages = data?.totalPages ?? 0;

  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle icon={nicheIcon}>Nichos de Mercado</PageTitle>
      <Link className="btn btn-primary mb-3" to="/niches/new">
        Novo Nicho
      </Link>
      {niches.length === 0 ? (
        <p>
          Nenhum nicho encontrado. <Link to="/niches/new">Crie um agora</Link>.
        </p>
      ) : (
        <>
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Hipóteses</th>
                  <th>Experimentos</th>
                  <th>Custo total</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {niches.map((n) => (
                  <NicheRow key={n.id} niche={n} />
                ))}
              </tbody>
            </table>
          </div>
          <PaginationControls
            page={data?.page ?? page}
            totalPages={totalPages}
            totalElements={data?.totalElements ?? niches.length}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}

function NicheRow({
  niche,
}: {
  niche: {
    id: number;
    name: string;
    enrichedNicheProfileId?: number | null;
    totalCost?: number | null;
    pipelineHypothesesCount: number;
    experimentsCount: number;
  };
}) {
  return (
    <tr>
      <td>{niche.name}</td>
      <td>{niche.pipelineHypothesesCount}</td>
      <td>{niche.experimentsCount}</td>
      <td>{formatCurrency(niche.totalCost)}</td>
      <td>
        <Link
          className="btn btn-sm btn-outline-primary me-1"
          to={`/niches/${niche.id}`}
        >
          Detalhes
        </Link>
        {niche.enrichedNicheProfileId ? (
          <Link
            className="btn btn-sm btn-outline-success me-1"
            to={`/oprm/enriched-niches/profile/${niche.enrichedNicheProfileId}`}
          >
            Nicho enriquecido
          </Link>
        ) : null}
        <Link
          className="btn btn-sm btn-outline-secondary"
          to={`/niches/${niche.id}/edit`}
        >
          Editar
        </Link>
      </td>
    </tr>
  );
}

function PaginationControls({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) {
    return (
      <p className="text-muted small">{totalElements} nichos encontrados.</p>
    );
  }

  return (
    <div className="d-flex align-items-center gap-2 mt-3">
      <button
        className="btn btn-outline-secondary btn-sm"
        type="button"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        Anterior
      </button>
      <span className="small text-muted">
        Página {page + 1} de {totalPages} · {totalElements} nichos
      </span>
      <button
        className="btn btn-outline-secondary btn-sm"
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Próxima
      </button>
    </div>
  );
}

function formatCurrency(value?: number | null) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value ?? 0);
}
