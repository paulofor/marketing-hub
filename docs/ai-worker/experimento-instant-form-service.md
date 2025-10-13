# AI Worker - Serviço EXPERIMENTO para INSTANT FORMS

Este documento descreve como o Worker IA gera registros de **Instant Form** para experimentos.
O fluxo reaproveita a hipótese vinculada ao experimento e as etapas da jornada para produzir
formulários consistentes com o plano pós-clique.

## Visão Geral

1. Seleciona experimentos com `instantFormsToGenerate > 0` e página do Facebook configurada.
2. Consulta o `InstantFormChatGptClient`, que envia um resumo do experimento, da hipótese e das
   etapas relevantes da jornada para o modelo da OpenAI.
3. Para cada plano retornado pela IA, normaliza os campos (trim, remoção de opções vazias) e salva o
   resultado como **rascunho interno** no backend, preenchendo `model`, `prompt` e demais campos
   necessários para que o usuário visualize e aprove o conteúdo.
4. Mantém o contador `instantFormsToGenerate` até que exista ao menos um rascunho salvo, garantindo
   nova tentativa caso o modelo não gere planos válidos.
5. Após a aprovação manual, o **Facebook Ads Worker** cria o Instant Form na Graph API. O Worker IA não
   executa chamadas diretas para serviços da Meta — ele integra exclusivamente com APIs de
   Inteligência Artificial.

## Componentes

| Componente | Responsabilidade |
|------------|------------------|
| `ExperimentInstantFormScheduler` | Agenda a execução a cada cinco minutos (`0 */5 * * * *`). |
| `ExperimentInstantFormService` | Orquestra a busca dos experimentos, chama a IA e salva o rascunho para revisão humana. |
| `InstantFormChatGptClient` | Monta o prompt, envia para a API de Responses da OpenAI e interpreta o JSON retornado. |
| `FacebookWorkerConfigurationClient` | Reutiliza a configuração exposta pelo backend para carregar parâmetros de contexto usados no prompt. |

> **Regra de responsabilidade:** Somente o Facebook Ads Worker chama as APIs do Facebook.
> O Worker IA comunica-se exclusivamente com serviços de Inteligência Artificial.

## Pré-requisitos

- Experimentos devem possuir:
  - `hypothesisRef` carregada;
  - `facebookPage` associado (necessário para preencher `fb_instant_form.page_id`).
- O template de jornada não é obrigatório, mas se existir as etapas com estímulo `LANDING_PAGE` ou `INSTANT_FORM`
  serão usadas como contexto adicional no prompt.
- Configurar `OPENAI_API_KEY` (ou equivalente) para habilitar o cliente real. Sem a chave, o serviço apenas registra logs
  informando que a geração foi ignorada.
- O backend deve expor uma configuração válida em `/api/accounts/facebook/worker-config` para fornecer página e demais
  defaults utilizados na geração do prompt. Tokens de acesso à Meta são consumidos apenas pelo Facebook Ads Worker após a
  aprovação do usuário.

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
- Os logs concentram os prompts enviados, a resposta da IA e o identificador do experimento, permitindo acompanhar a decisão
  humana sem expor tokens ou detalhes sensíveis da Meta.

## Execução Local

```bash
cd ai-worker
mvn -s settings.xml package
mvn spring-boot:run
```

Durante a execução, o serviço aparecerá nos logs a cada cinco minutos. Para rodadas únicas é possível invocar
`ExperimentInstantFormService.generate()` via teste ou comando manual em um `CommandLineRunner`. A criação efetiva do
Instant Form na Graph API deve ser testada no contexto do Facebook Ads Worker, após a aprovação do usuário.
