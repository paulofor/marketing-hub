# Metadados das jornadas

## Por que os metadados importam?
Metadados são campos chave-valor que complementam templates, passos e instâncias de jornada. Eles permitem parametrizar integrações, preservar contexto estratégico e enriquecer telemetria sem exigir alterações estruturais no modelo relacional. Todos os recursos da API expõem esse espaço flexível, garantindo que novas necessidades possam ser atendidas apenas configurando chaves adicionais.【F:docs/swagger/openapi.yaml†L1082-L1358】

### Camadas que armazenam metadados
| Camada | Onde vive | Para que serve |
| --- | --- | --- |
| **Template** | `JourneyTemplate.metadata` | Guardar diretrizes macro, tags técnicas ou integrações que valem para toda a jornada baseada no blueprint.【F:backend/ads-service/src/main/java/com/marketinghub/journey/model/JourneyTemplate.java†L31-L71】 |
| **Passo** | `JourneyStep.metadata` | Parametrizar o disparo do estímulo (ex.: template do e-mail, mensagem WhatsApp, IDs de campanha), definir nomes de eventos de telemetria e variáveis usadas pelo handler.【F:backend/ads-service/src/main/java/com/marketinghub/journey/model/JourneyStep.java†L33-L80】【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/SendGridEmailChannelHandler.java†L73-L125】【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/WhatsAppChannelHandler.java†L72-L133】【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/MetaAdsChannelHandler.java†L63-L111】【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/TelemetryService.java†L49-L185】 |
| **Jornada** | `Journey.metadata` | Registrar variantes táticas (ex.: segmento importado, promoção ativa), mapear integrações externas e habilitar toggles de execução por instância.【F:backend/ads-service/src/main/java/com/marketinghub/journey/model/Journey.java†L31-L79】【F:backend/ads-service/src/main/java/com/marketinghub/journey/service/JourneyService.java†L80-L175】 |
| **Eventos** | `EventLog.metadata` | Persistir dados ricos sobre respostas ou falhas, alimentando dashboards e regras de negócio posteriores.【F:backend/ads-service/src/main/java/com/marketinghub/journey/model/EventLog.java†L14-L55】【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/JourneyExecutionService.java†L182-L344】 |

## Boas práticas de modelagem
1. **Padronize prefixos** — Use nomes que indiquem o destino (`pixel.*`, `ga4.*`, `ads.*`) para facilitar filtros e debugging.
2. **Prefira strings curtas** — Como o armazenamento usa colunas `meta_value` (`VARCHAR`), textos sucintos evitam problemas de tamanho e facilitam comparações.【F:backend/ads-service/src/main/java/com/marketinghub/journey/model/JourneyTemplate.java†L53-L58】【F:backend/ads-service/src/main/java/com/marketinghub/journey/model/JourneyStep.java†L74-L79】【F:backend/ads-service/src/main/java/com/marketinghub/journey/model/Journey.java†L61-L70】
3. **Normalização sempre que salvar** — Os serviços duplicam os mapas recebidos para manter ordem determinística e impedir efeitos colaterais, então privilegie o uso de `LinkedHashMap` ao montar payloads.【F:backend/ads-service/src/main/java/com/marketinghub/journey/service/JourneyService.java†L84-L175】【F:backend/ads-service/src/main/java/com/marketinghub/journey/service/JourneyStepService.java†L65-L147】
4. **Documente chaves estáveis** — Registre em fichas de campanha ou runbooks quais chaves impactam cada integração para evitar regressões involuntárias.

## Exemplos práticos
### 1. E-mail transacional parametrizado por metadados
Um passo com estímulo `EMAIL` pode definir `templateId`, `subject` e `content` nos metadados. O handler do SendGrid injeta esses valores diretamente no payload, reaproveitando o corpo dinâmico enviado pelo contexto da atribuição.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/SendGridEmailChannelHandler.java†L73-L125】

```json
{
  "templateId": "d-1234567890",
  "subject": "[Lançamento] Seu acesso está liberado",
  "content": "Olá {{name}}, confira o material em anexo"
}
```

Benefícios:
- Atualizar rapidamente o template no SendGrid sem redeploy.
- Experimentar assuntos diferentes por jornada mantendo o mesmo passo.
- Registrar destinatário e template usado na telemetria de dispatch.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/SendGridEmailChannelHandler.java†L73-L125】【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/JourneyExecutionService.java†L233-L344】

### 2. Mensagem WhatsApp com fallback inteligente
Metadados como `templateName`, `templateLanguage` e `body` guiam o handler do WhatsApp. Quando o template aprovado está presente, o payload usa o formato de mensagem estruturada; caso contrário, ele busca o corpo texto dos metadados ou do contexto da lead, evitando falhas por falta de conteúdo.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/WhatsAppChannelHandler.java†L72-L133】

Exemplo de configuração:
```json
{
  "templateName": "aviso_pagamento",
  "templateLanguage": "pt_BR",
  "body": "Olá {{nome}}, confirmamos o recebimento do seu pagamento."
}
```

### 3. Jornadas com experimentos e segmentação externa
A própria jornada pode armazenar metadados como `segment.source`, `campaignCode` ou `experiment.variant`. Esses dados são normalizados pelo serviço ao criar/atualizar, mantendo ordem previsível para logs e auditoria.【F:backend/ads-service/src/main/java/com/marketinghub/journey/service/JourneyService.java†L80-L175】 Eles também ficam disponíveis para leitura via API e podem alimentar dashboards ou filtros internos.【F:docs/swagger/openapi.yaml†L1267-L1358】

### 4. Telemetria customizada por canal
Passos que disparam anúncios ou mensagens podem setar `pixelEvent` e `ga4Event` para substituir os nomes padrão enviados às plataformas de analytics. O serviço de telemetria consulta esses metadados antes de postar eventos no Meta Pixel e no GA4, garantindo alinhamento com a taxonomia analítica de cada squad.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/TelemetryService.java†L49-L185】

### 5. Ativações de mídia paga
Metadados em passos do tipo `AD` definem `campaignId`, `adsetId`, `creativeId`, `bidAmount` e status inicial. O handler de Meta Ads converte esses campos diretamente para o payload da API, registrando os IDs retornados para conciliação futura.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/channel/MetaAdsChannelHandler.java†L63-L111】

## Checklist ao criar novas chaves
- [ ] A chave será reaproveitada em outras jornadas? Se sim, documente no template.
- [ ] O handler consome corretamente o tipo de dado? Campos numéricos podem ser serializados como string, então converta no handler se necessário.
- [ ] É preciso expor a chave na interface administrativa? Verifique se os formulários do frontend já suportam a edição.
- [ ] A telemetria precisa registrar o valor? Considere adicionar a chave ao metadata dos eventos emitidos para manter rastreabilidade.【F:backend/ads-service/src/main/java/com/marketinghub/journey/execution/JourneyExecutionService.java†L182-L344】

Ao padronizar o uso de metadados, as squads ganham autonomia para iterar nas jornadas e integrações externas sem depender de alterações de schema ou deploys frequentes.
