# Referência de Prompts do ChatGPT

Este documento descreve como o Worker IA monta os prompts enviados ao ChatGPT para cada domínio suportado.

## Instant forms de experimentos

**Cliente responsável:** `ExperimentInstantFormChatGptClient`

### Mensagem do sistema

> Você é um especialista em Meta Ads focado em formulários instantâneos.

### Estrutura da mensagem do usuário

```text
Gere até {{quantity}} instant forms em português no formato JSON. Cada objeto deve conter as chaves "formId" (slug minúsculo com prefixo ai-form-), "name", "status" (draft, review ou approved), "locale" (pt_BR), "followUpActionUrl" e "privacyPolicyUrl". Retorne apenas um array JSON, sem texto adicional.

{{#if experiment.name}}
Experimento: {{experiment.name}}
{{/if}}
{{#if experiment.hypothesis}}
Resumo do experimento: {{experiment.hypothesis}}
{{/if}}
{{#if experiment.facebookPage.name}}
Página Meta: {{experiment.facebookPage.name}}
{{/if}}
{{#if experiment.facebookPage.pageId}}
ID da página: {{experiment.facebookPage.pageId}}
{{/if}}

{{#if hypothesis}}
{{#if hypothesis.title}}
Hipótese: {{hypothesis.title}}
{{/if}}
{{#if hypothesis.persona}}
Persona: {{hypothesis.persona}}
{{/if}}
{{#if hypothesis.problem}}
Problema: {{hypothesis.problem}}
{{/if}}
{{#if hypothesis.promise}}
Promessa: {{hypothesis.promise}}
{{/if}}
{{#if hypothesis.mechanism}}
Mecanismo: {{hypothesis.mechanism}}
{{/if}}
{{#if hypothesis.uniqueMechanism}}
Mecanismo único: {{hypothesis.uniqueMechanism}}
{{/if}}
{{/if}}

{{#if journey}}
{{#if journey.name}}
Jornada: {{journey.name}}
{{/if}}
{{#if journey.description}}
Descrição da jornada: {{journey.description}}
{{/if}}
{{#if journey.metadata}}
Metadados da jornada:
{{#each journey.metadata}}
- {{key}}: {{value}}
{{/each}}
{{/if}}
{{/if}}

{{#if stepContexts}}
Etapas que exigem instant form:
{{#each stepContexts}}
- Etapa {{positionOrId}}: {{nameOrSemNome}}
{{#if description}}
  Descrição: {{description}}
{{/if}}
{{#if metadata}}
  Metadados:
{{#each metadata}}
    - {{key}}: {{value}}
{{/each}}
{{/if}}
{{/each}}
{{/if}}

Projete formulários que coletem consentimento explícito, dados de contato e perguntas de qualificação alinhadas aos objetivos de cada etapa. Garanta coerência com a promessa e persona descritas.
Respeite o limite de caracteres e utilize URLs completas iniciando com https://.
```

### Observações

- `{{quantity}}` recebe o valor de `instantFormsToGenerate` definido no experimento.
- Os blocos condicionais (`{{#if ...}}`) só aparecem quando a informação correspondente está preenchida na entidade.
- `{{positionOrId}}` utiliza a posição da etapa na jornada; caso não exista, o identificador da etapa é usado como fallback.
- `{{nameOrSemNome}}` contém o nome da etapa e recorre ao texto "Sem nome" quando a etapa não possui título.
- `stepContexts` agrega as etapas do template de jornada que possuem tipo `INSTANT_FORM`, incluindo descrições e metadados relevantes.
- A resposta esperada do ChatGPT é exclusivamente um array JSON com os campos descritos no primeiro parágrafo do prompt.
