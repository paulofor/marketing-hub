# Mira #93 — conciliação do Processo 5.4

Data: 02/10/2026. Produto #10, tipo cadastrado `AI_PRODUCT` (Safira),
contrato `MIRA_COMMERCIAL_V1`, cadeia #24 v24, definição #58 v5,
`experiment-homologation-activation`, execução automática #36,
referência `experiment:93`. Sem ciclo informado.

## Estado confirmado e limites

O contexto copiado informava duas atividades concluídas e falha na mensuração.
A tela, os endpoints oficiais e o banco via MCP confirmaram três instâncias
concluídas: superfícies #471, compra/acesso/entrega #472 e mensuração #473.
Todas reutilizam o run produtivo #14, número 2, com gates aprovados,
qualidade de dados `VALID` e custo incremental conhecido de USD 0.

A execução #36 conservava `ERROR`, progresso 2/4 e atividade `measurement`,
apesar da persistência da terceira prova. A falha transacional já foi corrigida
pelo PR #5468, publicado no backend identificado pelo SHA
`2c6841486eac326f4c35c5bfacf83501b33f4a8f`. A correção mantém o gate financeiro;
apenas evita que uma pendência funcional invalide a consulta/conciliação.
Os logs antigos não estavam presentes na retenção disponível nesta consulta;
o diagnóstico anterior está preservado em `titulo-processo-posicao-cadeia.md`.

Após validar localmente a correção existente, a ação **Retomar processo**
na tela respondeu HTTP 200. O worker conciliou a mesma execução:
`WAITING_INPUT`, 3/4, atividade `financialGuardrails`, `failure_count=0`.
As três instâncias originais foram preservadas e não surgiu nova tarefa paga.

A quarta atividade continua sem objetivo comprovado. O plano comercial #8 v6
está `BLOCKED`: nenhuma das seis versões históricas consta em execução ou
concluída. Seu texto ainda menciona Plutus e preflight pendentes, embora
existam parecer financeiro #61 `APPROVE`, plano financeiro LIVE #8 revisão 3
não desatualizado e preflight aprovado. O parecer é válido até 06/10/2026,
com preço de R$ 49, envelope variável de R$ 14 e CAC máximo de R$ 25.
Ele não muda automaticamente o estado administrativo do plano nem autoriza gasto.

O experimento #93 está `INVALIDATED` e sua janela terminou em 30/09/2026.
Não é aceitável transformar o plano em ativo, ampliar a janela, repetir Plutus
ou marcar a quarta atividade concluída para contornar a pendência.
O gate já possui teste que rejeita plano rascunho/bloqueado. Uma eventual
decisão de planejamento deve respeitar o piloto encerrado e seus limites.

O pai é a definição #96 v10, atividade `preflight`. Sua execução #30
já está `COMPLETED` desde 29/09, com instância #439 comprovada pelo run #13.
A execução #36 foi iniciada independentemente (`parent_run_id` nulo);
o link do grafo identifica o pai, mas não cria retroativamente uma chamada.
Não reabrir a execução #30 nem substituir sua prova pelo subprocesso incompleto.

## Alternativas e melhoria do harness

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Marcar conclusão ou ativar o plano manualmente | Remove o aviso rapidamente | Dispensa o gate e pode reinterpretar o piloto encerrado | Rejeitada |
| Pedir outro parecer de Plutus | Produz mais um relatório | Gasto sem resolver o estado do plano, com premissas já aprovadas | Rejeitada |
| Conciliar provas e identificar o bloqueio na fonte | Preserva evidências e evita revisão redundante | Melhoria pequena, sem relaxar critérios | Adotada |

A mensagem genérica não identificava qual plano impedia a conclusão e a
recomendação sugeria renovar a homologação técnica. O avaliador agora informa
experimento, IDs e estados dos planos vinculados. O executor orienta conferir
o estado do plano e preservar pareceres compatíveis, distinguindo esse problema
de evidência técnica ausente. Não foram alterados prompts, modelos, filas,
limites, oferta, agentes, autorização de mídia ou critérios de aprovação.

## Matriz de aceite local

| Cenário | Evidência exigida |
| --- | --- |
| Superfície, transação e mensuração | Gates do mesmo produto/run, referências e impressão preservadas |
| Plano governante válido | Limites coerentes e Plutus quando aplicável; `spendAuthorizedByThisActivity=false` |
| Plano bloqueado, cancelado ou rascunho | IDs/estados corretos, `ready=false`, nenhuma conclusão ou promoção do plano |
| Transação de conciliação | Três provas lidas e progresso 3/4 persistido em H2/Spring com próxima atividade bloqueada; nenhum rollback inesperado |
| Outras identidades | Caso original e dois experimentos sintéticos independentes; sem exceção por produto ou ID |
| Provas inválidas | Produto distinto, gate sem referência, fonte vencida e predecessor incompleto mantêm proteção |
| Idempotência | Evidência igual não cria nova ocorrência; custo incremental da reutilização permanece zero |
| Interface local | Backend simulado com resposta produzida pelos testes, título 5.4 e bloqueio legíveis em desktop, iPhone e Pixel emulados |
| Observabilidade e isolamento | Diário anterior preservado, sem IA, pagamento, tráfego ou eventos comerciais nos testes |

Testes aprovados comprovam comportamento técnico, não compras, uso, satisfação,
receita ou margem realizada. A homologação permanece 3/4 enquanto faltar
um plano governante elegível. Não confundir essa pendência com falta de parecer de Plutus.

## Resultados locais antes da publicação

- Suíte completa do backend: 610 relatórios, 3.776 casos, 3.753 executados,
  zero falhas/erros e 23 ignorados pela configuração existente. Os ignorados
  são contratos optativos de outros fluxos, que exigem flags/ambientes próprios,
  e uma comparação HTML já desabilitada; nenhum cenário deste ajuste foi ignorado.
- Regressões focadas: 16 casos aprovados, incluindo três identidades/estados
  de plano e transação H2/Spring que persiste `WAITING_INPUT` e progresso 3/4.
- Formatação Spotless e `git diff --check` aprovados; empacotamento Maven aprovado.
- Nove testes de integridade do pacote aprovados; 4.204 classes testadas e
  empacotadas idênticas, 720 recursos externos íntegros e catálogo de 489 cards
  inicializado a partir do JAR executável.
- Tela local com respostas oficiais sanitizadas e a resposta nova exportada
  pelo teste: desktop, iPhone 15 Pro e Pixel 7 emulados. Título **Processo 5.4**,
  progresso 3/4, plano #8 `BLOCKED` e recomendação de preservar Plutus legíveis,
  sem overflow horizontal, erros JavaScript ou requisições de escrita.
- O mock de leitura do contexto sem ciclo foi corrigido para devolver `null`
  em vez de objeto vazio, respeitando o contrato existente; não exigiu alteração
  do frontend. Nenhum teste produziu evento comercial, envio de e-mail real,
  geração de IA, pagamento ou gasto de campanha.
