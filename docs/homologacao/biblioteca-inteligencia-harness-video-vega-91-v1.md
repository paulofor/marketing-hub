# Matriz de homologacao — Biblioteca de Inteligencia do Harness v1 / Vega #91

## Objetivo comercial e limite

O piloto deve reduzir retrabalho na criacao e revisao do video do experimento Vega #91 sem tratar
artigo, parecer de agente, retencao ou clique como venda. A decisao comercial continua baseada em
eventos humanos reconciliados: retencao, CTA, checkout, pagamento, entrega e custo.

O piloto nao publica video, nao ativa campanha, nao autoriza provider pago e nao altera o orcamento
do experimento. O teto e os gates financeiros existentes continuam sendo a unica autorizacao de
consumo.

## Alternativas consideradas

1. Injetar todos os artigos nos prompts: menor implementacao inicial, maior custo, latencia, ruido e
   risco de contexto duplicado.
2. Vincular pastas completas por agente: roteamento simples, mas ainda envia textos extensos e
   envelhece sem demonstrar qual achado influenciou a peca.
3. Compilar cartoes curtos e selecionar deterministicamente por agente e contexto: custo limitado,
   fonte e hash auditaveis, reuso entre videos e separacao entre orientacao e resultado comercial.

A terceira alternativa e o contrato do piloto.

## Cenarios ponta a ponta

| Area | Cenario | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Abrir ou criar projeto vinculado ao experimento #91 | A tela apresenta a selecao v1 com rotas separadas para Iris, Apolo, Psique e Temis. |
| Caminho feliz | Salvar outro projeto de video | O mesmo mecanismo seleciona cartoes pelo novo briefing, sem regra exclusiva para Vega. |
| Selecao | Apolo recebe o projeto do Vega | Recebe no maximo quatro cartoes de `video` e `prazer-audio-visual`, com fonte, hash, validade e limites. |
| Selecao | Iris recebe tarefa de comunicacao | Recebe somente sua rota curta de `neuromarketing` e `momentos-de-compra-b2c`. |
| Revisao | Psique ou Temis recebe tarefa | Artigos entram apenas como criterio consultivo; o artefato real e os eventos continuam sendo a fonte da decisao. |
| Auditoria | O ciclo autorizado cria job de Apolo | O metadata persistido registra cartoes entregues; o plano registra os IDs efetivamente aplicados. |
| Validacao | A IA cita cartao inexistente | O gate bloqueia antes do provider pago. |
| Validacao | A IA ignora todos os cartoes entregues | O gate bloqueia antes do provider pago. |
| Validade | Um cartao ultrapassa sua data limite | O backend deixa de entrega-lo aos agentes, sem depender da IA para percebe-lo. |
| Validacao | Artigo novo ou alterado entra no pacote | O catalogo recalcula path, data e SHA-256 a partir do Markdown versionado. |
| Falha | Biblioteca ausente ou ilegivel | O backend falha com causa explicita; nao monta contexto ficticio. |
| Falha | Artigo sem secao semantica completa | O cartao declara a lacuna e permanece inspiracao externa, sem inventar mecanismo causal. |
| Integracao | Backend entrega tarefa a worker | O contrato traz objeto tipado, sem JSON serializado dentro de string adicional. |
| Integracao | OpenAI planeja storyboard | Usa JSON Schema estrito, modo Flex e preserva request e response brutos no job. |
| Observabilidade | Operador abre o Estudio | Ve colecoes, cartoes, motivo de selecao, fontes, validade, autoridade e limite de uso. |
| Metricas | Video recebe trafego humano | Hub separa play/retencao/CTA/checkout/pagamento/custo por versao; nenhum sinal intermediario vira venda. |
| Segregacao | Preview, teste automatizado ou fixture | Nao entra nas metricas comerciais humanas do experimento. |
| Desktop | Chromium desktop | Painel legivel, sem overflow e sem bloquear edicao do projeto. |
| Mobile | iPhone 15 Pro e Pixel 7 | Cartoes e rotas permanecem legiveis e controles mantem area de toque adequada. |

## Criterios de decisao

- **Continuar:** menos correcoes por video ou melhora de retencao, CTA, checkout ou pagamento sem
  aumento desproporcional de tokens, prazo ou custo.
- **Ajustar:** melhora dos gates internos sem melhora no comportamento humano, ou selecao ainda
  pouco aderente ao briefing.
- **Parar:** maior custo/latencia/retrabalho sem melhora mensuravel, contaminacao de autoridade ou
  qualquer artigo tratado como prova de venda.
