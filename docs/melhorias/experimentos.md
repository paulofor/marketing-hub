# Melhorias de experimentos

## Referência de análise

Este documento consolida aprendizados e decisões de melhoria para experimentos de marketing digital do Marketing Hub.

### Fontes prioritárias

- Relatórios Markdown em `docs/relatorios/experimentos/`.
- Registros históricos em `docs/registros/experimentos.md` e `docs/registros/diario.md`.
- Métricas persistidas do funil, analytics e campanhas.
- Evidências de página, CTA, checkout, submissão e eventos públicos.

## Diagnóstico consolidado recente

Nos experimentos recentes, o gargalo principal apareceu depois do clique: alguns anúncios geraram CTR aceitável e clique barato, mas a passagem de visualização da página para intenção real de compra ou envio de formulário ficou muito baixa.

Leitura prática:

- O topo do funil já demonstrou capacidade de atrair atenção em alguns nichos.
- A página intermediária ainda precisa provar valor antes de pedir checkout.
- CTA frio direto para compra tende a desperdiçar tráfego quando a promessa exige demonstração visual ou personalização.
- Tracking incompleto reduz a capacidade de diferenciar problema de criativo, página, formulário ou checkout.

### Trava sistêmica contra compra direta com tráfego frio

Decisão de melhoria aplicada em 2026-07-04: todo experimento com intenção de compra deve passar por página de venda auditada antes de checkout. A regra vale para `LOW_TICKET_PRODUCT` e para qualquer campanha futura com `campaignObjective = SALES`.

Interpretação aplicada em 2026-07-05: Produto IA `AI_PERSONALIZED_SAMPLE` não precisa ter checkout real antes da página quando o primeiro passo comercial é a amostra personalizada. Nesse caso, o GeraSalesPage v1 usa o funil aprovado do Lead Portal como destino intermediário, publica a página dentro dele e só depois a campanha pode ser liberada.

Impacto esperado:

- impedir liberação manual de experimento que mande anúncio direto para checkout;
- impedir que a fila do Facebook Ads Worker receba experimentos irregulares;
- forçar prova de valor e coleta de métricas antes da intenção de compra;
- reduzir desperdício de mídia em tráfego frio sem contexto suficiente para compra.

## Experimento 55

### Problema identificado

O experimento 55 tinha campanha com entrega ativa no ecossistema Meta, mas sem clique e sem evento útil de funil. Na análise da página, o CTA principal podia apontar para a própria URL do fluxo, criando risco de loop em vez de conduzir claramente ao formulário.

### Decisão aplicada

Antes de escalar ou lançar o experimento 56, a prioridade passou a ser corrigir o fluxo do 55:

- transformar links autorreferentes da página em rolagem para o formulário;
- registrar `form_start` quando o usuário começa a preencher;
- registrar `form_submit` quando o formulário é enviado;
- manter `page_view` e `page_load_metric` como sinais de saúde da página;
- preservar o relatório do funil com dados persistidos, não apenas logs.

### Correção operacional complementar

Após a publicação da correção de CTA/tracking, o experimento 55 ficou `PAUSED` no Hub, mas a campanha Meta vinculada continuou ativa porque a pausa administrativa não criava solicitação operacional para o worker. A correção sistêmica é fazer `updateStatus(PAUSED)` registrar `stop_requested_at` nas campanhas Facebook vinculadas com motivo `ADMIN_EXPERIMENT_PAUSED`, mantendo o backend como fonte da decisão e o worker como executor da pausa na Meta.

Após o deploy dessa correção, a nova pausa expôs um bloqueio de schema: `facebook_ads_campaign.stop_reason` ainda estava como `ENUM` no banco real, rejeitando o motivo `ADMIN_EXPERIMENT_PAUSED`. O reparo necessário é garantir via Liquibase incremental que a coluna fique como `VARCHAR(100)`, porque motivos de parada são contrato operacional extensível e não devem depender de enum físico antigo em produção.

### Hipótese de melhoria

Se o visitante enxergar prova visual e for conduzido para uma ação de menor atrito antes do checkout, a perda entre visualização da página e intenção deve diminuir. O próximo julgamento do 55 deve separar:

- problema de anúncio, se continuar sem clique;
- problema de página/CTA, se houver clique mas não houver `form_start`;
- problema de formulário, se houver `form_start` mas não houver `form_submit`;
- problema de oferta/checkout, se houver submissão mas não houver avanço para compra.

## Próximos testes recomendados

- Manter orçamento baixo até haver clique e eventos úteis.
- Usar CTAs de menor atrito:
  - "ver exemplo preenchido";
  - "gerar minha amostra";
  - "ver antes/depois".
- Colocar prova visual antes do checkout.
- Só levar ao checkout depois de demonstrar valor concreto.
- Não lançar o experimento 56 até validar que o gargalo de página/formulário do 55 foi corrigido.
