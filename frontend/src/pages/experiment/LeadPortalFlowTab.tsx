import axios from "axios";
import { useMemo, useState } from "react";
import type { Experiment } from "../../api/experiment/useExperiments";
import { useLeadPortalFlows } from "../../api/leadPortal/useLeadPortalFlows";
import { useUpdateLeadPortalFlowApproval } from "../../api/leadPortal/useUpdateLeadPortalFlowApproval";
import { useRequestLeadPortalFlows } from "../../api/experiment/useRequestLeadPortalFlows";
import { useUpdateExperiment } from "../../api/experiment/useUpdateExperiment";
import {
  type CreateLeadPortalFlowQuestionRequest,
  type LeadPortalQuestionType,
  useCreateLeadPortalFlow,
} from "../../api/leadPortal/useCreateLeadPortalFlow";
import WorkerRequestBanner from "./WorkerRequestBanner";

interface LeadPortalFlowTabProps {
  experiment: Experiment;
}

type FeedbackState = {
  variant: "success" | "error";
  message: string;
};

export default function LeadPortalFlowTab({ experiment }: LeadPortalFlowTabProps) {
  const { data: flows, isLoading, isError } = useLeadPortalFlows(experiment.id);
  const requestFlows = useRequestLeadPortalFlows(experiment.id);
  const updateExperiment = useUpdateExperiment(experiment.id);
  const createFlow = useCreateLeadPortalFlow();
  const updateApproval = useUpdateLeadPortalFlowApproval();
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [pendingAssignmentId, setPendingAssignmentId] = useState<number | null>(null);
  const [pendingApprovalId, setPendingApprovalId] = useState<number | null>(null);
  const [isCreateFormVisible, setIsCreateFormVisible] = useState(false);
  const [newFlowName, setNewFlowName] = useState("Formulário simples para personal trainer");
  const [newFlowSlug, setNewFlowSlug] = useState("formulario-simples-personal-trainer");
  const [newFlowDescription, setNewFlowDescription] = useState(
    "Fluxo simples para coleta inicial de informações sem necessidade de envio de imagens.",
  );
  const [manualQuestions, setManualQuestions] = useState<CreateLeadPortalFlowQuestionRequest[]>(() =>
    createSimpleFormTemplateQuestions(),
  );

  const sortedFlows = useMemo(() => {
    if (!Array.isArray(flows)) return [];
    return [...flows].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }, [flows]);

  const requestedCount = experiment.leadPortalFlowsToGenerate ?? 0;
  const assignedFlowId = experiment.leadPortalFlowId ?? null;

  const handleRequestFlows = async (quantity: number) => {
    await requestFlows.mutateAsync(quantity);
    setFeedback({
      variant: "success",
      message: quantity === 1
        ? "Solicitamos 1 fluxo do portal ao Worker IA."
        : `Solicitamos ${quantity} fluxos do portal ao Worker IA.`,
    });
  };

  const handleAssignFlow = async (flowId: number | null) => {
    const kpiTargetValue = experiment.kpiTarget ?? experiment.kpiTargetCpl;
    if (kpiTargetValue == null || experiment.metricPresetId == null) {
      setFeedback({
        variant: "error",
        message:
          "Defina a meta de KPI e o preset de métricas antes de vincular um fluxo do portal ao experimento.",
      });
      return;
    }
    setPendingAssignmentId(flowId ?? 0);
    setFeedback(null);
    try {
      await updateExperiment.mutateAsync({
        name: experiment.name,
        hypothesis: experiment.hypothesis,
        kpiTarget: Number(kpiTargetValue),
        metricPresetId: experiment.metricPresetId ?? undefined,
        sampleSize: experiment.sampleSize ?? undefined,
        mde: experiment.mdePercent ?? undefined,
        startDate: experiment.startDate ?? undefined,
        endDate: experiment.endDate ?? undefined,
        creativesToGenerate: experiment.creativesToGenerate ?? undefined,
        instantFormsToGenerate: experiment.instantFormsToGenerate ?? undefined,
        emailsToGenerate: experiment.emailsToGenerate ?? undefined,
        deliverablesToGenerate: experiment.deliverablesToGenerate ?? undefined,
        leadPortalFlowsToGenerate: experiment.leadPortalFlowsToGenerate ?? undefined,
        journeyTemplateId: experiment.journeyTemplateId ?? undefined,
        facebookPageId: experiment.facebookPage?.id ?? null,
        facebookInstantFormId: experiment.facebookInstantForm?.id ?? null,
        instagramAccountId: experiment.instagramAccount?.id ?? null,
        followUpActionUrl: experiment.followUpActionUrl ?? null,
        leadPortalFlowId: flowId,
      });
      setFeedback({
        variant: "success",
        message: flowId
          ? "Fluxo do portal vinculado ao experimento."
          : "O experimento não está mais associado a um fluxo do portal.",
      });
    } catch {
      setFeedback({
        variant: "error",
        message: "Não foi possível atualizar o experimento. Tente novamente em instantes.",
      });
    } finally {
      setPendingAssignmentId(null);
    }
  };

  const handleToggleApproval = async (flowId: number, approved: boolean) => {
    setPendingApprovalId(flowId);
    setFeedback(null);
    try {
      await updateApproval.mutateAsync({ id: flowId, approved });
    } catch {
      setFeedback({
        variant: "error",
        message: "Não foi possível atualizar a aprovação deste fluxo. Tente novamente.",
      });
    } finally {
      setPendingApprovalId(null);
    }
  };

  const handleFlowNameChange = (value: string) => {
    setNewFlowName(value);
    setNewFlowSlug(toSlug(value));
  };

  const handleQuestionChange = (
    index: number,
    key: keyof CreateLeadPortalFlowQuestionRequest,
    value: string | boolean,
  ) => {
    setManualQuestions((current) =>
      current.map((question, questionIndex) => {
        if (questionIndex !== index) return question;

        if (key === "title") {
          const title = String(value);
          return { ...question, title, dataKey: toDataKey(title) };
        }

        if (key === "type") {
          const nextType = value as LeadPortalQuestionType;
          const isChoiceType = nextType === "SINGLE_CHOICE" || nextType === "MULTIPLE_CHOICE";
          return {
            ...question,
            type: nextType,
            options: isChoiceType ? question.options ?? [] : undefined,
          };
        }

        return { ...question, [key]: value };
      }),
    );
  };

  const handleQuestionOptionsChange = (index: number, value: string) => {
    const options = value
      .split("\n")
      .map((option) => option.trim())
      .filter(Boolean);
    setManualQuestions((current) =>
      current.map((question, questionIndex) =>
        questionIndex === index ? { ...question, options } : question,
      ),
    );
  };

  const addQuestion = () => {
    setManualQuestions((current) => [
      ...current,
      {
        title: "Nova pergunta",
        dataKey: `campo_${current.length + 1}`,
        type: "TEXT",
        required: false,
        options: undefined,
      },
    ]);
  };

  const removeQuestion = (index: number) => {
    setManualQuestions((current) => current.filter((_, questionIndex) => questionIndex !== index));
  };

  const handleCreateSimpleFlow = async () => {
    if (manualQuestions.length === 0) {
      setFeedback({ variant: "error", message: "Adicione pelo menos uma pergunta para criar o fluxo." });
      return;
    }

    try {
      await createFlow.mutateAsync({
        name: newFlowName,
        slug: newFlowSlug,
        description: newFlowDescription,
        experimentId: experiment.id,
        model: "manual",
        questions: manualQuestions.map((question) => ({
          ...question,
          options:
            question.type === "SINGLE_CHOICE" || question.type === "MULTIPLE_CHOICE"
              ? question.options ?? []
              : undefined,
        })),
      });

      setFeedback({
        variant: "success",
        message: "Fluxo simples criado com sucesso. Agora você já pode vinculá-lo ao experimento.",
      });
      setIsCreateFormVisible(false);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? error.response?.data?.message ?? "Não foi possível criar o fluxo simples."
        : "Não foi possível criar o fluxo simples.";
      setFeedback({ variant: "error", message });
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <WorkerRequestBanner
        title="Fluxos de portal do lead"
        subtitle="Solicite sugestões de perguntas para o portal e vincule o fluxo ideal ao experimento."
        resourceName="fluxo"
        resourceNamePlural="fluxos"
        requestedCount={requestedCount}
        existingCount={sortedFlows.length}
        buttonLabel="Gerar fluxos"
        onRequest={handleRequestFlows}
        isRequesting={requestFlows.isPending}
        helperText="Os fluxos gerados ficam disponíveis para revisão e aprovação antes de serem publicados."
      />

      <div className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h5 className="mb-1">Criar formulário simples (sem imagem)</h5>
              <p className="text-muted small mb-0">
                Monte um fluxo manual para o portal com perguntas diretas, como nome, contato e tipo de aula.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-primary btn-sm"
              onClick={() => setIsCreateFormVisible((value) => !value)}
              disabled={createFlow.isPending}
            >
              {isCreateFormVisible ? "Fechar formulário" : "Novo formulário simples"}
            </button>
          </div>

          {isCreateFormVisible ? (
            <div className="d-flex flex-column gap-3">
              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label">Nome do fluxo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={newFlowName}
                    onChange={(event) => handleFlowNameChange(event.target.value)}
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Slug *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={newFlowSlug}
                    onChange={(event) => setNewFlowSlug(toSlug(event.target.value))}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Descrição</label>
                  <textarea
                    className="form-control"
                    rows={2}
                    value={newFlowDescription}
                    onChange={(event) => setNewFlowDescription(event.target.value)}
                  />
                </div>
              </div>

              <div className="d-flex flex-column gap-3">
                {manualQuestions.map((question, index) => {
                  const isChoiceType =
                    question.type === "SINGLE_CHOICE" || question.type === "MULTIPLE_CHOICE";
                  return (
                    <div key={`${question.dataKey}-${index}`} className="border rounded p-3">
                      <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                        <h6 className="mb-0">Pergunta {index + 1}</h6>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => removeQuestion(index)}
                          disabled={manualQuestions.length === 1 || createFlow.isPending}
                        >
                          Remover
                        </button>
                      </div>

                      <div className="row g-3">
                        <div className="col-md-6">
                          <label className="form-label">Título *</label>
                          <input
                            type="text"
                            className="form-control"
                            value={question.title}
                            onChange={(event) =>
                              handleQuestionChange(index, "title", event.target.value)
                            }
                          />
                        </div>
                        <div className="col-md-6">
                          <label className="form-label">Chave de dados *</label>
                          <input
                            type="text"
                            className="form-control"
                            value={question.dataKey}
                            onChange={(event) =>
                              handleQuestionChange(index, "dataKey", toDataKey(event.target.value))
                            }
                          />
                        </div>
                        <div className="col-md-4">
                          <label className="form-label">Tipo *</label>
                          <select
                            className="form-select"
                            value={question.type}
                            onChange={(event) => handleQuestionChange(index, "type", event.target.value)}
                          >
                            <option value="TEXT">Texto curto</option>
                            <option value="TEXTAREA">Texto longo</option>
                            <option value="PHONE">Telefone</option>
                            <option value="SINGLE_CHOICE">Escolha única</option>
                            <option value="MULTIPLE_CHOICE">Múltipla escolha</option>
                          </select>
                        </div>
                        <div className="col-md-8 d-flex align-items-end">
                          <div className="form-check">
                            <input
                              className="form-check-input"
                              type="checkbox"
                              id={`required-question-${index}`}
                              checked={question.required}
                              onChange={(event) =>
                                handleQuestionChange(index, "required", event.target.checked)
                              }
                            />
                            <label className="form-check-label" htmlFor={`required-question-${index}`}>
                              Pergunta obrigatória
                            </label>
                          </div>
                        </div>
                        {isChoiceType ? (
                          <div className="col-12">
                            <label className="form-label">Opções (uma por linha) *</label>
                            <textarea
                              className="form-control"
                              rows={4}
                              value={(question.options ?? []).join("\n")}
                              onChange={(event) =>
                                handleQuestionOptionsChange(index, event.target.value)
                              }
                            />
                          </div>
                        ) : null}
                      </div>
                    </div>
                  );
                })}

                <div className="d-flex flex-wrap gap-2">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={addQuestion}
                    disabled={createFlow.isPending}
                  >
                    Adicionar pergunta
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() => setManualQuestions(createSimpleFormTemplateQuestions())}
                    disabled={createFlow.isPending}
                  >
                    Restaurar exemplo
                  </button>
                </div>
              </div>

              <div className="d-flex justify-content-end">
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={handleCreateSimpleFlow}
                  disabled={createFlow.isPending}
                >
                  {createFlow.isPending ? (
                    <span className="spinner-border spinner-border-sm" role="status" />
                  ) : null}
                  {createFlow.isPending ? "Criando..." : "Criar formulário"}
                </button>
              </div>
            </div>
          ) : null}
        </div>
      </div>

      {feedback ? (
        <div
          className={`alert ${feedback.variant === "success" ? "alert-success" : "alert-danger"}`}
          role="alert"
        >
          {feedback.message}
        </div>
      ) : null}

      {isLoading ? (
        <p className="text-muted">Carregando fluxos do portal...</p>
      ) : isError ? (
        <p className="text-danger">Não foi possível carregar os fluxos disponíveis.</p>
      ) : sortedFlows.length === 0 ? (
        <p className="text-muted">Nenhum fluxo do portal disponível no momento.</p>
      ) : (
        <div className="d-flex flex-column gap-3">
          {sortedFlows.map((flow) => {
            const isSelected = assignedFlowId === flow.id;
            const isApproving = pendingApprovalId === flow.id && updateApproval.isPending;
            const isAssigning = pendingAssignmentId === flow.id || (pendingAssignmentId === 0 && flow.id === assignedFlowId);
            return (
              <div key={flow.id} className="card border-0 shadow-sm">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start gap-3">
                    <div>
                      <h5 className="card-title mb-1 d-flex align-items-center gap-2">
                        {flow.name}
                        {isSelected ? (
                          <span className="badge text-bg-primary">Selecionado</span>
                        ) : null}
                        {flow.approved ? (
                          <span className="badge text-bg-success">Aprovado</span>
                        ) : (
                          <span className="badge text-bg-secondary">Pendente</span>
                        )}
                      </h5>
                      <p className="text-muted small mb-0">Slug: {flow.slug}</p>
                      {flow.publicUrl ? (
                        <p className="text-muted small mt-2 mb-0">
                          URL pública:{" "}
                          <a href={flow.publicUrl} target="_blank" rel="noopener noreferrer">
                            {flow.publicUrl}
                          </a>
                        </p>
                      ) : null}
                      {flow.description ? (
                        <p className="text-muted small mt-2 mb-0">{flow.description}</p>
                      ) : null}
                    </div>
                    <div className="d-flex flex-column gap-2">
                      <button
                        type="button"
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() => handleToggleApproval(flow.id, !flow.approved)}
                        disabled={isApproving}
                      >
                        {isApproving ? (
                          <span className="spinner-border spinner-border-sm" role="status" />
                        ) : null}
                        {isApproving
                          ? "Atualizando..."
                          : flow.approved
                            ? "Revogar aprovação"
                            : "Aprovar"}
                      </button>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => handleAssignFlow(flow.id)}
                        disabled={isAssigning || updateExperiment.isPending}
                      >
                        {isAssigning || updateExperiment.isPending ? (
                          <span className="spinner-border spinner-border-sm" role="status" />
                        ) : null}
                        {isAssigning || updateExperiment.isPending
                          ? "Aplicando..."
                          : isSelected
                            ? "Fluxo selecionado"
                            : "Usar neste experimento"}
                      </button>
                    </div>
                  </div>

                  <div className="mt-3">
                    <h6 className="fw-semibold">Perguntas</h6>
                    <ol className="list-group list-group-numbered">
                      {flow.questions.map((question) => (
                        <li key={question.id} className="list-group-item">
                          <div className="d-flex justify-content-between align-items-start gap-3">
                            <div>
                              <div className="fw-semibold">{question.title}</div>
                              <div className="text-muted small">
                                Campo: {question.dataKey} · Tipo: {question.type.replace(/_/g, " ")}
                              </div>
                              {question.description ? (
                                <p className="text-muted small mb-0">{question.description}</p>
                              ) : null}
                              {question.options.length > 0 ? (
                                <ul className="text-muted small mb-0 mt-2 ps-3">
                                  {question.options.map((option) => (
                                    <li key={option}>{option}</li>
                                  ))}
                                </ul>
                              ) : null}
                            </div>
                            <span className="badge text-bg-light text-dark align-self-start">
                              {question.required ? "Obrigatória" : "Opcional"}
                            </span>
                          </div>
                        </li>
                      ))}
                    </ol>
                  </div>

                  <div className="mt-3 d-flex flex-wrap gap-3 text-muted small">
                    {flow.model ? <span>Modelo: {flow.model}</span> : null}
                    {flow.approvedAt ? <span>Aprovado em: {formatDate(flow.approvedAt)}</span> : null}
                    <span>Criado em: {formatDate(flow.createdAt)}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <div className="d-flex justify-content-end">
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          onClick={() => handleAssignFlow(null)}
          disabled={assignedFlowId == null || updateExperiment.isPending}
        >
          {updateExperiment.isPending ? (
            <span className="spinner-border spinner-border-sm" role="status" />
          ) : null}
          Remover fluxo do experimento
        </button>
      </div>
    </div>
  );
}

function createSimpleFormTemplateQuestions(): CreateLeadPortalFlowQuestionRequest[] {
  return [
    {
      title: "Nome",
      dataKey: "nome",
      type: "TEXT",
      required: true,
    },
    {
      title: "Trabalha em alguma academia ou studio? Qual nome?",
      dataKey: "academia_ou_studio",
      type: "TEXT",
      required: false,
    },
    {
      title: "Forma de contato",
      dataKey: "forma_contato",
      type: "SINGLE_CHOICE",
      required: true,
      options: ["Telefone", "WhatsApp", "Instagram"],
    },
    {
      title: "Qual é o número de telefone para contato?",
      dataKey: "telefone",
      type: "TEXT",
      required: false,
      description: "Preencha este campo quando a forma de contato escolhida for Telefone.",
    },
    {
      title: "Qual é o WhatsApp para contato?",
      dataKey: "whatsapp",
      type: "TEXT",
      required: false,
      description: "Preencha este campo quando a forma de contato escolhida for WhatsApp.",
    },
    {
      title: "Qual é o Instagram para contato?",
      dataKey: "instagram",
      type: "TEXT",
      required: false,
      description: "Preencha este campo quando a forma de contato escolhida for Instagram.",
    },
    {
      title: "Tipo de aulas que presta",
      dataKey: "tipo_aulas",
      type: "MULTIPLE_CHOICE",
      required: true,
      options: ["Musculação", "Yoga", "Outros"],
    },
    {
      title: "Se marcou outros, descreva quais aulas presta",
      dataKey: "outras_aulas",
      type: "TEXTAREA",
      required: false,
    },
  ];
}

function toSlug(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

function toDataKey(value: string) {
  const normalized = value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s_-]/g, "")
    .replace(/\s+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^-+|-+$/g, "");

  if (!normalized) {
    return "campo";
  }

  return /^[a-z]/.test(normalized) ? normalized : `campo_${normalized}`;
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
