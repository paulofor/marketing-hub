# Pendências para campanha no Facebook Ads

O worker já consulta o backend por experimentos aprovados, cria a campanha com
objetivo `OUTCOME_TRAFFIC`, gera o ad set, criativo e anúncio correspondentes na
Graph API e registra a campanha no backend. Para completar o fluxo de veiculação
e governança, ainda faltam os itens abaixo, agrupados por tema.

## Planejamento e configuração

- Mapear o objetivo da campanha a partir dos dados do experimento, permitindo
  valores distintos de `OUTCOME_TRAFFIC`.
- Persistir modo de orçamento, orçamento diário e limites de gasto reais em vez
  de constantes.
- Suportar janelas de veiculação (datas de início/fim) e status da campanha
  (ex.: programada, ativa, pausada).
- Alimentar o backend com os identificadores de ad set, criativo e anúncio para
  manter rastreabilidade completa no modelo de dados.
- Versionar e auditar alterações na configuração do worker (`worker-config`),
  registrando quem atualizou parâmetros sensíveis (token, orçamento padrão,
  página fallback).

## Segmentação, criativos e lances

- Criar conjuntos de anúncios com segmentação derivada de localização,
  interesses, públicos semelhantes e demais filtros definidos no planejamento.
- Carregar e associar criativos aprovados (media assets, ad creatives e ads) às
  campanhas geradas, substituindo os placeholders atuais.
- Implementar configuração de posicionamentos, dispositivos e estratégia de
  lance/otimização.

## Rastreamento e conformidade

- Configurar eventos de conversão, Pixel e URL da landing page com
  call-to-action apropriado.
- Gerar parâmetros UTM padronizados (`utm_source`, `utm_medium`, `utm_campaign`,
  `utm_content`, `utm_term`) e persistir em `facebook_ads_ad_tracking_utm`.
- Validar conformidade com políticas do Facebook antes de criar ou publicar as
  campanhas.

## Métricas e monitoramento

- Utilizar `FacebookAdsService.getCampaignMetrics` para consultar métricas
  periódicas das campanhas.
- Persistir insights no backend para alimentar dashboards e alertas.
- Definir métricas de sucesso e plano de acompanhamento junto ao time de
  marketing.

## Robustez operacional

- Adicionar tratamento de erros detalhado para respostas da Graph API (limites
  de requisição, falhas de autenticação, validações).
- Registrar logs e métricas de execução do worker para observabilidade.
- Implementar reprocessamento idempotente e políticas de retry para evitar
  duplicação de campanhas.
