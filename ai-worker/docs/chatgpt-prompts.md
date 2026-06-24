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

## Fluxos do portal do lead

**Status:** fluxo legado removido do AI Worker na fase 4; a reativação deve nascer por contrato HTTP do backend, sem repository/JPA no worker.

### Mensagem do sistema

> Você é um especialista em onboarding de leads.

### Estrutura da mensagem do usuário

```text
Gere até {{quantity}} fluxos para portal de leads em português no formato JSON. Cada item deve conter: "name", "slug", "description" e "questions".

Em "questions" retorne objetos com as chaves:
- "title": enunciado direto da pergunta;
- "dataKey": identificador curto em snake_case;
- "type": um dos valores TEXT, TEXTAREA, NUMBER, EMAIL, PHONE, DATE, SINGLE_CHOICE, MULTIPLE_CHOICE ou IMAGE_UPLOAD;
- "required": booleano indicando obrigatoriedade;
- "description" e "placeholder": textos auxiliares opcionais;
- "options": lista de respostas sugeridas (obrigatória para SINGLE_CHOICE e MULTIPLE_CHOICE).

Solicite perguntas simples que façam o lead refletir sobre o problema e o diagnóstico, usando opções de resposta realistas sempre que houver múltipla escolha. Inclua SEMPRE uma pergunta obrigatória do tipo EMAIL com `dataKey` igual a `email`, pois toda comunicação com o lead é feita por e-mail. Considere que o frontend envia o formulário via POST multipart (`FormData`) para `{{url}}` com envio assíncrono e feedback de sucesso/erro. Finalize SEMPRE cada fluxo com uma pergunta do tipo IMAGE_UPLOAD pedindo de forma objetiva uma foto nítida do empreendimento para criar materiais de divulgação e melhorias.

{{#if experiment.name}}
Experimento: {{experiment.name}}
{{/if}}
{{#if experiment.hypothesis}}
Resumo do experimento: {{experiment.hypothesis}}
{{/if}}
{{#if hypothesis.problem}}
Problema do lead: {{hypothesis.problem}}
{{/if}}
{{#if hypothesis.promise}}
Promessa da solução: {{hypothesis.promise}}
{{/if}}
{{#if hypothesis.persona}}
Persona: {{hypothesis.persona}}
{{/if}}

Responda apenas com um array JSON válido, sem comentários adicionais.
```
