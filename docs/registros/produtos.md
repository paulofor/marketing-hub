# Registro de evolução do catálogo de produtos

## 2026-09-01 — Argos amplia o mercado de forma controlada dentro da mesma execução

- Evidência: a execução #11 encontrou três dores potenciais, mas somente uma de dez ofertas e nenhum
  anúncio Meta comprovado; todas ficaram em `RESEARCH_MORE` e nenhuma receita foi gerada.
- Alternativas: ampliar tudo em uma única busca diluiria aderência; abrir execuções independentes
  perderia contexto e elevaria consumo; foi escolhida a reavaliação acumulativa e limitada.
- Decisão: em `DISCOVER_MARKETS`, Argos pode testar até três lentes adjacentes, uma por vez, mantendo
  público, canal, país, segurança e autoridade estratégica de Atena.
- Métrica: continuar somente com evidência incremental; liberar a cadeia ao encontrar candidata
  `DOSSIER_READY`; parar sem mascarar a lacuna quando não houver progresso ou o limite terminar.
- Verdade comercial: ampliação, oferta observada, anúncio e dossiê não são vendas; receita continua
  sendo somente pagamento aprovado e reconciliado.

## 2026-08-31 — Handoff autônomo da descoberta até produto planejado

- Evidência: ciclos independentes de Argos concluíam sem candidatas estruturais, dossiê ou fila para
  Atena; a maturidade devolvida pelo worker permanecia apenas dentro do JSON de evidências.
- Alternativas: transferência manual mantinha a perda de contexto; um pipeline paralelo duplicaria
  filas e responsabilidades; foi escolhida a conexão com os contratos BPM já publicados.
- Decisão: tema amplo inicia descoberta B2C/Instagram com Web, Meta e `/pesquisas`; o backend cria um
  dossiê por candidata e libera sequencialmente Atena, Plutus e Dédalo apenas para `DOSSIER_READY`.
- Segurança: alto risco e rejeição prevalecem sobre prontidão declarada pelo modelo; o worker e o
  backend bloqueiam combinações contraditórias antes do handoff.
- Materialização: três gates aprovados criam um produto `PLANNED` e preservam plano, harness,
  evidências e custos, sem publicar, gastar ou registrar venda.
- Relatório: a tela independente deve explicar candidatas, fontes, dossiês, decisões, bloqueios,
  próxima ação e produto sem obrigar leitura de JSON técnico.

## 2026-08-30 — Argos amplia a descoberta factual para aquisição pelo Instagram

- Evidência histórica: os ciclos 40 a 44 operaram em fallback determinístico, com pouca cobertura e
  sugestões genéricas, apesar de existirem artigos e fontes editoriais úteis para abrir novas lentes.
- Decisão: manter Argos como investigador factual e Atena como estrategista; criar outro agente
  duplicaria coleta, custo e autoridade sem resolver a falta de evidência.
- Evolução: a tela passa a declarar descoberta ou validação, B2C/B2B, Instagram e fontes editoriais;
  o worker cruza web, `/pesquisas`, ofertas e cobertura Meta antes de organizar candidatas distintas.
- Gate comercial: candidata imatura pode permanecer visível como `RESEARCH_MORE`; aprovação continua
  exigindo dez ofertas comparáveis e os gates canônicos. Sugestão, anúncio e intenção não são venda.
- Métrica de liberação: toda candidata deve ligar público, situação, dor, alternativa, esforço,
  potencial visual e risco apenas a evidências identificadas e auditáveis.

## 2026-08-29 — Consultores enriquecidos pelos comentários e artigos da pesquisa

- Evidência recorrente: pessoas procuram ajuda em momentos concretos, pagam por trabalho
  operacional que reduz esforço e precisam conferir a evidência e a próxima ação; chat genérico não
  cria vantagem suficiente sobre alternativas gratuitas.
- Decisão: Turmalina e Fluorita avançam para bases v2 nos mesmos nove cards editáveis, sem ampliar o
  schema antes de uso e vendas reais.
- Fluorita: a primeira resposta explicita marca, origem, motivo, escopo e controle; microvalor,
  permissão, ajuda humana e continuidade passam a fazer parte do contrato.
- Turmalina: link e microvalor antecedem instalação ou cadastro pesado; instalação só é sugerida
  depois de valor e a recorrência é medida em D1, D7 e D30.
- Proteção: personalização usa dados declarados/autorizados, inferências continuam revisáveis e
  nenhuma ação de alto impacto acontece sem confirmação.
- Verdade comercial: artigo, comentário, utilidade ou intenção não são venda; somente pagamento
  reconciliado entra na métrica de receita.

## 2026-08-26 — Transição de processo libera o período aberto antes do próximo

- Evidência produtiva: a ativação autorizada da Vega pelo experimento 90 retornou HTTP 500 e foi
  integralmente revertida; o MySQL registrou `Duplicate entry '4-1'` na restrição
  `uk_product_process_period_open`.
- Causa-raiz confirmada: o serviço marcava o período vigente como fechado e criava o seguinte na
  mesma transação, mas o Hibernate ordenava o `INSERT` antes do `UPDATE` ainda não efetivado.
- Decisão: efetivar o fechamento do período antes de inserir o próximo, preservando a transação
  atômica e a restrição que impede dois períodos abertos para o mesmo produto.
- Prevenção: o teste do serviço exige explicitamente `saveAndFlush` do período fechado antes do
  `save` do período seguinte. Falha na ativação não pode deixar experimento, run, produto ou janela
  comercial em estado parcial.

## 2026-08-25 — Tela dedicada do histórico da cadeia

- Evidência produtiva: o backend já entregava períodos e custos por etapa, mas o catálogo expunha o
  histórico somente dentro do card e não possuía o botão ou a rota individual solicitados.
- Decisão: adicionar consulta backend por produto e uma tela cronológica navegável pelo card, sem
  filtrar todo o catálogo no navegador.
- Verdade operacional: datas ausentes, saídas não comprovadas e cobertura financeira parcial ou não
  reportada ficam explícitas; processo e subprocesso não são somados para evitar dupla contagem.
- Critério: botão, endpoint individual, histórico e estados de falha devem funcionar em desktop,
  iPhone 15 Pro e Pixel 7 sem alterar dados produtivos.

## 2026-08-25 — Permanência e custo por processo e subprocesso

- Gargalo: os cards indicavam posição e próximo objetivo, mas não mostravam há quanto tempo o produto
  estava parado nem quanto custo conhecido havia acumulado na etapa.
- Alternativas descartadas: commits do Git medem código, não movimento do produto; `product.updated_at`
  muda em qualquer edição e não constitui histórico de transição.
- Decisão: o backend preserva períodos dos macroprocessos e consolida subprocessos pela trilha de
  `agent_task`; a saída preferencial é a entrada comprovada na etapa seguinte.
- Custos: uso de modelo nas tarefas e ledger do Estúdio são agregados por intervalo. Cobertura parcial
  ou ausente permanece explícita, sem transformar subtotal conhecido em total exato.
- Compatibilidade histórica: a posição vigente recebe backfill identificado como estimado; novas
  transições são registradas no instante da alteração do estado comercial.

## 2026-08-24 — Descoberta encerrada sem novo produto aprovado

- Processo executado: `pde-opportunity-discovery` v4, com Rigel 82/100 como benchmark fixo.
- Resultado: `PESQUISAR MAIS`; Pedido no Azul liderou duas rodadas finais com 73 e 70, sem alcançar
  o benchmark e com valor percebido de 72 nas duas execuções.
- Decisão de portfólio: nenhum produto, tipo, estrela, oferta ou experimento foi cadastrado. Pedido
  no Azul permanece somente como sinal prioritário, evitando transformar nota ou parecer em venda.
- Lacunas para reabrir a pesquisa: preferência observada frente à calculadora gratuita, intenção de
  pagar, conclusão sem assistência e rota orgânica atribuível.
- Evidência completa: `docs/marketing/descoberta-oportunidade-pde-2026-08-24.md`.

## 2026-08-23 — Identidade interna separada do nome comercial

- Evidência: o cadastro persistia somente `product.name`; produtos como “PDE Anti-Invisibilidade
  Profissional” e nomes de trabalho versionados disputavam o mesmo campo usado nas ofertas públicas.
- Causa-raiz: identidade de trabalho, nome comercial e nomes históricos não possuíam contratos
  separados, criando risco de duplicação do produto ou exposição de rótulo técnico ao cliente.
- Decisão: manter `id` e `slug` como identidade estável, separar nome interno e nome comercial e
  permitir até 20 apelidos internos únicos e pesquisáveis.
- Proteção comercial: apelidos não entram em definição pública, landing, checkout ou entrega; a
  resolução interna retorna o produto canônico que continua vinculado por `id` e `slug`.
- Critério: uma busca por qualquer nome deve localizar o mesmo cadastro, enquanto a comunicação
  pública usa exclusivamente o nome comercial.

## 2026-08-23 — Catálogo extensível de tipos de produto

- Evidência produtiva: nove registros usavam três rótulos distintos de tipo e três ainda estavam
  sem classificação; `product_type` era texto livre, sem cadastro ou prevenção de duplicidade.
- Alternativas comparadas: manter texto livre, fechar os tipos em enum ou criar catálogo extensível.
- Decisão: catálogo com código estável, nome, descrição, apelidos, estado e contagem de produtos.
- Liberdade de exploração: tipos novos podem nascer como `PROPOSED`; somente `ACTIVE` recebe novos
  produtos, e `RETIRED` preserva histórico.
- Compatibilidade: os três rótulos produtivos atuais são migrados de forma determinística; os dois
  rótulos que descreviam canal/formato viram apelidos dos tipos canônicos e o texto legado permanece
  disponível durante a transição.
- Prevenção: nome, código e apelidos são únicos entre tipos e a tela deixa de aceitar classificação
  livre no cadastro de produtos.

## 2026-08-23 — Universos dos nomes internos

- Decisão: produtos usam estrelas e tipos usam minerais, sem reutilização de codinomes.
- Produtos cadastrados em produção: Vega/Método MUSA, Sirius/Anti-Invisibilidade,
  Capella/Agenda Cheia, Altair/Especialista no WhatsApp e Rigel/Kit WhatsApp Pronto.
- Codinomes reservados para cadastro após publicar a atualização isolada: Polaris/Nexo,
  Antares/rascunho Personal Trainer, Spica/rascunho Manicure e Regulus/rascunho Autoridade e
  negociação.
- Minerais preservados na tela publicada como apelidos pesquisáveis: Opala/PDE, Quartzo/low-ticket,
  Safira/Produto IA, Fluorita/atendimento por sandbox, Ágata/área de membros e
  Labradorita/educação interativa. O cadastro no campo próprio aguarda a publicação da identidade
  interna de tipos e será concluído pela tela.
- Causa-raiz adicional: o formulário integral bloqueava produtos legados sem tipo e regravava todo o
  contrato mesmo quando somente o nome interno mudava; a atualização isolada passa a proteger os
  campos comerciais e permitir governar rascunhos sem inventar classificação.
- Prevenção adicional: o tipo passa a ter `internal_name` próprio; minerais deixam de depender de
  apelidos, aparecem como família interna nos produtos e continuam vedados nas superfícies públicas.
