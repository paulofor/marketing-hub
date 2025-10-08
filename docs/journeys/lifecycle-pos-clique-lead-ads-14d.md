# Jornada "Lifecycle Pós-Clique Lead Ads 14d"

Esta jornada inicial foi semeada para ilustrar como o Marketing Hub transforma um clique de Lead Ads em relacionamento multicanal ao longo de 14 dias. Ela combina passos automatizados de e-mail, push, WhatsApp e anúncios de retargeting, todos alinhados ao consentimento do lead e às metas de negócio.

## Objetivo estratégico

Converter a curiosidade gerada pelo anúncio em um relacionamento contínuo com canais próprios de baixo custo, garantindo mensuração completa e controle de frequência em todos os pontos de contato.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L8-L32】

### Metadados centrais

- **Mensuração avançada:** habilita Meta Conversions API e Google Enhanced Conversions para reconciliar eventos de clique com conversões downstream.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L24-L32】
- **Compliance:** registra consentimento granular LGPD para e-mail e WhatsApp, garantindo rastreabilidade do opt-in antes de qualquer estímulo ativo.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L24-L32】
- **Janela operacional:** plano de 14 dias com notas que orientam uso parcimonioso de canais pagos e priorizam canais próprios.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L24-L32】

## Passos da jornada

Cada passo reforça o storytelling do funil AIDA, respeitando atrasos mínimos e condições de entrada/saída.

1. **Clique no anúncio Lead Ads (Facebook)** — Gatilho inicial capturado via CAPI com UTMs e `click_id` para atribuição completa.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L34-L46】
2. **Captura de lead e consentimento** — Instant Form ou landing converte o clique em lead qualificado com consentimentos sincronizados no CDP/CRM.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L48-L59】
3. **Boas-vindas D0** — E-mail imediato entrega valor inicial, confirma opt-ins e sugere próximo passo, com push opcional.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L61-L70】
4. **Conteúdo educativo D1** — Nutrição com prova social após 24h para manter interesse e medir engajamento com KPIs de abertura/clique.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L72-L81】
5. **Nudge comportamental D3** — Web push reativa leads inativos 72h após captura, com mensagem curta de urgência leve.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L83-L92】
6. **Oferta leve D5** — E-mail com micro conversão para leads aquecidos, segmentando por engajamento prévio para manter relevância.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L94-L103】
7. **WhatsApp utilitário D5** — Template aprovado entrega conteúdo útil apenas para opt-ins WhatsApp engajados, controlando custos de conversa.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L105-L114】
8. **Retargeting Meta Ads D7-D14** — Sequência paga atinge leads engajados sem conversão, com criativos de prova social e orçamento otimizado para ROAS.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L116-L125】

## Instância de referência

A instância "Lifecycle Pós-Clique Lead Ads 14d - Exemplo" conecta-se ao segmento `leads_meta_click_to_wa`, ativa imediatamente após o seed e mantém metadados que documentam ownership, automações de CDP e KPIs de LTV/conversão em 14 dias.【F:backend/ads-service/src/main/resources/db/changelog/V2026_08_25__seed_facebook_post_click_journey.sql†L129-L154】

## Diagrama da jornada

```mermaid
gantt
    dateFormat  HH:mm
    title Timeline simplificada (referência relativa ao clique inicial)
    section Atenção
    Clique Lead Ads            :done,    a1, 00:00, 00:05
    section Interesse
    Captura & Consentimento    :done,    a2, 00:05, 00:30
    Boas-vindas D0             :active,  a3, 00:30, 04:00
    Conteúdo Educativo D1      :        a4, 24:00, 08:00
    section Desejo
    Nudge comportamental D3    :        a5, 72:00, 01:00
    Oferta leve D5             :        a6, 120:00, 02:00
    WhatsApp utilitário D5     :        a7, 120:30, 01:00
    section Ação
    Retargeting Meta Ads D7-D14:        a8, 168:00, 168:00
```

> **Como ler:** o gráfico considera o clique como tempo zero e mostra a sobreposição aproximada dos estímulos. A fase de retargeting estende-se até o limite de 14 dias, com pausa automática após a conversão ou término da janela.
