# Checklist de publicação de experimento para campanha no Facebook Ads

Este checklist garante que um experimento só seja publicado quando tiver todos os
recursos essenciais configurados. Ele serve de base para o cartão "Campanha de
Facebook Ads" na tela de detalhes do experimento.

## 1. Bloqueios de publicação (diagnóstico do worker)

O worker de Facebook só publica se **todos os itens abaixo** estiverem prontos.
A regra está implementada em `ExperimentReadinessService` e abastece o alerta
cinza do diagnóstico.

1. **Criativos aprovados**
   - Pelo menos um criativo aprovado ou em produção no contexto do experimento.
   - Fonte de dados: tabela `creative` + flag `experiment.creative_approved`.
   - Ação recomendada: gerar/aprovar na aba _Criativos_.
2. **Fluxo do Portal do Lead**
   - O experimento precisa ter um fluxo associado (`lead_portal_flow`).
   - Fonte de dados: `lead_portal_flow` vinculado ao experimento.
   - Ação: criar ou associar na aba _Portal do Lead_.
3. **Público completo**
   - Necessário aprovar ao menos um interesse, um cargo e um comportamento (ou
     concluir o playbook de ad sets).
   - Fonte: elementos aprovados de segmentação (`targeting_element`).
   - Ação: aprovar itens na aba _Segmentação_.

Se algum dos itens acima estiver pendente, o worker interrompe a publicação e o
alerta apresenta a lista detalhada das inconsistências.

## 2. Configurações obrigatórias do experimento

Além dos bloqueios, o checklist também monitora as informações operacionais
previstas na rotina de publicação:

1. **Conta do Facebook Ads conectada** – precisa haver uma conta autorizada em
   _Contas do Facebook_ (dados expostos por `useFacebookConfigurationStatus`).
2. **Página do Facebook definida** – a página escolhida no experimento precisa
   existir e estar ativa no hub.
3. **Conta do Instagram vinculada** – usada para veicular os anúncios.
4. **Valor diário definido** – orçamento diário (`experiment.daily_budget`) que
   orienta a automação de mídia.

Esses itens não bloqueiam o worker sozinhos, mas ajudam o time a identificar se
há configuração faltante antes de liberar a campanha.

## 3. Como o checklist aparece na interface

- O cartão **Campanha de Facebook Ads** combina o alerta cinza (lógica do worker)
  com a visualização em lista.
- A primeira seção "Bloqueios de publicação" mostra exatamente os três
  critérios que travam o worker.
- A segunda seção "Configurações do experimento" exibe os itens operacionais
  acima (conta, página, Instagram e orçamento).
- Uma terceira seção "Fluxo operacional do Meta" relembra passos complementares
  (instant form, plataforma e status Planejado).

Quando todos os bloqueios estão prontos, o cartão sinaliza **Pronto** e o worker
pode publicar as campanhas para o experimento.

## 4. Liberação para o Facebook Ads Worker

1. Assim que os bloqueios forem resolvidos, o operador usa o botão **Liberar para o Facebook Ads Worker** na ficha do experimento.
2. A ação marca o status como `PLANNED`, preenche `facebook_release_requested_at` e zera o funil (eventos antes da liberação deixam de ser contabilizados).
3. O worker só consome `/api/facebook-campaigns/experiments-ready` para experimentos com status Planejado **e** liberação registrada; mudar o status manualmente não libera o job.
4. Sempre que for necessário reiniciar a operação (por exemplo, depois de um reset de campanhas), basta clicar novamente no botão para gerar um novo carimbo de liberação e limpar o funil de testes.

## Monitoramento do funil por experimento

Cada experimento agora possui uma aba **Funil de vendas** que expõe, em nove
etapas, o avanço desde a visualização do anúncio até o download do material
pago. Os números consolidados vêm das tabelas operacionais (impressões e cliques
em `experiment_campaign_metric`, engajamentos do Lead Portal e eventos de
checkout/pagamento). Consultar essa aba ajuda a validar se o experimento está
entregando tráfego antes de disparar a publicação ou ajustes adicionais.
