# AI Worker - Serviços existentes

O Worker IA é um aplicativo Spring Boot separado do backend `ads-service`. Ele consome o mesmo
modelo de dados para executar rotinas assíncronas agendadas a cada cinco minutos (`0 */5 * * * *`).
Essas rotinas consultam o banco, acionam modelos de IA quando necessário e persistem os resultados
de volta no serviço principal.

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
  `CreativeImageClient` para sugerir imagens, salvando os criativos com `CreativeService`.
- **Referências:** detalhes e fluxo em [experimento-criativo-service.md](experimento-criativo-service.md).

### Experimento → Conjuntos de anúncios
- **Disparo:** `AudienceAdSetScheduler` (cron `0 */5 * * * *`).
- **Fonte dos dados:** experimentos aprovados para Facebook com `audienceApproved = true` e sem ad sets já cadastrados.
- **O que faz:** `AudienceAdSetService` coleta os públicos do nicho e da hipótese relacionados ao experimento,
  envia o contexto para o `AudienceAdSetChatGptClient` estruturar localização, interesses, lookalikes e `targetingJson`,
  e persiste os registros via `AdSetService` preenchendo também `prompt` e `model`.
- **Referências:** documentação complementar em [experimento-adset-service.md](experimento-adset-service.md).

## Perguntas frequentes

### Existe serviço para criar público?
Sim. Basta solicitar públicos para um nicho (`audiencesToGenerate`) e aguardar a execução do
`NicheAudienceScheduler`. O serviço usa ChatGPT para gerar os registros em `audience`, mantendo o
histórico do `prompt` e do `model` utilizado.
