# Pareceres da homologação comercial — Vega / MUSA v7

## Resultado final local

- Processo: `pde-commercial-homologation-activation` v4, atividade `pdeGate`.
- Produto: `4`, nome interno `Vega`, versão
  `musa-pde-entry-v7-espelho-antes-de-sair`.
- Psique: `APPROVED`, valor percebido simulado `91/100`.
- Têmis: `APPROVED`, preço `100/100`, recomendação `READY_FOR_PREFLIGHT` e nenhuma alteração
  obrigatória na candidata local.
- Limite: os pareceres não autorizam publicação, contato, mídia, gasto ou estado `RUNNING`. A versão
  publicada ainda deve ser identificada por JSON e passar pelo preflight produtivo.

## Ajustes exigidos durante as revisões

1. Identidade comercial e promessa foram unificadas entre manifesto, catálogo, frontend e test
   double.
2. Token de acesso foi removido e mascarado em toda a telemetria do navegador.
3. Os sete formulários e orientações passaram a materializar o mecanismo específico de cada missão
   v7 a partir do catálogo canônico.
4. A degustação passou a emitir `TASTING_STARTED`, `VALUE_MOMENT`, `PAYWALL_VIEWED` e
   `CHECKOUT_STARTED`, com replay idempotente.
5. Marcos finais deixaram de ser duplicados pelo frontend e permanecem sob autoridade do backend.
6. A primeira amostra foi alinhada matematicamente: cinco vendas líquidas em 100 visitantes humanos
   correspondem a 5%; 0,8% permanece apenas como baseline histórico de planejamento.
7. Toda transação e acesso `@sandbox.local` permanece `INTERNAL_QA` durante compra, primeira
   utilização, missões, entrega e reembolso. O ciclo é preservado na auditoria bruta e fica zerado
   nos indicadores humanos e comerciais.

## Telemetria dos agentes

- Execuções concluídas contabilizadas: 15.
- Tokens de entrada: 1.969.285.
- Tokens de entrada em cache: 0.
- Tokens de saída: 25.612, dos quais 3.617 foram identificados como raciocínio.
- Custo estimado no tier padrão: US$ 8,389380, usando US$ 4,00 por milhão de tokens de entrada e
  US$ 20,00 por milhão de tokens de saída do catálogo versionado de `gpt-5.6-sol`.
- Uma tentativa de Têmis atingiu o timeout de 40 minutos sem evento final de uso; ela não foi
  declarada como custo zero nem incluída nos totais acima.
- Flex foi solicitado em todas as execuções finais, mas o catálogo do executor informou que o
  modelo não anuncia suporte e omitiu o tier; as chamadas efetivas usaram o tier padrão.

## Resultado comercial observado

- Vendas humanas: 0.
- Receita humana: R$ 0.
- Contatos enviados: 0.
- Gasto externo: R$ 0.
- A meta futura continua sendo cinco vendas líquidas, atribuídas e entregues satisfatoriamente;
  QA, parecer, visita, clique ou checkout não contam como venda.
