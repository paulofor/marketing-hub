import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import JourneyTemplateForm, {
  type JourneyTemplateFormSubmitPayload,
} from "./JourneyTemplateForm";
import { useJourneyTemplate } from "../../api/journey/useJourneyTemplate";
import { useUpdateJourneyTemplate } from "../../api/journey/useUpdateJourneyTemplate";
import { useDeleteJourneyStep } from "../../api/journey/useDeleteJourneyStep";
import { useUpdateJourneyStep } from "../../api/journey/useUpdateJourneyStep";
import { useCreateJourneyStep } from "../../api/journey/useCreateJourneyStep";
import "./JourneyPageShell.css";

export default function EditJourneyTemplatePage() {
  const params = useParams<{ id: string }>();
  const templateId = Number(params.id);
  const isValidId = Number.isInteger(templateId) && templateId > 0;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: template, isLoading, isError } = useJourneyTemplate(
    isValidId ? templateId : undefined,
  );
  const updateTemplate = useUpdateJourneyTemplate();
  const deleteStep = useDeleteJourneyStep();
  const updateStep = useUpdateJourneyStep();
  const createStep = useCreateJourneyStep();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async ({
    template: templatePayload,
    steps,
    removedStepIds,
  }: JourneyTemplateFormSubmitPayload) => {
    if (!isValidId) {
      return;
    }

    setIsSubmitting(true);
    try {
      await updateTemplate.mutateAsync({
        id: templateId,
        payload: templatePayload,
      });

      for (const stepId of removedStepIds) {
        await deleteStep.mutateAsync(stepId);
      }

      for (const step of steps) {
        const { id, ...stepPayload } = step;
        if (id) {
          await updateStep.mutateAsync({ id, payload: stepPayload });
        } else {
          await createStep.mutateAsync({
            templateId,
            payload: stepPayload,
          });
        }
      }

      await queryClient.invalidateQueries({
        queryKey: ["journey-template", templateId],
      });
      await queryClient.invalidateQueries({ queryKey: ["journey-templates"] });
      toast.success(`Template atualizado com sucesso.`);
      navigate(`/journey-templates/${templateId}`);
    } catch (error) {
      // handled by individual hooks
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isValidId) {
    return (
      <div className="journey-page-shell">
        <div className="journey-page-shell__state">
          <p>Template de jornada não encontrado.</p>
          <Link to="/journey-templates" className="btn btn-outline-primary">
            Voltar para templates
          </Link>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="journey-page-shell">
        <div className="journey-page-shell__state">Carregando template...</div>
      </div>
    );
  }

  if (isError || !template) {
    return (
      <div className="journey-page-shell">
        <div className="journey-page-shell__state journey-page-shell__state--error">
          <p>Não foi possível carregar o template solicitado.</p>
          <Link to="/journey-templates" className="btn btn-outline-primary">
            Tentar novamente
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="journey-page-shell">
      <header className="journey-page-shell__header">
        <PageTitle>Editar template de jornada</PageTitle>
        <p>
          Atualize objetivo, descrição e etapas para deixar o blueprint alinhado
          com sua operação.
        </p>
      </header>
      <JourneyTemplateForm
        initialTemplate={template}
        onSubmit={handleSubmit}
        isSubmitting={
          isSubmitting ||
          updateTemplate.isPending ||
          deleteStep.isPending ||
          updateStep.isPending ||
          createStep.isPending
        }
        submitLabel="Salvar alterações"
        onCancel={() => navigate(`/journey-templates/${templateId}`)}
      />
    </div>
  );
}
