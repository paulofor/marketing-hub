import {
  DragDropContext,
  Draggable,
  Droppable,
  DropResult,
  DroppableProvided,
  DraggableProvided,
} from "react-beautiful-dnd";
import { useForm } from "react-hook-form";
import { useState } from "react";

/**
 * Builder UI for arranging funnel steps.
 */
interface Step {
  id: string;
  stimulus_type: string;
  score_inc: number;
  expected_action: string;
}

export default function FunnelBuilder() {
  const [steps, setSteps] = useState<Step[]>([]);
  const { register, handleSubmit } = useForm<Step>();

  const onDragEnd = (result: DropResult) => {
    if (!result.destination) return;
    const items = Array.from(steps);
    const [reordered] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, reordered);
    setSteps(items);
  };

  const onSubmit = (data: Step) => {
    setSteps([...steps, { ...data, id: Date.now().toString() }]);
  };

  return (
    <div>
      <form>
        <input {...register("stimulus_type", { required: true })} />
        <input type="number" {...register("score_inc", { min: 0 })} />
        <input {...register("expected_action", { required: true })} />
        <button
          type="button"
          onClick={handleSubmit(onSubmit, (errors) => {
            console.log("Validation errors", errors);
          })}
        >
          Add Step
        </button>
      </form>
      <DragDropContext onDragEnd={onDragEnd}>
        <Droppable droppableId="steps">
          {(provided: DroppableProvided) => (
            <ul ref={provided.innerRef} {...provided.droppableProps}>
              {steps.map((step, index) => (
                <Draggable key={step.id} draggableId={step.id} index={index}>
                  {(prov: DraggableProvided) => (
                    <li
                      ref={prov.innerRef}
                      {...prov.draggableProps}
                      {...prov.dragHandleProps}
                    >
                      {step.stimulus_type} - {step.expected_action} (+
                      {step.score_inc})
                    </li>
                  )}
                </Draggable>
              ))}
              {provided.placeholder}
            </ul>
          )}
        </Droppable>
      </DragDropContext>
    </div>
  );
}
