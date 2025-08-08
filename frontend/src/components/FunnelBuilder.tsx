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
import { useCreateFunnel } from "../api/funnel/useCreateFunnel";

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
  const [name, setName] = useState("");
  const { register, handleSubmit } = useForm<Step>();
  const create = useCreateFunnel();

  const stimulusOptions = [
    "DM",
    "IG_POST_BOOST",
    "FB_AD",
    "WHATSAPP",
    "EMAIL",
    "SMS",
    "PUSH",
    "STORY",
    "WEBINAR",
    "CALL",
  ];

  const actionOptions = [
    "OPEN",
    "CLICK",
    "REPLY",
    "VIEW",
    "PURCHASE",
    "REGISTRATION",
    "OPT_IN",
    "OPT_OUT",
    "BOUNCE",
    "SHARE",
  ];

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

  const saveFunnel = () => {
    create.mutate({
      name,
      steps: steps.map((s) => ({
        stimulusType: s.stimulus_type,
        expectedAction: s.expected_action,
        scoreInc: s.score_inc,
      })),
    });
  };

  return (
    <div>
      <form>
        <label htmlFor="stimulus_type">Stimulus Type</label>
        <select
          id="stimulus_type"
          {...register("stimulus_type", { required: true })}
        >
          {stimulusOptions.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
        <label htmlFor="score_inc">Score Increment</label>
        <input
          id="score_inc"
          type="number"
          {...register("score_inc", { min: 0 })}
        />
        <label htmlFor="expected_action">Expected Action</label>
        <select
          id="expected_action"
          {...register("expected_action", { required: true })}
        >
          {actionOptions.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
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
      <label htmlFor="funnel_name">Funnel Name</label>
      <input
        id="funnel_name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <button type="button" onClick={saveFunnel}>
        Save Funnel
      </button>
    </div>
  );
}
