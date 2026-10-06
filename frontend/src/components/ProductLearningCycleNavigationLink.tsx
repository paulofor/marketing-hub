import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import type { ProductLearningCycleNavigation } from "../api/product/useProductValueChainPositions";
import "./ProductNextProcessLink.css";

/** Abre a passagem indicada pelo backend sem iniciar tarefas ou registrar decisões. */
export default function ProductLearningCycleNavigationLink({
  navigation,
}: {
  navigation: ProductLearningCycleNavigation;
}) {
  return (
    <div className="product-next-process">
      <span className="product-next-process__label">Análise e decisões</span>
      <strong className="product-next-process__title">
        Ciclos de aprendizado e vendas
      </strong>
      <Link
        className="btn btn-primary product-next-process__link"
        to={navigation.url}
      >
        Abrir ciclo e decisões
        <ArrowRight size={18} aria-hidden="true" />
      </Link>
      <small>
        Ciclo #{navigation.cycleId} · Experimento #{navigation.experimentId}
      </small>
      <small>{navigation.reason}</small>
    </div>
  );
}
