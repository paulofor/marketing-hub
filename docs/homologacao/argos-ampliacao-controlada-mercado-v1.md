# Homologação — ampliação controlada de mercado do Argos v1

## Objetivo e verdade comercial

Permitir que uma execução `DISCOVER_MARKETS` aprofunde uma lente adjacente quando a primeira coleta
terminar em `RESEARCH_MORE`, sem criar outra execução, sem escolher o posicionamento que pertence a
Atena e sem transformar anúncio, oferta ou score em venda.

Gargalo observado na execução independente #11: Argos formou três candidatas, mas encontrou somente
uma de dez ofertas comparáveis e nenhum anúncio Meta/Instagram comprovado. A tarefa terminou
tecnicamente, embora o próprio relatório pedisse aprofundamento. A métrica de liberação é ao menos
uma candidata `DOSSIER_READY`; venda continua sendo somente pagamento aprovado e reconciliado.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Tornar a primeira pesquisa genericamente mais ampla | Uma única chamada adicionalmente abrangente | Mistura mercados, reduz aderência e ainda depende de uma única aposta | Não escolhida |
| Criar uma execução nova para cada mercado adjacente | Histórico separado por mercado | Perde evidência acumulada, duplica tarefas e consumo e exige coordenação humana | Não escolhida |
| Reavaliar na mesma execução, com até três lentes adjacentes | Preserva contexto e auditoria, limita consumo e corrige a lacuna dentro da atividade vigente | Exige deduplicação, condições de parada e relatório por tentativa | Escolhida |

## Contrato de execução

- até três tentativas totais na mesma execução, ciclo, tarefa e lease;
- a primeira usa o escopo recebido; as seguintes ampliam exatamente uma lente investigativa
  adjacente e não definem o posicionamento final;
- Web, ofertas, Meta e acervo são acumulados e deduplicados antes de cada nova síntese;
- parar imediatamente ao obter uma candidata `DOSSIER_READY`;
- parar quando uma ampliação não trouxer nova evidência Web, oferta comparável nem anúncio Meta;
- parar no limite de tentativas, preservando `RESEARCH_MORE` e a lacuna real;
- `VALIDATE_MARKET` e fallback determinístico continuam com uma única tentativa;
- nenhuma tentativa autoriza publicação, compra, afiliação, campanha, contato ou aumento dos limites
  de coleta por fonte.

## Matriz ponta a ponta

| Cenário | Preparação local segregada | Resultado esperado | Observabilidade |
| --- | --- | --- | --- |
| Caminho feliz na segunda lente | Ciclo `DISCOVER_MARKETS`, primeira rodada com lacunas e segunda com evidência suficiente | Um único callback final, candidata `DOSSIER_READY` e handoff liberado pelo backend | Duas lentes, incrementos e motivo `DOSSIER_READY_FOUND` no relatório |
| Sucesso já na primeira rodada | Primeira síntese pronta | Nenhuma ampliação ou consumo extra | Uma tentativa e motivo de sucesso |
| Ampliação sem evidência nova | Segunda lente devolve somente itens já coletados | Encerrar sem terceira síntese e preservar o melhor relatório anterior | Motivo `NO_NEW_EVIDENCE` e incrementos zerados |
| Limite alcançado | Três rodadas com progresso, mas sem dossiê pronto | Encerrar honestamente em `RESEARCH_MORE` | Três tentativas e motivo `ATTEMPT_LIMIT_REACHED` |
| Lente/consultas repetidas | Planejador repete a lente ou quase todas as consultas | Não repetir coleta nem gastar outra síntese | Motivo `REPEATED_RESEARCH_LENS` |
| Modo de validação | Ciclo `VALIDATE_MARKET` | Uma única rodada | Motivo `EXPANSION_NOT_APPLICABLE` |
| Modelo desabilitado | Plano/síntese determinísticos | Uma única rodada, sem simular expansão inteligente | Motivo `EXPANSION_NOT_APPLICABLE` |
| Fonte externa indisponível | Meta ou marketplace devolve indisponibilidade, Web ainda responde | Ausência permanece lacuna; o fluxo pode testar outra lente até o limite | Cobertura e causa preservadas, sem inferir ausência de mercado |
| Deduplicação acumulada | Mesma URL/oferta aparece em duas consultas | Gate conta o item uma única vez | Contagem final e incremento por rodada coerentes |
| Auditoria do modelo | Duas ou três chamadas de plano/síntese com tokens conhecidos | Request, resposta bruta, modelo, URLs e consumo agregados na mesma tarefa | Histórico estruturado por tentativa, sem perder respostas anteriores |
| Backend e segregação | Test doubles usam ciclo e lease exclusivos | Nenhuma tarefa, ciclo ou produto extra é criado | Mesmo `cycleId`, mesmo lease e um callback terminal |
| Tela desktop | Relatório com três tentativas | Lentes, progresso, limite e motivo legíveis | Chromium desktop sem overflow ou ação enganosa |
| Tela iPhone 15 Pro | Mesmo relatório | Cards em uma coluna e conteúdo legível por toque | Emulação mobile completa |
| Tela Pixel 7 | Mesmo relatório | Cards em uma coluna e conteúdo legível por toque | Emulação mobile completa |

## Critério operacional

- **Continuar:** a rodada ainda não formou dossiê pronto, trouxe evidência nova e há tentativa
  disponível.
- **Ajustar:** mudar somente uma lente adjacente com base nas lacunas persistidas, mantendo público,
  país, canal e limites comerciais do briefing.
- **Parar:** dossiê pronto, nenhuma evidência nova, lente repetida, modo inelegível ou terceira
  tentativa concluída.

Se uma rodada completa revelar defeito, a homologação somente termina após a correção e duas rodadas
locais completas e consecutivas sem falhas.
