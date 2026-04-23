# Plano de Implementação — Avatar de Venda em Vídeo

- **Versão:** v2.0.0
- **Data de revisão:** 2026-03-24
- **Autor:** ChatGPT
- **Status:** draft
- **Modelo de execução:** backlog em sprints lógicos, sem duração fixa, para execução iterativa pelo Codex

---

## 1) Objetivo

Implementar um módulo de **Avatar de Venda em Vídeo** para gerar, revisar, renderizar, publicar e medir **vídeos curtos de benefício para landing pages**, usando o stack atual do Marketing Hub e uma arquitetura extensível para múltiplos provedores de geração de vídeo.

O objetivo do módulo é transformar dados da oferta em ativos de vídeo prontos para uso comercial, com foco em:

- explicar benefícios do produto digital com clareza;
- reduzir objeções antes do clique de compra;
- publicar vídeos leves e utilizáveis na landing page;
- permitir troca de fornecedor de vídeo sem reescrever a orquestração principal.

---

## 2) Decisões arquiteturais

### 2.1. O Worker IA continua como orquestrador

O `ai-worker` **não deve integrar diretamente cada fornecedor de vídeo**. Ele continuará responsável por:

- identificar jobs pendentes;
- montar contexto da oferta;
- gerar script, storyboard e metadados com LLM;
- enviar pedidos ao módulo de provedores de vídeo;
- acompanhar status;
- persistir resultados no `ads-service`.

### 2.2. Novo módulo para provedores de vídeo

Será criado um módulo novo de integração, desacoplado da integração atual com OpenAI para texto.

Estrutura sugerida:

```text
ai-worker/
  video-providers/
    video-provider-core/
    video-provider-openai/
    video-provider-heygen/
    video-provider-runway/
    video-provider-replicate/
```

Esse módulo deverá encapsular autenticação, payloads, polling, webhooks, download de resultado e normalização de erros por fornecedor.

### 2.3. O backend é a fonte de verdade

O `backend/ads-service` será o sistema de registro oficial para:

- definição de perfil de vídeo por produto;
- scripts e storyboards aprovados;
- jobs de renderização;
- assets gerados;
- slots de publicação na landing;
- métricas de play, progresso e clique.

### 2.4. A landing não conhece o provedor

O frontend e a landing devem consumir apenas:

- `asset_id`;
- URL final do vídeo;
- poster;
- legenda;
- metadados de exibição;
- CTA associado.

A escolha de provedor deve ficar restrita ao backend/worker.

---

## 3) Contexto do projeto atual

Este plano parte das seguintes premissas já existentes no repositório:

- o `ai-worker` é um app Spring Boot separado do `ads-service` e executa rotinas assíncronas agendadas; 
- o worker atualmente usa clientes ChatGPT/OpenAI para enriquecimento e geração textual;
- o plano de `avatar` atual já prevê orquestração via Worker IA e controle de status;
- o sistema já possui `Product`, `Asset`, landing pages, experimentos e backend/frontend separados.

---

## 4) Escopo funcional

### 4.1. Casos de uso do MVP

O MVP cobre três tipos de vídeo por oferta:

1. **Vídeo principal da landing**  
   Explica problema, benefício principal, mecanismo e CTA.

2. **Vídeo de objeção**  
   Responde perguntas como “serve para mim?”, “vale o preço?” e “quanto tempo leva?”.

3. **Vídeo de credibilidade/prova**  
   Reforça confiança, prova, demonstração ou segurança da compra.

### 4.2. Fora do escopo do MVP

- vídeo em tempo real por visitante;
- conversa síncrona com avatar;
- edição manual avançada dentro da plataforma;
- múltiplos avatares falando ao mesmo tempo;
- lip sync customizado por visitante;
- editor de timeline estilo NLE.

---

## 5) Arquitetura-alvo

## 5.1. Fluxo principal

```text
Product/Offer
  -> Sales Video Profile
  -> Script Generation
  -> Storyboard Generation
  -> Render Job
  -> Video Provider Module
  -> Asset Persistence
  -> Landing Video Slot
  -> Playback Analytics
```

## 5.2. Responsabilidades por camada

### `backend/ads-service`

Responsável por:

- CRUD de perfis de vídeo por produto;
- versionamento de script e storyboard;
- fila lógica de jobs de renderização;
- registro do provedor escolhido;
- publicação do asset na landing;
- ingestão de métricas.

### `ai-worker`

Responsável por:

- detectar jobs pendentes;
- gerar script base e variações;
- gerar storyboard estruturado;
- chamar `video-provider-core`;
- acompanhar processamento assíncrono;
- consolidar resultado;
- atualizar o backend.

### `video-provider-core`

Responsável por:

- contrato comum dos fornecedores;
- seleção do adapter correto;
- normalização de status;
- tratamento padrão de falhas;
- instrumentação comum;
- política de retry/backoff.

### `video-provider-*`

Responsável por:

- payload específico;
- autenticação específica;
- parsing de resposta;
- polling ou webhook;
- resolução de URL/arquivo final;
- mapeamento de erro específico.

### `frontend`

Responsável por:

- tela de configuração de vídeo por produto;
- preview de roteiro;
- aprovação do storyboard;
- publicação em slot da landing;
- visualização de status e métricas.

---

## 6) Contrato interno dos provedores

## 6.1. Interface sugerida

```java
public interface VideoGenerationProvider {
    ProviderSubmitResult submit(VideoGenerationRequest request);
    ProviderJobStatusResult getStatus(String providerJobId);
    ProviderDownloadResult download(String providerJobId);
    ProviderCancelResult cancel(String providerJobId);
    ProviderCapabilities capabilities();
}
```

## 6.2. Capacidades mínimas

```java
public record ProviderCapabilities(
    boolean supportsTalkingAvatar,
    boolean supportsImageInput,
    boolean supportsAudioInput,
    boolean supportsWebhook,
    boolean supportsPolling,
    boolean supportsPosterFrame,
    boolean supportsCaptionSeed,
    Integer maxDurationSeconds
) {}
```

## 6.3. Status interno padronizado

```text
QUEUED
PROCESSING
RENDERING
READY
FAILED
CANCELED
EXPIRED
```

## 6.4. Campos obrigatórios de rastreio

Todo job deve armazenar:

- `provider`
- `provider_job_id`
- `provider_model`
- `status`
- `progress`
- `failure_code`
- `failure_message`
- `output_url`
- `output_url_expires_at`
- `asset_id`
- `attempt_count`
- `last_polled_at`
- `webhook_received_at`

---

## 7) Modelo de dados proposto

## 7.1. `sales_video_profile`

Define como um produto deve virar vídeo.

Campos sugeridos:

- `id`
- `product_id`
- `name`
- `video_type` (`MAIN`, `OBJECTION`, `PROOF`)
- `persona_name`
- `persona_style`
- `language`
- `tone`
- `cta_text`
- `target_duration_seconds`
- `provider_preference`
- `status`
- `created_at`
- `updated_at`

## 7.2. `sales_video_script`

Script editorial gerado ou revisado.

Campos sugeridos:

- `id`
- `profile_id`
- `version`
- `source` (`AI`, `MANUAL`, `HYBRID`)
- `headline`
- `hook`
- `body_text`
- `cta_text`
- `status` (`DRAFT`, `APPROVED`, `REJECTED`)
- `approved_by`
- `created_at`
- `updated_at`

## 7.3. `sales_video_storyboard`

Representa as cenas a serem renderizadas.

Campos sugeridos:

- `id`
- `script_id`
- `version`
- `scene_json`
- `status`
- `created_at`
- `updated_at`

## 7.4. `sales_video_render_job`

Job técnico de geração.

Campos sugeridos:

- `id`
- `profile_id`
- `script_id`
- `storyboard_id`
- `provider`
- `provider_model`
- `provider_job_id`
- `status`
- `progress`
- `error_code`
- `error_message`
- `requested_at`
- `started_at`
- `finished_at`
- `output_url`
- `output_url_expires_at`
- `asset_id`
- `poster_asset_id`
- `caption_asset_id`

## 7.5. `landing_video_slot`

Liga o asset gerado à landing.

Campos sugeridos:

- `id`
- `landing_page_id`
- `profile_id`
- `asset_id`
- `poster_asset_id`
- `caption_asset_id`
- `slot_name`
- `position_order`
- `autoplay_enabled`
- `muted_by_default`
- `show_controls`
- `cta_text`
- `cta_url`
- `is_active`

## 7.6. `sales_video_event`

Telemetria de playback.

Campos sugeridos:

- `id`
- `landing_page_id`
- `profile_id`
- `session_id`
- `event_type`
- `video_second`
- `metadata_json`
- `created_at`

---

## 8) APIs propostas

## 8.1. Administração

### Criar perfil de vídeo

`POST /api/products/{productId}/sales-video-profiles`

### Listar perfis

`GET /api/products/{productId}/sales-video-profiles`

### Gerar script

`POST /api/sales-video-profiles/{profileId}/script:generate`

### Aprovar script

`POST /api/sales-video-scripts/{scriptId}/approve`

### Gerar storyboard

`POST /api/sales-video-scripts/{scriptId}/storyboard:generate`

### Aprovar storyboard

`POST /api/sales-video-storyboards/{storyboardId}/approve`

### Solicitar render

`POST /api/sales-video-storyboards/{storyboardId}/render`

### Consultar job

`GET /api/sales-video-render-jobs/{jobId}`

### Publicar na landing

`POST /api/landing-pages/{landingPageId}/video-slots`

## 8.2. Integrações internas do worker

### Reservar job pendente

`POST /api/internal/sales-video-render-jobs/{jobId}/claim`

### Atualizar status

`POST /api/internal/sales-video-render-jobs/{jobId}/status`

### Finalizar com asset

`POST /api/internal/sales-video-render-jobs/{jobId}/complete`

### Falhar job

`POST /api/internal/sales-video-render-jobs/{jobId}/fail`

## 8.3. APIs públicas da landing

### Carregar slots ativos

`GET /api/public/landing-pages/{landingPageId}/video-slots`

### Registrar evento de vídeo

`POST /api/public/video-events`

---

## 9) Regras de geração

## 9.1. Regras editoriais

Cada vídeo do MVP deve:

- focar em um único objetivo de conversão;
- explicar benefício antes de aprofundar detalhes;
- usar CTA claro e curto;
- evitar promessas absolutas;
- evitar claims que não possam ser sustentados pela oferta.

## 9.2. Regras técnicas

Cada render do MVP deve produzir:

- `mp4` como formato obrigatório;
- `webm` opcional quando viável;
- `poster` obrigatório;
- `WebVTT` obrigatório para legendas;
- duração curta por perfil;
- asset persistido internamente após geração.

## 9.3. Regras de publicação

Cada slot publicado na landing deve ter:

- poster configurado;
- fallback visual quando o vídeo não carregar;
- controles habilitados conforme o tipo de slot;
- legenda disponível;
- evento de analytics ativo.

---

## 10) Requisitos não funcionais

## 10.1. Performance de página

A landing deve evitar embeds pesados de terceiros como padrão. O player publicado deve priorizar asset próprio ou facade/light embed quando necessário.

## 10.2. Acessibilidade

Legendas automáticas podem ser usadas como ponto de partida, mas devem passar por revisão antes de publicação em produção.

## 10.3. Resiliência

Provedores podem operar com polling, webhook ou URLs temporárias. O sistema deve persistir o arquivo final assim que o resultado estiver pronto.

## 10.4. Observabilidade

Cada job deve emitir logs estruturados, progresso, falha, retries e provedor utilizado.

---

## 11) Backlog em sprints lógicos

> Observação: as sprints abaixo são **lotes de implementação**, não caixas de tempo. Cada sprint pode ser executada integralmente ou quebrada em subtarefas pelo Codex.

## Sprint 1 — Fundamentos do domínio

### Objetivo

Criar a base persistente e as APIs administrativas do módulo.

### Entregas

- criar entidades e migrations:
  - `sales_video_profile`
  - `sales_video_script`
  - `sales_video_storyboard`
  - `sales_video_render_job`
  - `landing_video_slot`
  - `sales_video_event`
- criar DTOs, repositories, services e controllers no `ads-service`;
- criar enums de domínio;
- criar validações de negócio;
- criar endpoints CRUD mínimos de perfil e consulta de jobs;
- criar testes de persistência e controller.

### Critério de aceite

É possível cadastrar um perfil de vídeo vinculado a um produto, gerar registros iniciais e consultar o estado do fluxo via API.

## Sprint 2 — Geração de script e storyboard

### Objetivo

Transformar dados do produto em roteiro e storyboard aprováveis.

### Entregas

- criar serviço de geração de script no `ai-worker`;
- reaproveitar integração textual atual com OpenAI para script/storyboard;
- criar endpoint de geração de script;
- criar endpoint de aprovação/rejeição;
- criar serviço de geração de storyboard estruturado em JSON;
- salvar versão do script e storyboard;
- registrar histórico de geração.

### Critério de aceite

É possível gerar script e storyboard de um perfil de vídeo, revisar o resultado e aprová-lo para renderização.

## Sprint 3 — Módulo `video-provider-core`

### Objetivo

Separar definitivamente orquestração de integração com provedores de vídeo.

### Entregas

- criar módulo `video-provider-core`;
- definir interface `VideoGenerationProvider`;
- definir `ProviderCapabilities`;
- criar modelos padronizados de request/response;
- criar normalizador de status;
- criar utilitários de retry/backoff;
- criar camada de seleção de provider;
- criar testes unitários do contrato.

### Critério de aceite

O `ai-worker` consegue pedir geração de vídeo via interface comum sem conhecer payloads específicos do fornecedor.

## Sprint 4 — Adapter `video-provider-openai`

### Objetivo

Entregar o primeiro provedor operacional usando a API de vídeo da OpenAI.

### Entregas

- criar adapter `video-provider-openai`;
- implementar `submit`, `getStatus`, `download`, `cancel`;
- mapear status do fornecedor para status interno;
- persistir `provider_job_id`, progresso e expiração do output;
- baixar o resultado final e anexar como asset interno;
- criar testes de integração simulada.

### Critério de aceite

Um storyboard aprovado consegue gerar um job, acompanhar progresso e produzir um asset persistido via adapter OpenAI.

## Sprint 5 — Adapter `video-provider-heygen`

### Objetivo

Adicionar suporte a avatar falante e fluxos baseados em foto/avatar.

### Entregas

- criar adapter `video-provider-heygen`;
- mapear fluxo assíncrono por `video_id`;
- suportar seleção de avatar/imagem quando aplicável;
- implementar download e persistência do arquivo final;
- criar fallback de erro específico do fornecedor;
- criar testes de contrato compartilhado.

### Critério de aceite

O sistema consegue escolher HeyGen para um perfil compatível e concluir a geração sem alterar o restante da orquestração.

## Sprint 6 — Tela administrativa no frontend

### Objetivo

Dar controle operacional ao produto/marketing dentro do painel.

### Entregas

- criar tela/listagem de perfis de vídeo por produto;
- criar formulário de criação/edição;
- criar preview de script e storyboard;
- criar ações de aprovar, rejeitar e renderizar;
- exibir progresso do job;
- exibir asset final e poster;
- exibir legenda associada.

### Critério de aceite

Um usuário interno consegue criar o perfil, revisar o conteúdo, disparar render e publicar o resultado sem usar Swagger.

## Sprint 7 — Publicação na landing page

### Objetivo

Colocar o vídeo no fluxo real de conversão.

### Entregas

- criar `landing_video_slot` no frontend/backend;
- implementar consumo dos slots na rota pública da landing;
- renderizar player HTML5 com poster;
- incluir legendas `WebVTT`;
- incluir fallback para indisponibilidade do vídeo;
- implementar CTA associado ao slot;
- garantir que a landing não dependa do nome do provedor.

### Critério de aceite

Uma landing consegue exibir o vídeo principal publicado, com poster, legenda e CTA funcionando.

## Sprint 8 — Analytics de vídeo

### Objetivo

Medir impacto real em engajamento e conversão.

### Entregas

- registrar eventos:
  - `VIDEO_IMPRESSION`
  - `VIDEO_PLAY`
  - `VIDEO_25`
  - `VIDEO_50`
  - `VIDEO_75`
  - `VIDEO_100`
  - `VIDEO_CTA_CLICK`
- expor consulta agregada por perfil/landing;
- ligar métricas de vídeo às métricas já existentes de produto/landing quando possível;
- criar dashboard básico no frontend.

### Critério de aceite

É possível comparar quais vídeos geram mais play, mais retenção e mais clique no CTA.

## Sprint 9 — Resiliência operacional

### Objetivo

Fortalecer o fluxo para produção contínua.

### Entregas

- retries controlados por tipo de erro;
- reprocessamento manual de jobs falhos;
- limpeza e expiração de URLs temporárias;
- persistência imediata do arquivo final;
- suporte a cancelamento;
- deduplicação por hash do payload;
- registro de auditoria técnica.

### Critério de aceite

Falhas temporárias de fornecedor não derrubam o fluxo e jobs podem ser reprocessados sem inconsistência.

## Sprint 10 — Provedores adicionais e roteamento

### Objetivo

Preparar escalabilidade de fornecedor.

### Entregas

- criar `video-provider-runway`;
- criar `video-provider-replicate` quando houver caso de uso claro;
- implementar política de seleção por capacidade;
- permitir fallback por prioridade de fornecedor;
- registrar custo e taxa de sucesso por provider;
- permitir override manual do provider por perfil.

### Critério de aceite

O sistema consegue selecionar ou trocar o fornecedor de vídeo com impacto mínimo no restante do código.

## Sprint 11 — Governança editorial

### Objetivo

Evitar publicações ruins ou arriscadas.

### Entregas

- workflow de aprovação de script;
- workflow de aprovação de storyboard;
- checklist pré-publicação;
- bloqueio para assets sem legenda revisada;
- bloqueio opcional para claims sensíveis;
- versionamento editorial completo.

### Critério de aceite

Nenhum vídeo entra em produção sem revisão mínima e histórico de aprovação.

## Sprint 12 — Otimização de conversão

### Objetivo

Transformar o módulo em ferramenta de melhoria contínua.

### Entregas

- suporte a múltiplas variações por perfil;
- associação com experimentos existentes;
- comparação de performance entre vídeos;
- recomendação de regravação por baixa retenção;
- geração de vídeos de objeção e prova a partir do mesmo produto.

### Critério de aceite

O time consegue operar o módulo como parte do ciclo de experimentação da landing.

---

## 12) Ordem recomendada de execução pelo Codex

Se a execução for totalmente guiada por Codex, a ordem recomendada é:

1. Sprint 1
2. Sprint 3
3. Sprint 2
4. Sprint 4
5. Sprint 6
6. Sprint 7
7. Sprint 8
8. Sprint 9
9. Sprint 5
10. Sprint 10
11. Sprint 11
12. Sprint 12

Essa ordem antecipa a separação arquitetural do módulo de provedores antes de expandir integrações.

---

## 13) Critérios de aceite do MVP

O MVP será considerado concluído quando existir:

- cadastro de perfil de vídeo vinculado a produto;
- geração de script e storyboard aprováveis;
- renderização assíncrona por pelo menos um provedor;
- asset final persistido no backend;
- publicação em slot da landing;
- player com poster e legenda;
- telemetria mínima de play e CTA.

---

## 14) Riscos principais

- acoplamento indevido do worker com APIs de fornecedor;
- expiração de URLs antes da persistência do asset;
- vídeos pesados prejudicando performance da landing;
- legenda automática publicada sem revisão;
- diversidade excessiva de provedores antes da estabilização do contrato comum.

---

## 15) Recomendações finais

1. Começar com **OpenAI ou HeyGen**, mas com contrato genérico desde o início.
2. Persistir todo output final em asset próprio assim que disponível.
3. Tratar **script e storyboard como artefatos versionados**, não como texto efêmero.
4. Publicar primeiro o **vídeo principal da landing** antes de expandir para objeção e prova.
5. Manter a landing desacoplada do fornecedor.

---

## 16) Referências usadas para esta versão

- Repositório do projeto e README principal.
- Documentação do `ai-worker` do projeto.
- Plano atual de implementação de avatar.
- OpenAI Videos API.
- HeyGen Video API.
- Runway SDK/API docs.
- Replicate Webhooks docs.
- web.dev sobre performance de vídeo.
- W3C WAI sobre legendas e revisão de captions automáticas.
