# Referência de Prompts do ChatGPT

Este documento descreve como o Worker IA monta os prompts enviados ao ChatGPT para cada domínio suportado.

## Instant forms de experimentos

**Cliente responsável:** `ExperimentInstantFormChatGptClient`

### Mensagem do sistema

> Você é um especialista em Meta Ads focado em formulários instantâneos.

### Estrutura da mensagem do usuário

```text
Gere até {{quantity}} instant forms em português no formato JSON. Cada objeto do array deve conter apenas a chave "questions". Não crie nem atribua IDs — o Facebook cuidará dessa etapa. Retorne apenas um array JSON, sem texto adicional.

Em "questions", siga o formato aceito pela Graph API:
- "type": utilize valores suportados pela Meta (por exemplo, FULL_NAME, EMAIL, PHONE, CUSTOM);
- "key": identificador obrigatório para perguntas CUSTOM ou para diferenciar campos de telefone (ex.: "whatsapp");
- "label": enunciado criativo que estimule a resposta;
- "helper_text": instruções adicionais quando precisar orientar o preenchimento manual;
- "options": lista de objetos com "label" (e opcionalmente "value") para perguntas de múltipla escolha;
- demais campos auxiliares da Graph API, como "allow_multi_select" ou "required", podem ser incluídos quando necessário.

Garanta que a lista de perguntas inclua coleta explícita de nome completo (type FULL_NAME), e-mail (type EMAIL) e WhatsApp (type PHONE com instruções adequadas), além de questionamentos que reforcem consentimento e qualificação.

{{#if experiment.name}}
Experimento: {{experiment.name}}
{{/if}}
{{#if experiment.hypothesis}}
Resumo do experimento: {{experiment.hypothesis}}
{{/if}}
{{#if experiment.facebookPage.name}}
Página Meta: {{experiment.facebookPage.name}}
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

Projete perguntas que coletem consentimento explícito, reforcem a proposta de valor e mantenham coerência com a persona descrita. Respeite o limite de caracteres da Meta e utilize URLs completas iniciando com https://.
```

### Observações

- `{{quantity}}` recebe o valor de `instantFormsToGenerate` definido no experimento.
- Os blocos condicionais (`{{#if ...}}`) só aparecem quando a informação correspondente está preenchida na entidade.
- A resposta esperada do ChatGPT é exclusivamente um array JSON com os campos definidos na seção "Estrutura da mensagem do usuário".
