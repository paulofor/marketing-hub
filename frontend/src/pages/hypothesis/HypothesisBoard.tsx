import { useDroppable, useDraggable, DndContext, DragEndEvent } from "@dnd-kit/core";
import { restrictToHorizontalAxis } from "@dnd-kit/modifiers";
import { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";
import { useUpdateHypothesisStatus } from "../../api/hypothesis/useUpdateHypothesisStatus";
import { CSS } from "@dnd-kit/utilities";

interface ColumnProps {
  id: string;
  title: string;
  items: Hypothesis[];
  color: string;
}

function Column({ id, title, items, color }: ColumnProps) {
  const { setNodeRef } = useDroppable({ id });
  return (
    <div className="p-2 flex-grow-1" ref={setNodeRef} style={{ background: color, minHeight: 200 }}>
      <h5>{title}</h5>
      {items.map((h) => (
        <Card key={h.id} item={h} />
      ))}
    </div>
  );
}

function Card({ item }: { item: Hypothesis }) {
  const { attributes, listeners, setNodeRef, transform } = useDraggable({ id: item.id });
  const style = {
    transform: CSS.Translate.toString(transform),
    maxWidth: 280,
  };
  return (
    <div ref={setNodeRef} style={style} className="card mb-2" {...listeners} {...attributes}>
      <div className="card-body p-2">
        <div className="fw-bold text-truncate" style={{ WebkitLineClamp: 2, overflow: "hidden" }}>{item.title}</div>
      </div>
    </div>
  );
}

interface BoardProps {
  board: Record<string, Hypothesis[]>;
  experimentId: string;
}

export default function HypothesisBoard({ board, experimentId }: BoardProps) {
  const update = useUpdateHypothesisStatus(experimentId);
  const onDragEnd = (e: DragEndEvent) => {
    if (e.over && e.active.data.current) {
      const id = Number(e.active.id);
      const status = String(e.over.id);
      update.mutate({ id, status });
    }
  };
  return (
    <DndContext onDragEnd={onDragEnd} modifiers={[restrictToHorizontalAxis]}>
      <div className="d-flex gap-2">
        <Column id="BACKLOG" title="Backlog" color="#f1f5f9" items={board.BACKLOG || []} />
        <Column id="TESTING" title="Em Teste" color="#fee2e2" items={board.TESTING || []} />
        <Column id="VALIDATED" title="Validada" color="#dcfce7" items={board.VALIDATED || []} />
        <Column id="INVALIDATED" title="Invalidada" color="#f3f4f6" items={board.INVALIDATED || []} />
      </div>
    </DndContext>
  );
}
