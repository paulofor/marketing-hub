# Projeção de descoberta no contrato ativo de Plutus

Matriz definida em 18/10/2026 antes da correção e de seus testes. Capella, produto 7,
ciclo 12, experimento 105: decisão #3517 aceita descoberta limitada a R$100/5 dias,
com 30% preservados para continuidade/escala; #3636 eleva somente a preparação a
US$15 cumulativos. A revisão 21 recebeu cobertura `COMPLETE_DETAILED`, mas Plutus87
retornou `ADJUST` por resultado BASE negativo e margem de 15,81%.

A comparação do histórico e do código confirmou a causa: a correção anterior incluiu
a exceção no núcleo e no template v1; `FinancialCodexRunner` executa o template v3,
cuja regra inicial continuava exigindo lucro BASE e contribuição após CAC positivos.
O teste anterior verificava a composição v3 sem exigir a exceção nesse caminho.
O gate Quartzo corretamente continua exigindo parecer próprio `APPROVE`.

| Alternativa | Benefício | Risco/esforço e decisão |
| --- | --- | --- |
| Alterar vendas, margem ou custos para tornar BASE rentável | Evitaria o bloqueio | Fabrica viabilidade e altera a decisão do usuário; rejeitada |
| Aceitar ADJUST no gate de publicação | Mudança pequena | Elimina revisão independente e mistura riscos; rejeitada |
| Transportar o escopo estruturado até o template efetivamente executado | Preserva decisões, números e gates | Mudança localizada no executor, com validação antes do modelo; escolhida |

| Caso | Aceite local |
| --- | --- |
| Descoberta com BASE negativo | Escopo do backend seleciona LIMITED_DISCOVERY no prompt v3, antes dos dados |
| Outra identidade | Mesmo contrato funciona com produto/ciclo/experimento diferentes, sem regra por nome ou ID |
| Texto sem aceite | Observação livre não seleciona descoberta nem dispensa rentabilidade normal |
| Contrato malformado | Aceite inválido não inicia o processo do modelo |
| Caminho rentável | ECONOMIC_VIABILITY preserva a exigência de margem, contribuição e resultado BASE |
| Transporte e auditoria | Runner real com Codex simulado preserva schema v3, prompt completo, resposta, tokens e decisão |
| Parecer desfavorável | ADJUST e BLOCKED não são promovidos a APPROVE pelo executor |
| Limites | Cobertura completa e entrega positiva antes do CAC continuam exigidas; não amplia teto, prazo ou autorização |

Os testes locais usam o simulador Codex existente e dados sintéticos, sem inferência,
campanha ou compra. Eles comprovam seleção, transporte e validação do contrato; não
garantem a decisão do modelo nem aumento de vendas. A revisão independente real só
será retomada após a publicação, dentro do saldo da autorização já existente.

O parecer86 também identificou reserva acumulada misturada a investimento novo nos
dados de Capella. A revisão21 separou consumo conhecido e saldo futuro: US$9,6222068
estimados após86 e até R$32,26 de investimento futuro na convenção R$6/US$. O custo
de87, US$0,1387412, reduz novamente o saldo; não é recibo faturado conciliado. Essa
correção de entrada não dispensa o contrato compartilhado nem altera o histórico.

## Resultado local

- A regressão do caminho real falhou antes da correção: a composição v3 não continha
  a exceção estruturada que já existia no núcleo/template v1.
- Suíte do executor: 82 casos, zero falhas/erros e nenhum teste omitido. Vinte casos
  verificam composição/escopo; o runner executa o simulador já existente, captura o
  schema v3 e preserva resposta, auditoria e tokens. Três identidades e decisões
  APPROVE/ADJUST/BLOCKED cobrem isolamento e independência.
- Doze entradas malformadas foram recusadas antes de iniciar o executável. Texto
  sem aceite, pedidos legados e uma projeção rentável mantêm o contrato normal.
- Não houve nova chamada paga, dado comercial ou alteração de frontend/backend
  principal nessa validação. Os custos dos pareceres86/87 permanecem no caso real.
