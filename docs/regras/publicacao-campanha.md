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
   - Agora é possível gerar até três anúncios diretamente a partir do pipeline (Texto do Anúncio + Prompt da Imagem). O botão **Gerar anúncios do pipeline** usa o Worker AI/gpt-image-1.5 e já deixa os criativos sinalizados como `DRAFT` para revisão.
   - Fonte de dados: tabela `creative` + flag `experiment.creative_approved`.
   - Ação recomendada: gerar/aprovar na aba _Criativos_.
   - Quando houver múltiplos criativos com status **READY**, o worker publica todos no mesmo conjunto de anúncios para preservar as variações aprovadas.
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
  (instant form, plataforma, status Planejado e o pixel automático do Facebook).
- O item "Pixel do Facebook" aparece nesse bloco para deixar claro se o worker já criou
  o identificador após a liberação. Enquanto isso não acontece, o checklist orienta o
  operador a aguardar a criação automática.

Quando todos os bloqueios estão prontos, o cartão sinaliza **Pronto** e o worker
pode publicar as campanhas para o experimento.

## 4. Liberação para o Facebook Ads Worker

1. Assim que os bloqueios forem resolvidos, o operador usa o botão **Liberar para o Facebook Ads Worker** na ficha do experimento.
2. A ação marca o status como `PLANNED`, preenche `facebook_release_requested_at` e zera o funil (eventos antes da liberação deixam de ser contabilizados).
3. O worker só consome `/api/facebook-campaigns/experiments-ready` para experimentos com status Planejado **e** liberação registrada; mudar o status manualmente não libera o job.
4. Sempre que for necessário reiniciar a operação (por exemplo, depois de um reset de campanhas), basta clicar novamente no botão para gerar um novo carimbo de liberação e limpar o funil de testes.
5. O campo `facebook_release_requested_at` agora permanece preenchido mesmo após o experimento mudar para RUNNING/PAUSED, garantindo que o funil continue filtrando os eventos coletados antes da última liberação. Ele só é atualizado quando o botão é acionado novamente.
6. A liberação também coloca o experimento na fila do worker de pixels. O pixel é criado automaticamente assim que o status fica `PLANNED`, por isso o botão não exige mais um pixel prévio: ele é o gatilho para a criação e para o preenchimento dos campos `facebook_pixel_id` e `facebook_pixel_code`.

## Monitoramento do funil por experimento

Cada experimento agora possui uma aba **Funil de vendas** que expõe, em nove
etapas, o avanço desde a visualização do anúncio até o download do material
pago. Os números consolidados vêm das tabelas operacionais (impressões e cliques
em `experiment_campaign_metric`, engajamentos do Lead Portal e eventos de
checkout/pagamento). Consultar essa aba ajuda a validar se o experimento está
entregando tráfego antes de disparar a publicação ou ajustes adicionais.

Além disso, o cartão do funil destaca o **total gasto na campanha** sincronizado
via Marketing API do Meta Ads e calcula automaticamente o **custo por conversão
por etapa** dividindo esse valor total pela quantidade de eventos consolidados
em cada linha. Isso facilita identificar gargalos (etapas caras) sem sair da
própria tela do experimento.


### Novos controles do funil

- Um botão **Zerar contagens** foi adicionado à aba Funil de vendas. Ele atualiza o campo `experiment.funnel_reset_at` e faz com que apenas eventos coletados após o reset sejam exibidos. Use-o quando o teste de anúncios ou tráfego interno poluir o funil sem precisar liberar novamente o worker.
- Cada anúncio listado no cartão **Execução registrada** agora mostra a referência de rastreio (valor usado nos parâmetros `campaign`/`utm_campaign`) e uma tabela com as conversões atribuídas às etapas 3 a 9 do funil. Isso facilita entender rapidamente qual anúncio sustentou as próximas conversões sem sair da ficha do experimento.

