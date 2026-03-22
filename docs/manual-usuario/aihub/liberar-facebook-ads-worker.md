# Liberar experimento para o Facebook Ads Worker

Use este fluxo quando o experimento já cumpriu todos os bloqueios de publicação
e está pronto para ser enviado ao Facebook Ads Worker.

## Pré-requisitos

- Criativos aprovados e prontos para uso.
- Ao menos um fluxo do Portal do Lead associado ao experimento.
- Segmentação completa (interesses, cargos e comportamentos aprovados).
- Configurações operacionais feitas: conta/página do Facebook, Instagram e
  orçamento diário.

## Passo a passo

1. Abra o experimento e localize o cartão **Campanha de Facebook Ads**.
2. Verifique se o alerta de bloqueios está vazio (status **Pronto**).
3. Clique em **Liberar para Facebook Ads Worker**.
4. Confirme a notificação de sucesso. A badge muda para Planejado e o funil é
   reiniciado automaticamente.

> **Importante:** o botão só é habilitado quando todos os bloqueios estiverem
> resolvidos. Ao liberar, todo evento do funil registrado antes da liberação é
> desconsiderado (tratado como teste) e o status do experimento volta para
> `Planejado`.

## O que acontece depois

- O campo `facebook_release_requested_at` é preenchido com a data/hora da
  liberação.
- O funil de vendas ignora eventos automáticos ou manuais ocorridos antes desse
  carimbo.
- O Facebook Ads Worker passa a enxergar o experimento no endpoint
  `/facebook-campaigns/experiments-ready` e cria as campanhas assim que houver
  capacidade.

## Repetindo a liberação

Caso precise resetar campanhas ou fazer novos testes:

1. Execute o reset normalmente.
2. Gere novos eventos de teste no funil, se necessário.
3. Clique novamente em **Liberar para Facebook Ads Worker**. Um novo carimbo é
   salvo e o funil volta para zero antes da segunda rodada de publicação.
