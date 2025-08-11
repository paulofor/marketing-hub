import {
  DragDropContext,
  Draggable,
  Droppable,
  DropResult,
  DroppableProvided,
  DraggableProvided,
} from "react-beautiful-dnd";
import { useForm } from "react-hook-form";
import { useState, useEffect } from "react";
import { useSaveFunnel } from "../api/funnel/useSaveFunnel";

/**
 * Builder UI for arranging funnel steps.
 */
interface Step {
  id: string;
  backendId?: string;
  stimulus_type: string;
  score_inc: number;
  expected_action: string;
  note?: string;
}

interface FunnelProps {
  funnel?: { id?: string; name: string; steps: Step[] };
}

export default function FunnelBuilder({ funnel }: FunnelProps) {
  const [steps, setSteps] = useState<Step[]>(funnel?.steps ?? []);
  const [name, setName] = useState(funnel?.name ?? "");

  useEffect(() => {
    setSteps(funnel?.steps ?? []);
    setName(funnel?.name ?? "");
  }, [funnel]);
  const { register, handleSubmit, reset } = useForm<Step>();
  const save = useSaveFunnel();

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
    "LANDING",
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
    setSteps([
      ...steps,
      {
        ...data,
        score_inc: data.score_inc ?? 0,
        id: Date.now().toString(),
      },
    ]);
    reset();
  };

  const updateStep = (index: number, updates: Partial<Step>) => {
    setSteps((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], ...updates };
      return next;
    });
  };

  const saveFunnel = () => {
    save.mutate({
      id: funnel?.id,
      name,
      steps: steps.map((s, index) => ({
        id: s.backendId,
        stimulusType: s.stimulus_type,
        expectedAction: s.expected_action,
        scoreInc: s.score_inc,
        orderIdx: index,
        note: s.note,
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
        <label htmlFor="note">Observation</label>
        <textarea
          id="note"
          rows={3}
          style={{ width: "100%" }}
          {...register("note")}
        />
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
                      <select
                        value={step.stimulus_type}
                        onChange={(e) =>
                          updateStep(index, { stimulus_type: e.target.value })
                        }
                      >
                        {stimulusOptions.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </select>
                      <input
                        type="number"
                        value={step.score_inc}
                        onChange={(e) =>
                          updateStep(index, {
                            score_inc: parseInt(e.target.value, 10) || 0,
                          })
                        }
                      />
                      <select
                        value={step.expected_action}
                        onChange={(e) =>
                          updateStep(index, { expected_action: e.target.value })
                        }
                      >
                        {actionOptions.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </select>
                      <textarea
                        rows={3}
                        style={{ width: "100%" }}
                        value={step.note ?? ""}
                        onChange={(e) =>
                          updateStep(index, { note: e.target.value })
                        }
                      />
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
