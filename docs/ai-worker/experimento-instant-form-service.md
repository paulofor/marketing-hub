# AI Worker - Serviço EXPERIMENTO para INSTANT FORMS

Este documento descreve como o Worker IA gera registros de **Instant Form** para experimentos.
O fluxo reaproveita a hipótese vinculada ao experimento e as etapas da jornada para produzir
formulários consistentes com o plano pós-clique.

## Visão Geral

1. Seleciona experimentos com `instantFormsToGenerate > 0` e página do Facebook configurada.
2. Consulta o `InstantFormChatGptClient`, que envia um resumo do experimento, da hipótese e das
   etapas relevantes da jornada para o modelo da OpenAI.
3. Para cada plano retornado pela IA, normaliza os campos (trim, remoção de opções vazias) e envia um
   `POST /{page-id}/leadgen_forms` para a Graph API usando o access token fornecido pelo backend.
   Em seguida ativa o formulário via `POST /{form-id}` (`status=ACTIVE`) e lê os metadados do
   formulário recém-criado (`GET /{form-id}?fields=...`).
4. Persiste os registros em `fb_instant_form` com o ID numérico retornado pela Meta, preenchendo os
   campos `model`, `prompt`, `status`, `createdTime`, `updatedTime` e `leadsCount` com os valores
   fornecidos pela Graph API.
5. Zera o contador `instantFormsToGenerate` apenas quando ao menos um formulário foi persistido, para
   permitir nova tentativa caso a criação no Facebook falhe.

## Componentes

| Componente | Responsabilidade |
|------------|------------------|
| `ExperimentInstantFormScheduler` | Agenda a execução a cada cinco minutos (`0 */5 * * * *`). |
| `ExperimentInstantFormService` | Orquestra a busca dos experimentos, chama a IA, cria o formulário na Graph API e salva o registro. |
| `InstantFormChatGptClient` | Monta o prompt, envia para a API de Responses da OpenAI e interpreta o JSON retornado. |
| `FacebookWorkerConfigurationClient` | Reutiliza a configuração exposta pelo backend para obter o access token e defaults do worker. |
| `FacebookLeadGenFormClient` | Encapsula as chamadas à Graph API para criação, ativação e leitura do Instant Form. |

## Pré-requisitos

- Experimentos devem possuir:
  - `hypothesisRef` carregada;
  - `facebookPage` associado (necessário para preencher `fb_instant_form.page_id`).
- O template de jornada não é obrigatório, mas se existir as etapas com estímulo `LANDING_PAGE` ou `INSTANT_FORM`
  serão usadas como contexto adicional no prompt.
- Configurar `OPENAI_API_KEY` (ou equivalente) para habilitar o cliente real. Sem a chave, o serviço apenas registra logs
  informando que a geração foi ignorada.
- O backend deve expor uma configuração válida em `/api/accounts/facebook/worker-config`, com token que possua os escopos
  `pages_manage_ads`, `ads_management` e acesso à página do experimento. Sem o token, o worker ignora o experimento e
  mantém `instantFormsToGenerate` para nova tentativa.
- As variáveis `FACEBOOK_GRAPH_API_BASE_URL` e `FACEBOOK_GRAPH_API_VERSION` permitem apontar o worker para ambientes de
  sandbox e ajustar a versão da API (padrão `https://graph.facebook.com/v23.0`).

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
- O log do serviço registra os caminhos da Graph API e payloads com o token mascarado, além do ID numérico retornado pelo
  Facebook para facilitar auditoria.

## Execução Local

```bash
cd ai-worker
mvn -s settings.xml package
mvn spring-boot:run
```

Durante a execução, o serviço aparecerá nos logs a cada cinco minutos. Para rodadas únicas é possível invocar
`ExperimentInstantFormService.generate()` via teste ou comando manual em um `CommandLineRunner`.
