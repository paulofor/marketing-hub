# Navegação do produto para o ciclo pendente — 06/10/2026

## Estado confirmado e causa

Capella (#7) mantém `commercial_status=VALIDACAO_COMERCIAL`. O ciclo #4/#88 foi
encerrado para ajuste e tem sucessor #5/#98, aberto em `ADJUSTMENT`, na cadeia #26.
Mira (#10) conserva #3/#93 encerrado para ajuste, sem sucessor. A consulta pelo MCP
e a UI confirmaram estes estados; o backend publicado era `9f79de860be9f53b2e9160509b2cc6c3b71b0c73`.

O card buscava atividades do Processo 5 por status comercial e podia apontar à
homologação encerrada. `SalesFlowResolver` só participa da posição quando o status
já indica Processo 6. Logo, trocar apenas o href de Capella esconderia a causa.

## Alternativas comparadas

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Alterar o link por produto/ID no frontend | Mudança pequena | Repete o defeito em outros produtos e ignora o estado oficial | Descartada |
| Alterar o status comercial para forçar o Processo 6 | Reutiliza parte do card existente | Confunde evolução comercial com navegação e altera história | Descartada |
| Backend expor a navegação do ciclo pendente na posição existente | Fonte única, sem mutações e sem outra chamada do navegador | Projeção aditiva, teste de contexto e precedência | Adotada |

`GET /api/products/value-chain-positions` (inclusive `playOnly=true`) e
`GET /api/products/value-chain-positions/{productId}` passam a expor
`learningCycleNavigation`: produto, ciclo, experimento, cadeia, fase, status, motivo e URL.
O resumo leve permanece sem leituras históricas. Consulta da última passagem filtrada
por produto e versão exata da cadeia no SQL. Ciclo aberto ou ajuste encerrado sem
sucessor recebe o destino `/business-process-chains/learning-cycles?productId=...&chainId=...`.
O backend não migra ciclos, renova pareceres ou altera autorizações por leitura.

## Matriz definida antes da validação

| Cenário | Aceite e evidência prevista |
| --- | --- |
| Capella histórico #4 em decisão, status comercial no Processo 5 | Botão azul abre os ciclos de #7/#26 |
| Capella atual #5/#98 em ajuste | Mesmo destino, identidade do sucessor preservada |
| Outro produto/IDs em outra cadeia | URL própria, nenhum ID fixo ou herança de autorização |
| Ajuste encerrado sem sucessor (Mira) | Destino do ciclo permite preparar a continuidade |
| Ajuste já sucedido; ciclo terminal | Não sugerir decisão encerrada; conservar rota oficial anterior |
| Nenhum ciclo; macroprocesso concluído | Continuação do processo anterior preservada |
| Contexto divergente entre produto, cadeia, ciclo ou experimento | Não mostrar navegação de outra passagem |
| Falha de API da posição | Não reutilizar decisão como válida; recuperação anterior preservada |
| Consulta HTTP, navegação e observabilidade | Contrato aditivo; zero gravações, tarefas, inferências pagas ou mídia |
| Chromium desktop, iPhone 15 Pro e Pixel 7 | Link visível e clicável abre a URL exata; sem âncora de processo |
| Dados de teste e métricas | Fixtures e APIs simuladas locais; navegação administrativa sem visita comercial |

## Harness e limite da entrega

A lacuna nas regressões era não representar ciclo pendente enquanto o cadastro
comercial ainda apontava ao Processo 5. Ampliar as fixtures e os contratos existentes
com esse estado comprova a precedência sem criar mecanismo paralelo de decisão.
Nenhum prompt de especialista muda: este pedido trata da navegação, não executa
atividade, aprova estratégia ou comprova venda. Evidências locais e publicadas serão
registradas após os testes e a entrega.

## Evidências locais

- Frontend: 61 testes dos cards, da lista e da continuidade passaram; os 18 testes
  do card com ciclo foram revalidados após preservar o atalho secundário da atividade
  delegada. TypeScript e build do bundle produtivo passaram.
- Navegação: 12 cenários no bundle local em Chromium desktop, iPhone 15 Pro e Pixel 7;
  URL exata, seleção de produto/cadeia, acesso ao ciclo simulado, zero mutações,
  zero consultas à homologação histórica e ausência de overflow/erros JavaScript.
- Harness comercial existente: 15 testes e construção das 61 atestações passaram.
- Spotless nos oito arquivos Java envolvidos e revisão de comentários passaram.
- A primeira execução completa do backend terminou por OOM (exit 137, confirmado
  pelo cgroup), após 3.460 testes sem falhas/erros. A conclusão exige a execução
  recuperada, isolada do build e com cache de contextos Spring limitado a quatro.

Os testes usam apenas fixtures e integrações locais. A leitura de produção pelo
MCP/UI confirmou o contexto; o teste do navegador não acessa campanha ou página
comercial. A URL de ciclo preserva os IDs do próprio contrato, sem âncora de processo.

A execução recuperada concluiu com sucesso: 4094 testes, 638 suítes, zero falhas/erros, 27 testes condicionais ignorados;
92 verificações ArchUnit passaram. O cache limitado e a liberação do heap de
compilação do processo Maven pai evitaram nova pressão de memória, sem alterar
o código produtivo ou os testes. Os testes condicionais ignorados não incluem
a matriz de navegação, contratos ou arquitetura desta entrega.
