# AI Worker - Serviço EXPERIMENTO para E-MAILS

Este documento detalha o serviço responsável por gerar conteúdos de e-mail para as jornadas
associadas a experimentos.

## Visão Geral

1. Seleciona experimentos com `emailsToGenerate > 0` e jornada ativa criada a partir do template configurado.
2. Carrega as etapas da jornada com estímulo `EMAIL` e compila o contexto (hipótese, metadados do template e etapas).
3. Envia o material para o `ExperimentEmailChatGptClient`, que solicita ao modelo da OpenAI assuntos, CTA e resumo.
4. Atualiza os metadados da jornada com os campos gerados (`subject`, `templateId`, `status`, `notes`, `preheader`, `model`,
   `prompt`) e zera `emailsToGenerate`.

## Componentes

| Componente | Responsabilidade |
|------------|------------------|
| `ExperimentEmailScheduler` | Dispara a rotina a cada cinco minutos (`0 */5 * * * *`). |
| `ExperimentEmailService` | Consolida contexto, chama a IA e aplica o resultado na jornada. |
| `ExperimentEmailChatGptClient` | Monta o prompt e interpreta a resposta estruturada em JSON. |

## Pré-requisitos

- O experimento precisa de uma jornada ativa (criada via `ExperimentJourneyService`) associada ao template.
- O template deve conter pelo menos uma etapa com `stimulusType = EMAIL`.
- Definir `OPENAI_API_KEY` para habilitar o cliente real; sem a chave o worker registrará que a geração foi ignorada.

## Estrutura do Prompt

O prompt contém:

- Nome do experimento e resumo da hipótese (persona, problema, promessa, mecanismo etc.).
- Metadados da jornada (ex.: owner, janelas de medição) e das etapas de e-mail (posição, descrição, metadados).
- Instruções para produzir um array JSON com `stepId`, `subject`, `templateId`, `status`, `notes`, `callToAction` e `preheader`.

A resposta é armazenada em metadados individuais (`email.step.<ID>.prompt`), junto com o valor de `model` para cada passo.

## Metadados Gerados

Para cada etapa de e-mail são preenchidos os seguintes campos na jornada:

- `email.step.<ID>.subject`
- `email.step.<ID>.templateId`
- `email.step.<ID>.status` (normalizado para `draft`, `review` ou `approved`)
- `email.step.<ID>.notes` (inclui CTA recomendado quando fornecido)
- `email.step.<ID>.preheader` (quando presente)
- `email.step.<ID>.model`
- `email.step.<ID>.prompt`

Isso permite que o front-end exiba os conteúdos sugeridos e que o time de CRM acompanhe o histórico de geração.

## Execução Local

```bash
cd ai-worker
mvn -s settings.xml package
mvn spring-boot:run
```

A cada ciclo de cinco minutos o scheduler processará os experimentos pendentes. Para depuração pode-se invocar
`ExperimentEmailService.generate()` manualmente em um teste ou bean auxiliar.
