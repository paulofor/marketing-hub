# Modulo de Planejamento Comercial

## Objetivo

Criar uma camada de planejamento acima dos pipelines atuais do Marketing Hub para manter o desenvolvimento e a operacao focados no objetivo principal do sistema: criar e vender produtos digitais com valor percebido.

O modulo de Planejamento Comercial nao deve ser mais um pipeline periferico. Ele deve responder, de forma simples e objetiva:

- qual objetivo comercial esta ativo;
- ate quando esse objetivo precisa gerar evidencia;
- qual produto, oferta, publico e canal estao sendo testados;
- qual metrica decide avancar, pausar, corrigir ou encerrar;
- qual proxima acao aproxima o Marketing Hub de venda;
- qual bloqueio esta impedindo o avanco.

## Diagnostico que justifica o modulo

Os documentos atuais indicam que o fluxo comercial correto ja existe conceitualmente:

```text
NichoCNAE -> Hipotese -> Experimento -> Campanha -> Landing -> Publicacao -> Metricas -> Aprendizado
```

Porem, a operacao ainda pode se perder em etapas tecnicas, geracao de artefatos e correcoes perifericas sem uma camada que cobre prazo, objetivo e decisao comercial.

Evidencias no repositorio:

- `docs/canonical/procedimento-experimento-canon.v1.md` define que o experimento deve preservar uma dor principal, uma isca principal, um produto low-ticket de entrada, uma promessa principal e um CTA principal.
- `docs/canonical/pipeline-operacional-canon.v1.md` reforca que pipeline existe para avancar venda ou decisao comercial, nao apenas executar jobs.
- `docs/marketing/pipeline-nichocnae-v3.md` limita o NichoCNAE a pesquisa de rotina, dores e evidencias, sem gerar oferta prematura.
- `docs/relatorios/experimentos/analises/analise-campanhas-resultados-fracos.md` mostra que campanhas tiveram cliques, mas 575 visualizacoes de formulario somadas e 0 envios, apontando quebra de funil e falta de criterio claro antes de escalar.

A causa-raiz operacional nao e apenas ausencia de mais etapas. E ausencia de um centro de decisao que conecte cada etapa ao objetivo final de venda.

## Principio central

Todo plano comercial deve existir para validar uma pergunta de negocio em prazo definido.

Frase obrigatoria de todo plano:

```text
Ate a data X, queremos validar se o publico Y compra ou demonstra intencao clara pela oferta Z, usando o canal W, medindo a metrica M.
```

Se essa frase nao puder ser preenchida, o plano nao deve ser iniciado.

## Escopo da primeira versao

A primeira versao deve ser simples e eficaz:

- criar uma tela "Planejamento";
- criar apenas um tipo de plano: `Plano de Primeira Venda`;
- permitir vincular nicho, hipotese e experimento;
- controlar objetivo, prazo, metrica principal, criterio de sucesso e criterio de parada;
- mostrar checklist dos marcos comerciais;
- exibir a proxima acao mais importante;
- simular cenarios futuros antes de aprovar a proxima acao relevante;
- registrar bloqueios e causa-raiz;
- gerar um relatorio simples de foco: estamos avancando para venda ou desviando para periferia?

## Entidade conceitual: Plano Comercial

Campos recomendados:

| Campo | Objetivo |
|---|---|
| Nome do plano | Identificar o plano de forma simples. |
| Tipo do plano | Na primeira versao: `Primeira Venda`. |
| Objetivo comercial | Exemplo: gerar a primeira venda low-ticket no nicho escolhido. |
| Publico alvo | Pessoa ou nicho que sera testado. |
| Dor principal | Dor unica que orienta hipotese, experimento, anuncio e landing. |
| Oferta principal | Produto low-ticket ou oferta de entrada a validar. |
| Isca principal | Recompensa usada para capturar interesse inicial, quando aplicavel. |
| Canal principal | Exemplo: Meta Ads, Instagram, WhatsApp, trafego organico. |
| Metrica principal | Venda, lead qualificado, envio de formulario ou outro sinal comercial forte. |
| Criterio de sucesso | Condicao objetiva para considerar o ciclo promissor. |
| Criterio de parada | Condicao objetiva para pausar ou reprovar a execucao. |
| Prazo final | Data limite para gerar evidencia. |
| Orcamento maximo | Limite de gasto autorizado para o plano. |
| Status | Rascunho, em andamento, bloqueado, concluido, cancelado. |
| Proxima acao | A acao mais importante para aproximar o plano de venda. |
| Bloqueio atual | Causa que impede avanco, quando houver. |
| Cenario mais provavel | Resultado esperado se o plano continuar como esta. |
| Principal risco futuro | Ponto onde o plano tende a quebrar ou gerar conclusao errada. |
| Acao a evitar | Acao que aumentaria esforco ou custo sem melhorar a evidencia comercial. |

## Marcos do plano

O plano deve organizar os pipelines existentes em marcos de negocio:

1. Nicho aprovado
2. Hipotese aprovada
3. Oferta low-ticket definida
4. Experimento criado
5. Campanha criada
6. Landing aprovada
7. Publicacao validada
8. Resultado analisado
9. Proxima decisao tomada

Cada marco deve ter:

- status;
- prazo;
- origem da evidencia;
- bloqueio, se existir;
- proxima acao recomendada.

## Gates de foco em venda

Antes de avancar em qualquer marco relevante, o modulo deve verificar:

- isso aproxima o Marketing Hub de publicar, vender ou aprender algo comercialmente valido?
- existe oferta definida?
- existe produto de entrada ou proxima oferta clara?
- existe uma dor principal, e nao varias dores misturadas?
- existe um CTA principal?
- existe metrica de decisao?
- existe prazo?
- existe criterio de parada?
- a proxima etapa resolve causa-raiz ou apenas uma consequencia?

Se a resposta for negativa em ponto critico, o plano deve ficar bloqueado ate a decisao ser corrigida.

## Simulacao de cenarios futuros

O Planejamento Comercial deve usar IA para simular cenarios provaveis antes de aprovar uma proxima acao relevante. A funcao nao deve tentar "prever o futuro"; deve antecipar possibilidades praticas para reduzir desperdicio de tempo, midia e desenvolvimento.

Pergunta central:

```text
Se executarmos esta proxima acao, qual cenario estamos provavelmente criando?
```

A simulacao deve comparar impacto esperado, risco, custo, prazo e evidencia comercial provavel.

Cenarios padrao:

| Cenario | Uso pratico |
|---|---|
| Conservador | Poucos cliques, baixa conversao e aprendizado limitado. Serve para definir criterio de parada. |
| Provavel | Resultado mais esperado com base nos dados atuais. Serve para decidir orcamento, prazo e proxima acao. |
| Otimista realista | Boa resposta do publico, sem assumir milagre. Serve para preparar continuidade se aparecer sinal positivo. |
| Risco | Mostra onde podemos interpretar errado o mercado. Exemplo: clique barato com formulario ainda sem envio. |

Entradas minimas da simulacao:

- objetivo comercial do plano;
- publico, dor, oferta, isca e canal;
- metrica principal;
- prazo restante;
- orcamento disponivel;
- marcos ja concluidos;
- bloqueios conhecidos;
- metricas historicas de experimentos vinculados;
- aprendizados anteriores do mesmo nicho, canal ou tipo de oferta.

Saida obrigatoria da simulacao:

- cenario mais provavel;
- melhor cenario realista;
- pior cenario provavel;
- principal risco;
- melhor proxima acao;
- acao que deve ser evitada;
- condicao para continuar;
- condicao para parar;
- evidencia comercial esperada em 7, 14 e 30 dias, quando o prazo permitir.

Regra de uso:

```text
Antes de aprovar uma proxima acao relevante, o Planejamento Comercial deve gerar uma simulacao simples de cenarios futuros e registrar a decisao tomada.
```

Essa capacidade e especialmente importante porque os relatorios atuais mostram campanhas com trafego e sem conversao: 575 visualizacoes de formulario somadas e 0 envios. Nesse contexto, o modulo nao deve recomendar automaticamente mais criativos, mais campanhas ou mais landings. Ele deve testar primeiro se o gargalo provavel esta em formulario, promessa, oferta, canal, publico ou mensuracao.

## Decisoes apoiadas por simulacao

A simulacao deve ajudar a responder:

- se mantivermos este canal e esta oferta, qual resultado provavel em 7, 14 e 30 dias?
- onde o plano tende a quebrar: anuncio, landing, formulario, oferta, preco, entrega ou publico?
- qual pequena acao aumenta mais a chance de venda?
- o que nao devemos fazer agora porque so aumentaria esforco sem gerar evidencia?
- o plano esta criando aprendizado comercial ou apenas atividade operacional?

Exemplos de decisao:

| Situacao | Decisao recomendada |
|---|---|
| Cliques existem, mas formulario segue com 0 envio | Validar formulario, promessa e friccao antes de criar nova campanha. |
| Baixo clique e baixa leitura de landing | Revisar angulo, publico e promessa antes de mexer no produto. |
| Lead existe, mas nao compra | Revisar oferta, preco, prova e mecanismo de entrega. |
| Gasto cresce sem nova evidencia | Pausar e corrigir causa-raiz antes de escalar. |
| Sinal positivo pequeno aparece | Continuar com limite de orcamento e criterio claro de proximo marco. |

## Tela principal

A tela de Planejamento deve responder rapidamente:

```text
O que precisamos fazer agora para chegar mais perto de vender?
```

Blocos recomendados:

- objetivo comercial ativo;
- dias restantes;
- status geral;
- proxima acao mais importante;
- gargalo atual;
- cenario mais provavel;
- melhor proxima acao simulada;
- acao a evitar;
- marcos do plano;
- experimentos vinculados;
- metricas principais;
- historico de decisoes.

Nao deve ser uma tela tecnica de pipeline. Deve ser uma tela de direcao comercial.

## Relatorio do plano

O relatorio deve ser gerado com dados persistidos, nao por interpretacao de logs tecnicos.

Perguntas que o relatorio deve responder:

- o plano ainda esta no caminho da venda?
- qual marco esta travando?
- a causa-raiz e tecnica, estrategica, de oferta, de canal, de landing ou de mensuracao?
- qual foi a evidencia comercial ate agora?
- o prazo ainda e realista?
- devemos continuar, corrigir, pausar ou encerrar?
- qual cenario futuro mais provavel se nada mudar?
- qual acao deve ser evitada para nao desperdicarmos esforco ou midia?

## Regras de decisao

Regras iniciais recomendadas:

- nao publicar campanha sem metrica principal e criterio de parada;
- nao interpretar mercado quando a execucao estiver tecnicamente invalida;
- nao considerar CPL como `R$0,00` quando nao houve leads;
- nao iniciar landing se a hipotese nao tiver dor, promessa, isca, produto de entrada e CTA principal;
- nao criar novo artefato quando o bloqueio atual for conversao, formulario, entrega ou oferta;
- priorizar sempre a menor acao capaz de gerar evidencia de compra ou intencao clara.
- nao aprovar proxima acao relevante sem simulacao de cenario futuro registrada;
- nao escalar midia quando o cenario de risco indicar quebra provavel em formulario, oferta ou mensuracao.

## Integracao com os pipelines existentes

O Planejamento Comercial deve mandar por prioridade de negocio, mas nao substituir os pipelines.

Responsabilidade dos pipelines:

- NichoCNAE: descobrir publico, rotina, dores e evidencias.
- Hipotese: transformar dor, resultado, mecanismo, prova e oferta em proposta testavel.
- Experimento: definir variavel comercial, isca, produto, CTA e metrica.
- Campanha: gerar angulo, copy e briefing de imagem.
- GeraLanding: criar landing para capturar interesse e validar promessa.
- Publicacao e metricas: colocar o teste no mercado e medir resultado.

Responsabilidade do Planejamento:

- definir por que esses pipelines estao sendo executados;
- definir prazo e meta;
- vincular as entregas a um objetivo comercial;
- impedir avanco sem criterio de decisao;
- mostrar o gargalo;
- orientar a proxima acao de maior impacto.
- simular cenarios futuros antes de decisoes relevantes.

## Plano de implementacao da simulacao

A implementacao deve ser incremental para gerar valor rapido sem criar complexidade desnecessaria.

### Fase 1: simulacao manual assistida

Objetivo: permitir que o usuario solicite uma simulacao para um plano ativo.

Entregas:

- adicionar acao "Simular cenarios" na tela do plano;
- usar dados ja persistidos do plano, experimento e metricas vinculadas;
- gerar os quatro cenarios padrao;
- salvar a simulacao no historico do plano;
- mostrar recomendacao objetiva: continuar, corrigir, pausar ou encerrar.

Criterio de pronto:

- o usuario consegue ver o cenario mais provavel, principal risco, melhor proxima acao, acao a evitar, condicao de continuidade e condicao de parada.

### Fase 2: simulacao obrigatoria antes de decisao relevante

Objetivo: impedir decisoes comerciais sem avaliacao minima de futuro provavel.

Entregas:

- exigir simulacao antes de aprovar publicacao, escalar orcamento, criar nova landing, criar nova campanha ou encerrar o plano como sucesso;
- registrar qual decisao foi tomada apos a simulacao;
- registrar quando o usuario decidir seguir contra a recomendacao;
- destacar risco comercial quando houver trafego sem conversao.

Criterio de pronto:

- toda decisao relevante do plano tem uma simulacao vinculada e uma justificativa objetiva.

### Fase 3: simulacao baseada em historico

Objetivo: tornar a previsao mais util usando aprendizados acumulados.

Entregas:

- comparar o plano atual com experimentos anteriores do mesmo nicho, canal, tipo de oferta e metrica;
- usar aprendizados registrados para indicar gargalos recorrentes;
- apontar risco de repeticao de erro, como aumentar campanha quando o problema real e formulario, oferta ou promessa;
- sugerir menor teste capaz de confirmar ou descartar a causa-raiz.

Criterio de pronto:

- a simulacao cita evidencias historicas usadas e diferencia hipotese de fato observado.

### Fase 4: simulacao no relatorio executivo

Objetivo: transformar a simulacao em apoio continuo de gestao.

Entregas:

- incluir bloco de "Cenarios futuros" no relatorio do plano;
- mostrar tendencia em 7, 14 e 30 dias quando aplicavel;
- apresentar impacto esperado, risco, custo, prazo e evidencia provavel;
- recomendar proxima decisao em linguagem simples de negocio.

Criterio de pronto:

- o relatorio permite decidir o proximo movimento sem consultar logs tecnicos ou reconstruir contexto manualmente.

## Primeira entrega recomendada

Para uma primeira implementacao futura, o menor produto util do modulo e:

1. cadastro de Plano de Primeira Venda;
2. vinculo com nicho, hipotese e experimento;
3. checklist dos marcos;
4. prazo, meta, orcamento e metrica principal;
5. campo de proxima acao;
6. campo de bloqueio e causa-raiz;
7. acao manual para simular cenarios futuros;
8. relatorio simples de foco comercial com cenario mais provavel, risco principal e acao recomendada.

Essa versao ja resolve o principal problema: impedir que o Marketing Hub trabalhe em etapas soltas sem saber se esta mais perto de vender.

## Exemplo de plano

```text
Plano: Primeira venda para manicures a domicilio
Objetivo: Validar interesse comercial por um kit low-ticket de mensagens e rotina de confirmacao de agenda.
Prazo: 14 dias
Publico: manicure autonoma que atende por WhatsApp e sofre com cliente que some
Dor: agenda vulneravel por falta de confirmacao e retorno
Oferta: kit low-ticket de mensagens prontas, follow-up e rotina simples de confirmacao
Isca: 5 mensagens prontas para confirmar horario sem parecer grossa
Canal: Meta Ads
Metrica principal: envio valido de formulario ou primeira venda
Criterio de sucesso: 1 venda ou 3 leads qualificados com intencao clara
Criterio de parada: 100 acessos validos sem envio ou falha tecnica de formulario
Proxima acao: validar formulario mobile ponta a ponta antes de publicar
```

## Criterio de pronto do modulo

O modulo pode ser considerado pronto para uso inicial quando a tela conseguir mostrar, para cada plano ativo:

- objetivo comercial;
- prazo;
- oferta;
- metrica principal;
- etapa atual;
- bloqueio;
- proxima acao;
- decisao recomendada.

Sem esses pontos, o modulo corre o risco de virar apenas uma lista de tarefas, e nao um mecanismo de foco em venda.
