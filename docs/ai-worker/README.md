# AI Worker - Serviços existentes

O Worker IA é um aplicativo Spring Boot separado do backend `ads-service`. Ele consome o mesmo
modelo de dados para executar rotinas assíncronas agendadas a cada cinco minutos (`0 */5 * * * *`).
Essas rotinas consultam o banco ou consomem endpoints REST expostos pelo backend, acionam modelos
de IA quando necessário e persistem os resultados de volta no serviço principal.

## Rotinas ativas

### Produto de Sucesso — enriquecimento de copy
- **Disparo:** `SuccessProductScheduler` roda a cada cinco minutos.
- **Fonte dos dados:** produtos com `novo = true` buscados por `WorkerSuccessProductRepository.findByNovoTrue()`.
- **O que faz:** `SuccessProductAnalyzer` envia a descrição para o `ChatGptClient` (implementado por
  `OpenAiChatGptClient` ou `DummyChatGptClient`) para preencher campos de copy, funil e links antes de
  gravar o produto novamente no banco.
- **Referências:** veja o diagrama em [class-diagram.md](class-diagram.md) e a implementação nas classes
  `SuccessProductAnalyzer`, `SuccessProductScheduler` e `OpenAiChatGptClient`.

### Produto de Sucesso → Nicho e Hipótese
- **Disparo:** `SuccessProductNicheHypothesisScheduler` com a mesma cadência de cinco minutos.
- **Fonte dos dados:** produtos com `generate_niche_hypothesis = true`.
- **O que faz:** `SuccessProductNicheHypothesisService` consulta o ChatGPT para extrair um novo nicho e
  uma hipótese a partir da descrição do produto, cria os registros correspondentes e desativa o
  sinalizador no produto.
- **Referências:** detalhes adicionais em
  [produto-sucesso-nicho-hypotese-service.md](produto-sucesso-nicho-hypotese-service.md).

### Nicho → Hipótese
- **Disparo:** `NicheHypothesisScheduler` (cron `0 */5 * * * *`).
- **Fonte dos dados:** nichos com `hypothesesToGenerate > 0`.
- **O que faz:** `NicheHypothesisService` gera a quantidade solicitada de hipóteses com apoio do
  `ChatGptClient` específico do domínio, valida os dados e zera o contador após salvar os registros.
- **Referências:** documentação complementar em [nicho-hypotese-service.md](nicho-hypotese-service.md).

### Nicho → Públicos
- **Disparo:** `NicheAudienceScheduler` (cron `0 */5 * * * *`).
- **Fonte dos dados:** nichos com `audiencesToGenerate > 0`.
- **O que faz:** `NicheAudienceService` coleta informações do nicho e de suas hipóteses relacionadas,
  envia o contexto para o `AudienceChatGptClient` gerar públicos via ChatGPT e persiste os registros
  através do `AudienceService`, preenchendo os campos `model` e `prompt` exigidos pela plataforma.
- **Referências:** detalhes adicionais em [nicho-publico-service.md](nicho-publico-service.md).

### Experimento → Criativos
- **Disparo:** `ExperimentCreativeScheduler` (cron `0 */5 * * * *`).
- **Fonte dos dados:** experimentos com `creativesToGenerate > 0`.
- **O que faz:** `ExperimentCreativeService` chama o `CreativeChatGptClient` para gerar textos e o
  `CreativeImageClient` para gerar as imagens. As imagens são enviadas para o backend com `POST /api/assets`
  via `BackendAssetClient`, incluindo os campos de `prompt` e `model` utilizados na geração, e os criativos são
  salvos com `CreativeService`.
  Caso o endpoint de upload esteja exposto em outro caminho ou domínio, utilize as variáveis
  `BACKEND_ASSET_PATH` ou `BACKEND_ASSET_URL` para ajustá-lo. O worker tenta repetir a chamada sem o
  prefixo configurado quando recebe `404`, evitando falhas em ambientes com roteamento diferente.
- **Referências:** detalhes e fluxo em [experimento-criativo-service.md](experimento-criativo-service.md).

### Experimento → E-mails da jornada
- **Disparo:** `ExperimentEmailScheduler` (cron `0 */5 * * * *`).
- **Fonte dos dados:** experimentos com `emailsToGenerate > 0` que já possuem jornada ativa construída a partir do template.
- **O que faz:** `ExperimentEmailService` reúne as etapas da jornada com estímulo `EMAIL`, envia o contexto para o
  `ExperimentEmailChatGptClient` e grava nos metadados da jornada os campos `subject`, `templateId`, `status`, `notes`,
  além de `model` e `prompt` para cada passo. O objetivo é liberar o time de CRM com linhas editoriais aprovadas e CTA
  recomendada.
- **Referências:** documentação em [experimento-email-service.md](experimento-email-service.md).

- **Disparo:** `AudienceAdSetScheduler` (cron `0 */5 * * * *`).
- **Fonte dos dados:** experimentos na plataforma Facebook com criativos aprovados e pelo menos um público aprovado,
  obtidos pelo `BackendExperimentClient` em `GET /api/facebook-adsets/experiments-ready`.
- **O que faz:** `AudienceAdSetService` filtra os públicos retornados pelo backend, verifica se já existem registros com
  `GET /api/adsets?experimentId=...`, envia o contexto para o `AudienceAdSetChatGptClient` estruturar localização,
  interesses, lookalikes e `targetingJson`, e persiste os registros com `POST /api/adsets`, preenchendo também `prompt`
  e `model`.
- **Referências:** documentação complementar em [experimento-adset-service.md](experimento-adset-service.md).

## Integrações descontinuadas

- **Instant Forms (Meta/Lead Ads):** o Worker IA não acessa mais a Graph API do Facebook. A criação, ativação e manutenção de
  formulários Instant Form deve ser realizada exclusivamente pelo `facebook-ads-worker` após a aprovação manual do usuário.
  Qualquer lógica anterior de geração automática foi removida para eliminar dependências diretas da plataforma Meta.

## Perguntas frequentes

### Existe serviço para criar público?
Sim. Basta solicitar públicos para um nicho (`audiencesToGenerate`) e aguardar a execução do
`NicheAudienceScheduler`. O serviço usa ChatGPT para gerar os registros em `audience`, mantendo o
histórico do `prompt` e do `model` utilizado.
