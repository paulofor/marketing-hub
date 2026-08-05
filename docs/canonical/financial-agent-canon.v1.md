# Agente Financeiro Canônico v1

## Objetivo

O Agente Financeiro reconcilia diariamente custos e receitas do Marketing Hub por planejamento, identifica divergências e protege os gates econômicos. Sua conclusão é fiscalizatória e nunca representa autorização para gastar.

## Autoridade

- O backend congela planejamento, campanha, custos de IA/vídeo, demais custos atribuídos e receita aprovada.
- O executor consome somente o endpoint `pending` e opera com Codex em sandbox `read-only`.
- A v1 não movimenta dinheiro, compra créditos, altera preço, orçamento, campanha, publicação ou status comercial.
- Reembolsos e infraestrutura ausentes devem aparecer como lacuna de fonte, nunca como zero confirmado.
- Projeções, impactos estimados, pedidos, checkouts e PRs nunca contam como receita.

## Relatório

Cada execução persiste o snapshot recebido, totais reconciliados, cobertura das fontes, divergências, decisão, resposta bruta, modelo, custo da execução, falha e relatório diário com data e hora.

Decisões permitidas: `RECONCILED`, `REVIEW_REQUIRED` e `BLOCKED_BY_MISSING_SOURCE`.

## Operação

O módulo executor é `financial-agent-worker`. Prompt e schema ficam versionados em `src/main/resources/prompts/financial-agent/v1`. A imagem de produção deve ser construída exclusivamente pelo Dockerfile e Compose do repositório. O workflow dedicado testa, reconstrói, reinicia e valida o login do Codex no VPS. O backend permanece fonte de verdade e o worker não acessa o banco.

## Evolução

A autonomia somente poderá ser ampliada após pelo menos 30 dias de conciliações confirmadas, sem bloqueios indevidos ou divergências relevantes. Compras, transferências, mudanças de preço e aumento de orçamento continuam exigindo aprovação humana.
