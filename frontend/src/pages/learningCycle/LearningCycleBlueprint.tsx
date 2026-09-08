import { Link } from "react-router-dom";
import { useCycleCatalog } from "../../api/learningCycle/useLearningCycles";
import LearningCycleDiagram from "./LearningCycleDiagram";

export default function LearningCycleBlueprint({
  chainId,
}: {
  chainId: number;
}) {
  const catalog = useCycleCatalog(chainId);
  return (
    <section className="card card-body mb-3">
      <h2 className="h5">Ciclos de aprendizado e vendas</h2>
      <p>
        Cada experimento preserva seu aprendizado e termina com uma decisão que
        orienta o próximo movimento do produto.
      </p>
      {catalog.isError ? (
        <div role="alert" className="alert alert-warning">
          Não foi possível carregar o BPM dos ciclos. Atualize após conferir a
          disponibilidade do backend.
        </div>
      ) : null}
      <Link
        className="btn btn-outline-primary align-self-start"
        to={`/business-process-chains/learning-cycles?chainId=${chainId}`}
      >
        Abrir ciclos dos produtos
      </Link>
      {catalog.data ? (
        <details className="mt-3">
          <summary>
            Ver BPM com decisões e retornos · v{catalog.data.version}
          </summary>
          <LearningCycleDiagram diagram={catalog.data.diagram} />
        </details>
      ) : null}
    </section>
  );
}
