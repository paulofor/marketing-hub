# Produção autônoma de vídeo por Apolo e Plutus v1

## Objetivo

Apolo é o executor criativo dos vídeos do Estúdio. Plutus é o gate financeiro independente de cada ciclo. O backend é a fonte de verdade da fila, da decisão, do custo e do avanço.

## Fluxo canônico

1. Um usuário solicita pelo Estúdio um ciclo associado a produto, planejamento, projeto e perfil de vídeo.
2. O backend persiste o ciclo como `PENDING_FINANCIAL_REVIEW` e abre na mesa de Plutus uma tarefa de gate `VIDEO_BUDGET_APPROVAL`, com Apolo como solicitante.
3. Plutus decide pelo contrato formal do gate; alterar apenas o status da tarefa não libera provider nem consumo.
4. Nenhum job de provider existe antes da decisão financeira.
5. Somente a identidade técnica `financial-agent` pode registrar `APPROVED` ou `REJECTED`, sempre com motivo auditável.
6. Uma aprovação cria em modo `TEST` o job do executor oficial de vídeo, vinculado ao ciclo e ao teto em USD. Uma rejeição termina em `FINANCIAL_BLOCKED`.
7. Apolo planeja, gera, inspeciona e devolve o candidato. O provider não decide próxima etapa.
8. O custo conhecido deve ser conciliado no ledger. Novo consumo é bloqueado ao atingir o teto, quando o custo estiver desconhecido ou quando a cobertura financeira estiver incompleta.
9. QA independente decide qualidade. Apolo não aprova o próprio trabalho.

## Autoridade e segurança

- Aprovar um ciclo não autoriza publicação, campanha, mudança de preço ou compra de créditos.
- O metadata do job deve declarar `publicationAllowed: false`.
- Um teto é limite, nunca meta de gasto.
- Estimativas, jobs e aprovações não são vendas.
- A primeira operação do MUSA v7 permanece assistida e em `TEST` até comprovar custo, qualidade audiovisual, legibilidade mobile e callback completo.

## Homologação

A matriz cobre: caminho feliz, projeto sem perfil, decisão por identidade indevida, rejeição, decisão duplicada, provider indisponível, custo desconhecido, teto excedido, callback idempotente, QA independente, desktop, iPhone e Android. Providers reais não devem ser chamados na homologação local.
