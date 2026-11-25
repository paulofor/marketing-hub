# Aprendizado reutilizável — expectativas e surpresa positiva

25/11/2026 — solicitação 4027, sem produto ou autorização comercial específicos.

**Antes:** Psique já recebia orientação sobre novidade segura; Atena possuía biblioteca de ciência
comportamental, e o backend distribuía cartões por curadoria/audiovisual. Não havia ferramenta
transversal de preparação de alternativas nem leitura consultiva ligada à conciliação dos ciclos.

**Mudança v1:** protocolo científico versionado, onze fichas, roteamento contextual para os nove
responsáveis, planejador determinístico para quatro superfícies e três alternativas, e projeção
somente leitura do último recibo comercial do próprio produto/ciclo. Sem agentes, filas ou memória
paralelos. Interface na biblioteca; planejamento não equivale a artefato construído.

**Hipótese de ganho:** reduzir retrabalho de briefings e testes de novidade sem utilidade;
posteriormente comparar tempo até aceite, reincidência de confusão, compras líquidas, entrega e
contribuição. Não há baseline financeiro por surpresa nem aumento de vendas comprovado.

**Prevenção:** contexto irrelevante permanece sem protocolo; fingerprint cobre entrada e versão;
curadoria explícita e quatro cartões preservados. Rotas administrativas não são anunciadas como ferramentas dos executores. Medição bloqueada mais recente impede reciclar
sucesso anterior. Produto/experimento/versão errados, dados de teste e desconhecidos não são prova.
Referência de fonte nula, numérica ou vazia é rejeitada; a revisão preveniu a conversão de `null`
em texto aparentemente preenchido, com regressão determinística desses três casos.

A revisão de encaminhamento reproduziu a ausência de protocolo para a dimensão “Curiosidade e
prazer da descoberta”. O seletor passou a cobrir também curiosidade, originalidade e valor
percebido. O teste percorre os seis eixos declarados, preservando os casos de outros produtos,
agentes não cadastrados e tarefas não relacionadas; a hipótese de ganho comercial permanece aberta.

A conferência da fronteira audiovisual reproduziu outra perda: o adaptador SalesVideo projetava
somente os cinco campos anteriores e descartava a orientação nova. O contrato agora transporta
o campo opcional, preservando o construtor legado. A regressão serializa o contrato real de
projeto e de Apolo para dois produtos distintos, um vídeo anterior não relacionado e agente
desconhecido. O worker já injeta o metadata completo no prompt; não foi necessário criar outro
canal de contexto nem alterar sua autoridade.

O CI 45663811856 revelou que o nome `browser.test.cjs` fazia o Vitest coletar um teste exclusivo
do Node; a execução isolada local reproduziu `No test suite found`. O arquivo passou a
`browser.check.cjs`, ainda obrigatório no percurso HTTP real pelo Node. O aceite local agora
executa toda a suíte da interface para conferir a descoberta real, sem exclusão global nem
enfraquecimento de testes. Esse ajuste trata o encontro entre runners, não uma falha funcional
do fluxo comercial.

**Limites:** prévia não persistida; fontes declaradas ainda dependem de revisão; interpretação
criativa permanece com os agentes. Recibo positivo é observacional e não confirma causalidade.
Adoção comercial depende da medição real posterior e dos limites vigentes do ciclo.

Evidências executáveis e resultados em `docs/homologacao/expectativas-surpresa-v1.md`.
