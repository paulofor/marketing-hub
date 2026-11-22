# Diario de negocio

Este arquivo registra o fechamento de cada dia quando o usuario pedir `feche o dia`.

O foco do registro e negocio, marketing e operacao comercial: campanhas publicadas, experimentos criados ou avancados, produtos/ofertas estruturados, validacoes comerciais, aprendizados, metricas relevantes, decisoes tomadas e proximos passos.

Nao usar este diario como changelog tecnico de sistema. Detalhes tecnicos devem aparecer apenas quando explicarem impacto direto no negocio.

## 2026-07-02

- Experimento 51 ficou pronto para campanha com experiencia premium de compra: pagina de venda, checkout, pagina de obrigado e entrega digital validados publicamente.
- Campanha do experimento 51 foi publicada em modo controlado para validar venda direta do produto de organizacao de agenda para manicures em domicilio.
- Criamos e avancamos o experimento 52 no nicho de alongamento de unhas em domicilio, com produto low-ticket, pagina de venda, checkout, entrega e campanha publicados.
- Criamos o experimento 53 para varejo de vestuario e acessorios, com a oferta Semaforo de Promocao 7D, pagina de venda criada pelo pipeline, checkout, entrega e campanha de venda publicada.
- Definimos como regra de negocio que produtos low-ticket devem usar pagina de venda gerada pelo pipeline antes de liberar campanha, para acumular aprendizado e melhorar a qualidade das paginas.
- Ajustamos a leitura de funil low-ticket para acompanhar a jornada correta: anuncio, pagina de venda, clique no checkout, compra e entrega, sem misturar metricas de formulario.
- Passamos a auditar as paginas de venda geradas com os prompts e schemas usados em cada versao, preservando historico para comparar qualidade e aprendizado mesmo quando novas paginas forem criadas.
- Aprendizado principal do dia: campanhas de venda direta precisam nascer com checkout, entrega, pixel, objetivo de compra e experiencia pos-pagamento prontos antes de receber trafego; do contrario a metrica fica confusa ou a experiencia quebra.
- Proximo passo recomendado: acompanhar revisao e primeiras metricas dos experimentos 51, 52 e 53, olhando principalmente CTR, clique no checkout, compra aprovada e download do produto.

## 2026-07-04

- Experimentos 53 e 54 foram revisados sob criterio estatistico-financeiro e invalidados para proteger orcamento: o 53 ja tinha volume suficiente sem compra, e o 54 mostrou trafego caro demais para low-ticket antes de formar amostra util.
- As campanhas Meta dos experimentos 53 e 54 foram pausadas, evitando continuar comprando aprendizado com baixa expectativa racional de retorno.
- A regra de parada low-ticket foi reforcada: o sync de metricas agora tambem aciona a invalidacao estatistico-financeira, e foi criada regra complementar para trafego economicamente inviavel antes da amostra completa.
- O experimento 55 avancou como primeiro MVP `AI_PERSONALIZED_SAMPLE`, mantendo a regra de nao criar produto manualmente fora do sistema: hipotese preparada, experimento planejado, funil de coleta criado, pagina de venda gerada dentro do Lead Portal e fluxo de entrega paga estruturado.
- Criamos a linha operacional de Produto IA com `product-ai-worker`, pipeline `personalizedsample.v1` e etapa `paid-delivery`, separando a execucao de IA vendida ao cliente do `ai-worker` generico.
- Foi criada a trava de entrega paga para Produto IA: compra aprovada deve enfileirar geracao personalizada, registrar prompt/schema/modelo/request/response/custo e marcar a compra como entregue somente apos retorno do worker.
- O funil do experimento 55 passou a exigir coleta de dados do lead antes da compra, porque a promessa comercial depende de informacoes individuais para gerar uma amostra visual personalizada.
- O GeraSalesPage foi ajustado para Produto IA personalizado: a pagina aprovada fica dentro do funil do Lead Portal, CTAs apontam para a coleta de personalizacao, e a quality review bloqueia confusao entre amostra gratis, produto pago e entrega de R$ 27.
- Corrigimos riscos comerciais antes da campanha do experimento 55: bloqueio de pagina reprovada, remocao de iframe autorreferente no funil e normalizacao de CTA longo de criativos para evitar falha de persistencia.
- A tela de Planejamento foi estabilizada para nao quebrar quando a API retornar nenhum plano ou dados incompletos, permitindo retomar a visao operacional sem depender de estado perfeito.
- Aprendizado principal do dia: Produto IA personalizado so deve receber trafego quando a cadeia inteira estiver rastreavel e operavel pelo sistema: coleta do lead, pagina clara, criativo aprovado, compra, entrega personalizada, custo de IA e auditoria.
- Proximo passo recomendado: apos deploy da normalizacao de CTA, reprocessar os criativos do experimento 55, revisar imagem/copy comercialmente e liberar campanha somente se criativo, publico e prontidao de entrega estiverem aprovados.

## 20/11/2026 — Hipótese de assinatura do MUSA/Vega

O cadastro de Vega indica Consultor PWA com IA e deixa o premium sem preço. Na
tela financeira, R$ 67 e 90 dias ainda vinham de Opala. A homologação local
separou esse histórico da projeção mensal de Turmalina, distinguindo adesões,
renovações e custo da base gratuita. Cobrança, vendas e contribuição realizadas
não fizeram parte do teste. [Memória e critérios de aceite](../homologacao/plano-financeiro-tipo-assinatura-v1.md).

## 2026-11-21 — Nova hipótese de produto com fabricação terceirizada

- Comparadas 12 oportunidades; coleções para hospedagens superaram embalagens e treinamento na
  avaliação de valor, diferenciação por IA e facilidade inicial de produção. Notas são hipóteses.
- Jaspe cadastrado no catálogo em avaliação, sem produto vendável, gasto ou campanha.
- Aprendizado reutilizável: arquivo precisa passar por dimensões, resolução, integridade e escopo;
  uma prévia bonita não comprova fabricação nem demanda. A base foi testada localmente.
- Publicação da base aguarda recuperação comprovada de acessos; fornecedores e oferta ainda
  precisam de homologação antes de qualquer venda. Pesquisa e limites registrados no tema de produtos.

## 21/11/2026 — Continuidade da proposta Turmalina

Investigação por UI, banco MCP e código: a Big Idea da Vega chegou aos agentes; a perda ocorreu na
redução a uma candidata local sem contrato de cobertura integral. Implementação candidata da cadeia
v28/Processo 3 v12 preserva a base Turmalina, histórias, estados de tela, provas e revisões exatas.
Não migrada a execução 102/ciclo 22 nem iniciada inferência paga. Registros e matriz em
`docs/homologacao/turmalina-value-delivery-v1.md`; situação de testes/publicação registrada ali.

Validação Turmalina: 176 verificações selecionadas no backend e suítes dos três workers sem falhas,
com migração MySQL 5.7 e telas desktop/mobile. Bloqueio externo de publicação: comprovação de
revogação/substituição do incidente de credenciais anterior; preparação local concluída sem gasto.

O CI completo identificou ausência dos prompts Turmalina no inventário administrativo dos agentes.
A falha foi reproduzida localmente e os quatro artefatos foram registrados no catálogo existente;
o ajuste permanece no PR #5854, sem alterar o teste que detectou a omissão.

## 22/11/2026 — Continuidade de construção em versão publicada

Preparada correção para manter o ciclo atual quando seu processo histórico foi retirado, com
conferência de compatibilidade e reaproveitamento auditável das entregas. Vega não foi retomado;
dispensa 140, gastos desconhecidos e limites não foram alterados. A capacidade reutilizável evita
recomeçar um produto por mudança de versão do BPM; potencial de reduzir retrabalho, sem resultado
financeiro medido. Homologação e entrega registradas em
`docs/homologacao/vega-process-continuity-2026-11-22.md`.

## 22/11/2026 — Preparação com reaproveitamento parcial

Continuação da solicitação 4001: separação de aceites compatíveis e revalidação pendente,
preservando os artefatos do processo histórico. Vega/ciclo22/execução102 segue pausado,
sem sucessora criada nesta entrega técnica. O contexto dos agentes distingue resultados
históricos de provas aceitas pelo destino. Registro de causa, matriz, limites e evidências:
`docs/homologacao/partial-process-continuity-2026-11-22.md`. Sem mudança de limite, dispensa,
mídia, cobrança ou produção audiovisual; benefício comercial ainda não medido.

Complemento da mesma entrega: revisão do contexto consumido pelo Dédalo identificou uma exceção antiga na correção entre versões. Testes locais primeiro reproduziram os dois desvios; a filtragem pelo recibo mantém apenas aceites compatíveis nas conclusões, conserva a reprovação e entrega os artefatos preservados separadamente. O primeiro PR #5865 já integrado permanece preservado; complemento por PR próprio, sem execução paga do Vega.

### 22/11/2026 — proteção da continuidade Vega

Investigação somente leitura confirmou excesso de contexto na tarefa 922, sem entrega e sem
consumo informado. Correção local adiciona orçamento prévio e recuperação de provas históricas
sem falsa aprovação. Limites financeiros e continuidade permanecem preservados. Ganho esperado:
reduzir interrupções e repetição paga; impacto comercial não medido. Publicação deve ser
conferida no PR específico desta correção; Lab retoma separadamente após a entrega técnica.

Complemento após publicação do PR #5868 (main `acd17621920304a9f5a8639dd4938860ea57b6ce`):
a conferência visual detectou erro bruto em projeções legadas do processo/atividade e síntese
vazia por falta de estado/erro na consulta leve. Correção somente na leitura, com testes da
página inteira e segregação; diagnóstico, histórico, custos e aceites preservados. O executor
já publicado não é repetido nem o Vega retomado. Entrega complementar segue PR próprio.
