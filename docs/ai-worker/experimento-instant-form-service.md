# AI Worker - Serviço EXPERIMENTO para INSTANT FORMS

Este documento descreve como o Worker IA gera registros de **Instant Form** para experimentos.
O fluxo reaproveita a hipótese vinculada ao experimento e as etapas da jornada para produzir
formulários consistentes com o plano pós-clique.

## Visão Geral

1. Seleciona experimentos com `instantFormsToGenerate > 0` e página do Facebook configurada.
2. Consulta o `InstantFormChatGptClient`, que envia um resumo do experimento, da hipótese e das
   etapas relevantes da jornada para o modelo da OpenAI.
3. Persiste os registros em `fb_instant_form`, preenchendo obrigatoriamente os campos `model` e `prompt`
   para garantir rastreabilidade.
4. Zera o contador `instantFormsToGenerate` para que o experimento não seja processado novamente.

## Componentes

| Componente | Responsabilidade |
|------------|------------------|
| `ExperimentInstantFormScheduler` | Agenda a execução a cada cinco minutos (`0 */5 * * * *`). |
| `ExperimentInstantFormService` | Orquestra a busca dos experimentos, chama a IA e salva os formulários. |
| `InstantFormChatGptClient` | Monta o prompt, envia para a API de Responses da OpenAI e interpreta o JSON retornado. |

## Pré-requisitos

- Experimentos devem possuir:
  - `hypothesisRef` carregada;
  - `facebookPage` associado (necessário para preencher `fb_instant_form.page_id`).
- O template de jornada não é obrigatório, mas se existir as etapas com estímulo `LANDING_PAGE` ou `INSTANT_FORM`
  serão usadas como contexto adicional no prompt.
- Configurar `OPENAI_API_KEY` (ou equivalente) para habilitar o cliente real. Sem a chave, o serviço apenas registra logs
  informando que a geração foi ignorada.

## Estrutura do Prompt

O prompt base inclui:

- Nome e hipótese do experimento (persona, problema, promessa, mecanismos etc.).
- Lista resumida das etapas da jornada que dependem de formulário, com metadados relevantes.
- Instruções para retornar um array JSON contendo nome, status, idioma, URLs pós-conversão, proposta de valor e lista
  de perguntas sugeridas.

A resposta é armazenada em `FacebookInstantForm.prompt` junto com o texto do prompt, permitindo auditoria.

## Auditoria e Rastreamento

- Cada registro salvo recebe `model` com o nome do modelo configurado (ex.: `o3`).
- O campo `prompt` recebe um bloco com o prompt enviado e a resposta integral do modelo.
- O log do serviço informa quantos registros foram criados para cada experimento e captura falhas inesperadas.

## Execução Local

```bash
cd ai-worker
mvn -s settings.xml package
mvn spring-boot:run
```

Durante a execução, o serviço aparecerá nos logs a cada cinco minutos. Para rodadas únicas é possível invocar
`ExperimentInstantFormService.generate()` via teste ou comando manual em um `CommandLineRunner`.
