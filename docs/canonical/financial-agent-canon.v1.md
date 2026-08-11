# Agente Financeiro Canônico v1

## Objetivo

O Agente Financeiro reconcilia diariamente custos e receitas do Marketing Hub por planejamento, identifica divergências e protege os gates econômicos. Sua conclusão é fiscalizatória e nunca representa autorização para gastar.

## Autoridade

- O backend congela planejamento, campanha, custos de IA/vídeo, demais custos atribuídos e receita aprovada.
- O executor consome somente o endpoint `pending` e opera com Codex em sandbox `read-only`.
- A v1 não movimenta dinheiro, compra créditos, altera preço, orçamento, campanha, publicação ou status comercial.
- Reembolsos e infraestrutura ausentes devem aparecer como lacuna de fonte, nunca como zero confirmado.
- Projeções, impactos estimados, pedidos, checkouts e PRs nunca contam como receita.
- Projeções de receita devem consumir as premissas financeiras estruturadas e versionadas do Plano Comercial. Ausência de preço, custo variável, tráfego, conversão, CAC, reembolso ou custo fixo deve aparecer como limitação explícita, sem inferência silenciosa.
- Toda nova geração manual de imagem ou projeto de vídeo do Estúdio exige produto e plano comercial; experimento é opcional e deve pertencer ao plano quando informado. Tentativas legadas ou excepcionalmente sem plano nunca podem desaparecer: entram no ledger como custo sem atribuição e bloqueiam a conclusão até a regularização.
- Cada tentativa do Estúdio deve possuir entrada idempotente no ledger com tipo de ativo, origem, produto, plano, experimento, provedor, modelo, status, horários e evidência de custo.
- Compras de créditos pré-pagos devem ser registradas separadamente do ledger de consumo, com provedor, data, valor, moeda, quantidade de créditos e referência da evidência. A recarga representa saída de caixa e saldo adquirido; somente o uso por job representa custo consumido. Somar os dois como custo de produção é proibido.
- A entrada nasce antes do consumo externo e é atualizada pela mesma chave de origem durante processamento, sucesso, falha ou expiração. Áudio, vídeo, imagem, montagem, pós-produção e cada retry pago contam como tentativas independentes.
- Custo ausente do provedor deve permanecer ausente e reduzir a cobertura; nunca pode ser convertido em custo zero confirmado.

## Relatório

Cada execução persiste o snapshot recebido, totais reconciliados, cobertura das fontes, divergências, decisão, resposta bruta, modelo, custo da execução, falha e relatório diário com data e hora.

O snapshot expõe separadamente o custo conhecido do Estúdio em USD e a razão de tentativas com custo conhecido, sem conversão cambial implícita.

O snapshot também expõe custo e cobertura do Estúdio sem atribuição comercial. Esses valores não devem ser somados automaticamente ao planejamento em análise, pois isso contaminaria outro produto; devem aparecer como divergência bloqueante até que produto, plano e experimento corretos sejam vinculados.

O snapshot deve consolidar por provedor a quantidade de tentativas, cobertura de custo, assets revisados, assets aprovados, pendências, taxa de aprovação comercial e custo conhecido por asset aprovado. A taxa usa apenas revisões concluídas (`APPROVED` ou `REJECTED`) como denominador. O custo por aprovado soma o consumo conhecido do provedor e nunca deve ser apresentado como custo total confiável quando houver tentativas sem custo. O agente pode recomendar onde avaliar uma recarga, mas deve bloquear a comparação quando faltarem custos ou revisões comerciais e nunca pode comprar créditos.

Cobertura `NO_ATTEMPTS_RECORDED` não representa custo zero confirmado: significa que nenhuma tentativa do Estúdio foi auditada para o plano e deve bloquear a reconciliação. Cobertura `PARTIAL` também bloqueia a conclusão e informa explicitamente quantas tentativas permanecem sem custo. Somente `COMPLETE`, com ao menos uma tentativa, permite declarar o ledger do Estúdio coberto.

Decisões permitidas: `RECONCILED`, `REVIEW_REQUIRED` e `BLOCKED_BY_MISSING_SOURCE`.

## Operação

O módulo executor é `financial-agent-worker`. Prompt e schema ficam versionados em `src/main/resources/prompts/financial-agent/v1`. A imagem de produção deve ser construída exclusivamente pelo Dockerfile e Compose do repositório. O workflow dedicado testa, reconstrói, reinicia e valida o login do Codex no VPS. O backend permanece fonte de verdade e o worker não acessa o banco.

O worker deve persistir logs em arquivo e publicar somente a leitura pelo endpoint operacional versionado `/ops-financial-agent-observability-v1/financial-agent-worker-log`. O MCP deve disponibilizar essa origem no módulo `financial-agent-worker` da ferramenta `java_module_logs`, permitindo correlacionar reserva, conciliação, decisão, Codex e callbacks sem depender apenas do resumo persistido no backend.

O MCP deve expor o diagnóstico somente leitura `studio_ledger_coverage`, comparando as fontes canônicas de tentativas com o ledger por origem, tipo de ativo e provedor. O resultado deve destacar tentativas sem ledger, custo desconhecido e atribuição comercial ausente; nenhuma dessas lacunas pode ser apresentada como custo zero.

Toda execução do Codex no Agente Financeiro deve usar limite operacional padrão de 40 minutos, configurável por ambiente, encerrando e registrando como falha qualquer processo que ultrapasse esse prazo.

O agente deve permanecer cadastrado no catálogo canônico com a chave `financial-agent`, contrato versionado, modelo ativo e autoridade somente leitura.

## Evolução

A autonomia somente poderá ser ampliada após pelo menos 30 dias de conciliações confirmadas, sem bloqueios indevidos ou divergências relevantes. Compras, transferências, mudanças de preço e aumento de orçamento continuam exigindo aprovação humana.
# Ordem das migrações do ledger do Estúdio

O changelog mestre deve criar `studio_cost_ledger_entry`, permitir `commercial_plan_id` nulo e somente depois executar o backfill completo de mídias. Essa ordem é parte do contrato financeiro: consumos sem atribuição precisam ser preservados como pendentes, nunca descartados nem atribuídos artificialmente a outro plano.

O validador `scripts/validate-liquibase-mysql57.sh` deve bloquear ausência, duplicidade ou inversão desses três includes.
