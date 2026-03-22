# Funil de vendas por experimento

Esta tela ajuda a acompanhar todas as interações do funil de vendas de cada
experimento, da impressão do anúncio até o download do material pago.

## Onde acessar

1. Entre em **Experimentos** e abra o experimento desejado.
2. Clique na aba **Funil de vendas**.
3. A tabela mostra as nove etapas padronizadas, em ordem:
   1. Visualização do anúncio (impressões no Facebook Ads)
   2. Acesso ao formulário de lead (cliques no anúncio)
   3. Visualização do formulário (acessos registrados no Lead Portal)
   4. Envio do formulário (submissões do Lead Portal)
   5. Abertura do e-mail de amostra
   6. Acesso ao checkout no Mercado Pago
   7. Compra aprovada
   8. Abertura do e-mail de compra/entrega
   9. Download/visualização do material pago

Cada linha exibe números automáticos (coletados do banco), eventos manuais,
total, quantidade de registros únicos e a última atividade registrada.

## Registrando eventos manuais

Use o formulário logo abaixo da tabela para complementar ou corrigir o funil:

1. Escolha a etapa no seletor.
2. (Opcional) Informe o UUID da lead para deduplicação.
3. (Opcional) Preencha uma fonte (ex.: "manual", "validação" ou
   "integração externa").
4. (Opcional) Inclua um payload ou observação.
5. Clique em **Registrar evento**.

O funil é recalculado automaticamente e os totais são atualizados na tabela.

## O que abastece cada etapa automaticamente

- Impressões e cliques: tabela `experiment_campaign_metric` (Facebook Ads).
- Visualização/Envio do formulário: `flow_access` e `lead_portal_submission`,
  vinculados ao `lead_portal_flow.slug` do experimento.
- E-mail de amostra: `flow_submission_image_package.email_opened_at`.
- Checkout e compra: `lead_portal_purchase` (inclui aprovação/`mp_status`).
- E-mail de compra: `lead_portal_premium_delivery` associado ao `email_log`.
- Download pago: `flow_submission_image_package.images_viewed_at` com
  `payment_purchase_id`.

## Boas práticas

- Use o campo **Fonte** para diferenciar correções manuais de eventos vindos de
  outras integrações.
- Sempre que criar um checkout ou disparar e-mails fora do fluxo automatizado,
  registre o evento manual correspondente para manter o funil consistente.
- Verifique os números das primeiras etapas (impressões/cliques) antes de
  liberar novas campanhas: eles indicam se há entrega de mídia suficiente para
  alimentar o restante do funil.
- Ao clicar em **Liberar para o Facebook Ads Worker** na ficha do experimento, o sistema zera as etapas anteriores: use esse recurso somente quando terminar de testar, pois todo evento registrado antes da liberação deixa de ser contabilizado.
