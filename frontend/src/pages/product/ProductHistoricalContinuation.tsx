import { Link } from "react-router-dom";
import type { ProductLearningCycleNavigation } from "../../api/product/useProductValueChainPositions";

/** Apresenta o destino oficial da continuidade sem reabrir a referência histórica. */
export default function ProductHistoricalContinuation({
  navigation,
  unavailable = false,
}: {
  navigation?: ProductLearningCycleNavigation | null;
  unavailable?: boolean;
}) {
  return (
    <aside
      className="alert alert-info mt-2"
      aria-label="Continuidade do produto"
    >
      <strong>Esta tentativa terminou. Não há ação sua nesta atividade.</strong>
      <p className="mt-2">
        As provas e pendências são históricas. A continuidade acontece em outra
        passagem.
      </p>
      {unavailable ? (
        <p role="alert">
          Não foi possível confirmar a continuidade. Atualize a consulta antes
          de agir.
        </p>
      ) : navigation ? (
        <>
          <p>{navigation.reason}</p>
          <Link className="btn btn-primary text-wrap" to={navigation.url}>
            Ver próximo passo · ciclo #{navigation.cycleId}
          </Link>
          <p className="small mt-2 mb-0">
            Experimento #{navigation.experimentId}. O ciclo mostra o trabalho
            dos responsáveis e as decisões necessárias; abrir a tela não
            autoriza gasto.
          </p>
        </>
      ) : (
        <p className="mb-0">
          O backend ainda não informou uma continuidade para esta cadeia.
          Nenhuma retomada está disponível nesta referência.
        </p>
      )}
    </aside>
  );
}
