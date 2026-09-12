# Card de ciclo — homologação isolada

As fixtures de posição e contexto reproduzem os contratos somente leitura observados no Vega
em 10/09/2026. A fixture da atividade conserva a disponibilidade e os motivos do backend, mas
remove o conteúdo das tarefas e bloqueia comandos. Os produtos do navegador são sintéticos,
identificados como **QA local**. Nenhuma fixture representa uma nova execução produtiva.

`browser.cjs` serve o build real do frontend em uma porta local temporária, intercepta todas as
APIs com respostas locais, proíbe escrita e bloqueia conexões externas. Valida início, catálogo
e o painel do processo em desktop, iPhone e Pixel. Desde 12/09/2026, reutiliza o roteiro
`product-next-activity/browser.cjs`, que também cobre Rigel e o processo coordenador do ciclo. Chromium mobile é emulação, não Safari/iOS físico.

Para reproduzir a matriz: `bash infra/testing/vega-cycle-card/run-round.sh <rodada>`.
Não é necessário Docker ou banco local: esta alteração reutiliza os endpoints existentes,
sem mudar regra Java, persistência ou executor. A integração do navegador usa test doubles
dos contratos HTTP; a situação real foi previamente conferida pelo MCP e pela tela pública.
