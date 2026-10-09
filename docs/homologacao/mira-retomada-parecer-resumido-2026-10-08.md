# Mira — retomada da comunicação com parecer funcional persistido

Solicitação iniciada em 2026-10-08; validação local em 2026-10-09.
Produto 10, cadeia 26, ciclo 9, experimento 102.
Processo pai 113/v11, execução 54; criativos 121/v10, execução 55.

## Aceite e limites

A resposta à pendência #3287 autoriza teto **total acumulado de USD 15** para a mesma
preparação e revisão, incluindo o consumo anterior. Não autoriza mídia, geração paga
de vídeos, cobrança ou publicação comercial. O limite foi registrado pela tela do
produto, preservando a autorização anterior. Consumo conhecido antes da retomada:
USD 9,4724256 estimados, não conciliados com fatura; saldo estimado USD 5,5275744.

Reutilizar produto privado v3, gate 545 e mensagem 634 aceitos. Materializar a correção
do parecer de Psique 637, submeter os mesmos pixels a Psique e Têmis e comprovar os
aceites no backend. A decisão humana de uso da peça é independente desses pareceres.

## Causa comprovada

A retomada do pai pelo frontend retornou HTTP 200, mas o filho permaneceu BLOCKED
sem tarefa nova nem consumo. O domínio reconhece ADJUST de Psique 637, posterior à
produção 636. O diário registra NO_PROGRESS porque a chave de correção continua vazia.

O contexto de automação consulta o histórico com `includePromptAudit=false`. Essa
projeção omite também `comments`, onde o resultado funcional era lido pelo resolvedor
de correções. Os testes anteriores entregavam comentários completos e não reproduziam
o contrato resumido realmente utilizado. Histórico, resposta original e custo são válidos.

## Alternativas

| Alternativa | Benefício | Risco/esforço e aderência | Escolha |
|---|---|---|---|
| Carregar auditoria integral na automação | Disponibiliza o parecer | Hidrata prompts e evidências desnecessários em cada leitura; aumenta latência | Rejeitada |
| Retomar o filho manualmente ou trocar a chave sem causa nova | Contorna esta espera | Mantém intervenção recorrente e pode repetir consumo sem mudança | Rejeitada |
| Consultar a projeção funcional já existente, por definição e referência | Recupera o parecer sem auditoria e preserva deduplicação | Ajuste localizado com regressão integrada | Adotada |

## Matriz definida antes dos testes

| Dimensão | Critério |
|---|---|
| Caso original | Resumo sem comentários + ADJUST persistido posterior à produção permite uma correção |
| Caminho completo | Controller → coordenador → produção → revisão → correção → Psique → Têmis → espera humana; decisão explícita apenas na fixture |
| Regressão | Identificadores sintéticos diferentes, caminho antes válido, parecer antigo/aprovado/técnico/inválido e atividade independente |
| Escopo | Consulta funcional filtra definição e referência; resultado deve corresponder à tarefa, atividade e status do resumo |
| Idempotência | Polling, reinício do coordenador e falha da própria correção não criam nova tarefa com a mesma entrada |
| Persistência e observabilidade | Histórico/custos preservados, hash do parecer no diário; resumo não expõe prompt ou auditoria |
| Interface | Fluxo local em Chromium desktop, iPhone 15 Pro e Pixel 7; origem e decisão humana preservadas |
| Peça | Fonte aprovada, SHA-256, recorte fiel, pele explicitada, resultado útil, limites legíveis a 393px |
| Segregação | Banco e IDs locais, modelo simulado, nenhuma venda, mídia, SMTP real ou artefato de QA em produção |
| Entrega | Testes unitários/backend, integração, empacotamento e revisão do diff antes de PR; SHA/runs e comportamento publicado depois |

## Evidências

Snapshots, diário, autorização e logs locais ficam em `artifacts/mira-budget15/`.
Os resultados finais serão registrados após a validação, sem confundir revisão privada
com vendas ou lucro.

## Resultados locais

- Reprodução antes da correção: o teste integrado esperava WAITING_ACTIVITY e recebeu
  BLOCKED ao remover comentários do resumo, reproduzindo o impedimento observado.
- Backend completo: 4.425 cenários, 4.388 executados sem falha/erro e 37 condições opcionais
  dispensadas. Inclui ArchUnit, correção com duas identidades e leitura JPA do resultado
  funcional separada do resumo, recusando outra referência.
- MySQL 5.7: 19 cenários de API, 22 de ciclo e reinício com diário/deduplicação preservados.
  A fixture foi atualizada para injetar a dependência funcional obrigatória; o query real
  existente é exercitado também com H2 no ciclo de comunicação.
- Chromium desktop, iPhone 15 Pro e Pixel 7: correção, referência do pai, contexto copiado,
  formulário humano explícito, conclusão do double e leitura da prova; sem overflow ou
  erro JavaScript. Build da interface, replay do renderer, contratos de empacotamento
  comercial, Spotless e bash -n/ShellCheck dos runners relevantes aprovados.
- Prévia local: fonte aprovada 523, hash conferido, recorte fiel em `x=100,y=2390,979×780`;
  conteúdo documental, fonte e limites visíveis, headline de cuidados com a pele e
  rótulo de prévia distintos do CTA. Nenhum PNG local foi cadastrado como peça produtiva.
- Topologia MySQL e servidores locais removidos. Zero escrita comercial, chamada de
  modelo ou custo externo nos testes. Autorizações e resultados produtivos preservados.

A melhoria do harness é a fixture realista sem comentários na leitura de acompanhamento.
Os pareceres continuam disponíveis separadamente para a automação; um mock com auditoria
completa não pode voltar a mascarar esse contrato. Não há prova de ganho de vendas, custo
ou latência em produção nesta homologação local.
