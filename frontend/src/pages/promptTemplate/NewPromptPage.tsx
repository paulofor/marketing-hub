import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useCreatePrompt } from "../../api/promptTemplate/useCreatePrompt";
import PageTitle from "../../components/PageTitle";
import { PROMPT_DOMAINS } from "../../constants/prompts";
import PromptForm, { PromptFormValues } from "./PromptForm";

const DEFAULT_TEMPLATE = `Gere ${'${quantity}'} descrições detalhadas em formato JSON.
Cada item deve conter as chaves: "title", "overview", "pains", "desires", "needs".
A chave overview deve ser um parágrafo único que explique dores, desejos e necessidades do público do nicho, pronto para ser reutilizado em outros prompts.
As chaves pains, desires e needs devem ser listas (arrays JSON) com frases diretas.
Use o seguinte nicho como contexto:
Nome: ${'${niche.name}'}
<#if niche.description?has_content>Descrição: ${'${niche.description}'}</#if>
<#if niche.baseSegmentation?has_content>Segmentação base: ${'${niche.baseSegmentation}'}</#if>
<#if niche.interests?has_content>Interesses: ${'${niche.interests}'}</#if>
<#if niche.demographicFilters?has_content>Filtros demográficos: ${'${niche.demographicFilters}'}</#if>
<#if niche.extraTips?has_content>Dicas extras: ${'${niche.extraTips}'}</#if>
Retorne apenas o array JSON com os objetos solicitados, sem texto adicional.`;

export default function NewPromptPage() {
  const navigate = useNavigate();
  const createPrompt = useCreatePrompt();

  async function handleSubmit(values: PromptFormValues) {
    await createPrompt.mutateAsync(values);
    toast.success("Prompt criado com sucesso");
    navigate("/prompts");
  }

  return (
    <div className="d-flex flex-column gap-3">
      <PageTitle>Novo prompt</PageTitle>
      <PromptForm
        initialValues={{
          name: "",
          domain: PROMPT_DOMAINS[0]?.value ?? "",
          template: DEFAULT_TEMPLATE,
          active: true,
        }}
        isSubmitting={createPrompt.isPending}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
