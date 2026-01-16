import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useCreatePrompt } from "../../api/promptTemplate/useCreatePrompt";
import PageTitle from "../../components/PageTitle";
import { PROMPT_DOMAINS } from "../../constants/prompts";
import PromptForm, { PromptFormValues } from "./PromptForm";

const DEFAULT_PROMPT_TEMPLATES: Record<string, string> = {
  NICHE_DETAILED_DESCRIPTION: `Gere ${'${quantity}'} descrições detalhadas em formato JSON.
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
Retorne apenas o array JSON com os objetos solicitados, sem texto adicional.`,
  NICHE_HYPOTHESIS: `Gere ${'${quantity}'} hipóteses em formato JSON.
Use o seguinte nicho como contexto:
Nome: ${'${niche.name}'}
<#if niche.description?has_content>Descrição: ${'${niche.description}'}</#if>
<#if niche.baseSegmentation?has_content>Segmentação base: ${'${niche.baseSegmentation}'}</#if>
<#if niche.interests?has_content>Interesses: ${'${niche.interests}'}</#if>
<#if niche.demographicFilters?has_content>Filtros demográficos: ${'${niche.demographicFilters}'}</#if>
<#if niche.extraTips?has_content>Dicas extras: ${'${niche.extraTips}'}</#if>
<#if detailedDescription?has_content>
Use a descrição detalhada selecionada como referência:
<#if detailedDescription.title?has_content>Título: ${'${detailedDescription.title}'}</#if>
<#if detailedDescription.description?has_content>Descrição: ${'${detailedDescription.description}'}</#if>
<#if detailedDescription.pains?has_content>Dores: ${'${detailedDescription.pains}'}</#if>
<#if detailedDescription.desires?has_content>Desejos: ${'${detailedDescription.desires}'}</#if>
<#if detailedDescription.needs?has_content>Necessidades: ${'${detailedDescription.needs}'}</#if>
</#if>
<#if technology?has_content>
Considere a tecnologia diferenciada selecionada pelo usuário:
<#if technology.name?has_content>Nome: ${'${technology.name}'}</#if>
<#if technology.description?has_content>Descrição: ${'${technology.description}'}</#if>
<#if technology.promptText?has_content>Instruções adicionais: ${'${technology.promptText}'}</#if>
</#if>
<#if attributes?has_content>
Cada objeto deve conter as chaves: <#list attributes as attr>"${'${attr.name}'}"<#if attr_has_next>, </#if></#list>.
<#list attributes as attr>
Campo "${'${attr.name}'}": ${'${attr.description}'}.
</#list>
<#else>
Cada objeto deve conter as chaves: "title", "promise", "problem", "persona", "mechanism", "uniqueMechanism", "entrega", "successRule", "offerType", "price".
</#if>
O campo "offerType" deve ser "LEAD" ou "TRIPWIRE".
O campo "price" deve ser um número.
Retorne apenas um array JSON com esses objetos, sem texto adicional.`,
};

const DEFAULT_DOMAIN = PROMPT_DOMAINS[0]?.value ?? "";

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
          domain: DEFAULT_DOMAIN,
          template: DEFAULT_PROMPT_TEMPLATES[DEFAULT_DOMAIN] ?? "",
          active: true,
        }}
        defaultTemplates={DEFAULT_PROMPT_TEMPLATES}
        isSubmitting={createPrompt.isPending}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
