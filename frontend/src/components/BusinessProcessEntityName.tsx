import { ClipboardList, Workflow } from "lucide-react";
import "./BusinessProcessEntityName.css";

type BusinessProcessEntityNameProps = {
  kind: "process" | "activity";
  name: string;
  iconSize?: number;
};

/** Identifica nomes de processos e atividades com a semântica visual canônica. */
export default function BusinessProcessEntityName({
  kind,
  name,
  iconSize,
}: BusinessProcessEntityNameProps) {
  const Icon = kind === "process" ? Workflow : ClipboardList;
  const label = kind === "process" ? "Processo" : "Atividade";

  return (
    <span
      className={`business-process-entity-name business-process-entity-name--${kind}`}
      title={label}
    >
      <Icon
        size={iconSize ?? (kind === "process" ? 20 : 18)}
        aria-hidden="true"
      />
      <span>{name}</span>
    </span>
  );
}
