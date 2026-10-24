# Conciliação do checkout direto de Capella

## Evidência e causa

Durante a retomada da atividade 5.4.1, a conciliação chegou à decisão de Atena #6.
O parecer distinguiu quatro cliques no CTA de zero acessos na tabela de compras.
A inspeção confirmou o CTA do experimento 105 apontando diretamente à preferência
comercial do Mercado Pago. `CommercialProductCheckoutService` inclui produto e
experimento nos metadados; `CheckoutService.updateFromPayment` trata essa venda
fora de `lead_portal_purchase`. A auditoria é `mercadopago_webhook_log`, e a entrega
final do kit é `agenda_cheia_delivery`.

O histórico contém callbacks desse checkout em agosto, inclusive um teste nominal
real de R$ 0,67 sem atribuição a experimento. Isso confirma que consultar apenas a
tabela de pacotes não é fonte suficiente. Não houve callbacks do checkout de Capella
na janela de outubro consultada. A ausência registrada não comprova rejeição da oferta.
A hipótese de quatro abandonos não foi adotada: eram cliques, não quatro compras iniciadas.

## Alternativas

| Alternativa | Benefício | Risco e esforço |
|---|---|---|
| Continuar usando compras de pacotes | Sem mudança | O checkout direto permanece invisível |
| Conciliar os recibos existentes e a entrega final | Reutiliza auditoria, metadados e tabelas canônicas | Leitura adicional, com testes de atribuição e temporalidade |
| Criar outro ledger e reprocessar todos os webhooks | Novo contrato uniforme | Migração e duplicação desnecessárias para o defeito demonstrado |

Adotada a segunda alternativa. A seleção é pelo checkout explícito, sem exceção por
nome ou ID. O caminho legado de pacotes continua disponível quando não há checkout
comercial direto. Não somar as duas fontes. O relatório identifica a fonte financeira
e separa cliques do portal dos acessos ao checkout persistidos.

## Aceite e proteção contra recorrência

- Compra aprovada exige identidade de recibo, produto e experimento, ambiente real,
  moeda, preço e data com offset. Metadados ambíguos bloqueiam; não inferir pelo nome.
- Vários callbacks da mesma transação contam uma compra. O último recibo até o fim
  da janela governa o estado; falha posterior não pode reutilizar aprovação antiga.
- Outro produto/experimento, QA, sandbox e datas externas não compõem receita.
- Teste nominal real com preço divergente exige conciliação, sem fingir venda válida.
- Reembolso, valor parcial devolvido ou estado inválido bloqueiam até conciliação
  datada; não contar receita bruta como líquida nem descartar custo desconhecido.
- Entrega exige pacote final e data de entrega; e-mail inicial não comprova uso.
- A matriz usa H2 e MySQL 5.7 reais na sandbox, sem chamadas ao provedor nem SMTP real.
  Percorre o repositório SQL e o coletor de evidência, preservando testes anteriores.

A lacuna do harness era testar pagamentos apenas como pacotes. A regressão inicial
falhou nos dois experimentos sintéticos (`esperado 1, observado 0`); depois passou
pela fonte comercial real. A melhoria previne aprendizado baseado em receita omitida.
Não comprova aumento de vendas. STOP e autorizações de sucessores permanecem preservados.

## Orientação depois do parecer

A conferência em três dispositivos encontrou o formulário de aprovação de Atena
habilitado e, simultaneamente, o resumo dizendo que nenhuma decisão era necessária.
A projeção agora consulta a proposta persistida da revisão exata e reaproveita a
política preparatória existente: inconclusivo pronto exige decisão; ajuste elegível
não fabrica outra aprovação. Proposta antiga, aprovada, em execução ou ciclo encerrado
não é convertida em nova pendência. A leitura não grava, não chama modelos e não gasta.

A fixture de SQL foi conferida com o schema real. O preço usa `unit_price_brl`; a
coluna da fixture deriva da anotação canônica da entidade, evitando validar um nome
inventado apenas porque o teste construiu uma tabela com o mesmo erro.

## Resultado da validação

Rodada completa do backend sem falhas (5.257 casos reportados, 57 condicionais
não executados). Após os últimos ajustes, os 188 testes específicos de conciliação,
projeção, vídeos e arquitetura passaram. O cenário SQL passou também no MySQL 5.7
local em 44 casos. Os 16 testes existentes da tela de decisão e visão do ciclo passaram.
A consulta adicional de auditoria via MCP confirmou zero recibos no período real;
a correção da fonte não cria compras nem altera retroativamente a contagem histórica.

O CI identificou formatação pendente e a referência imutável da projeção na atestação
privada de Mira v43. A formatação foi aplicada, e a v44 registra apenas compatibilidade
das fontes compartilhadas, com os testes de projeção, vídeos e decisão já executados.
A v43 permanece intacta. O verificador de evidências e a seleção de publicação são
executados localmente; a atestação não autoriza publicar Mira nem renova pareceres.
