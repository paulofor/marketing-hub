import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import JourneyTemplateForm, {
  type JourneyTemplateFormSubmitPayload,
} from "./JourneyTemplateForm";
import { useCreateJourneyTemplate } from "../../api/journey/useCreateJourneyTemplate";
import { useCreateJourneyStep } from "../../api/journey/useCreateJourneyStep";
import "./JourneyPageShell.css";

export default function NewJourneyTemplatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const createTemplate = useCreateJourneyTemplate();
  const createStep = useCreateJourneyStep();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async ({
    template,
    steps,
  }: JourneyTemplateFormSubmitPayload) => {
    setIsSubmitting(true);
    try {
      const createdTemplate = await createTemplate.mutateAsync(template);

      for (const step of steps) {
        await createStep.mutateAsync({
          templateId: createdTemplate.id,
          payload: step,
        });
      }

      await queryClient.invalidateQueries({ queryKey: ["journey-templates"] });
      toast.success(`Template "${createdTemplate.name}" criado com sucesso.`);
      navigate("/journey-templates");
    } catch (error) {
      // As notificações de erro são tratadas pelos hooks de mutação.
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="journey-page-shell">
      <header className="journey-page-shell__header">
        <PageTitle>Criar template de jornada</PageTitle>
        <p>
          Padronize cadências multicanal definindo objetivo, fases e estímulos
          antes de replicar novas jornadas operacionais.
        </p>
      </header>
      <JourneyTemplateForm
        onSubmit={handleSubmit}
        isSubmitting={
          isSubmitting || createTemplate.isPending || createStep.isPending
        }
        submitLabel="Criar template"
        onCancel={() => navigate("/journey-templates")}
      />
    </div>
  );
}
