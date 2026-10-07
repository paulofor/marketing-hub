# Mira — passagem da matriz V3 ao gate — 07/10/2026

## Caso e causa comprovada

Após a entrega do PR 5527 (`989665c277f28711a841962bd207cd178b03f690`), Têmis 633 aprovou
as provas suplementares. O processo de construção 117/execução 53, ciclo 9/experimento 102,
permaneceu bloqueado pela homologação técnica. Todas as demais condições do gate estavam
aprovadas. O fluxo foi pausado pela tela para preservar resultados e consumo de IA.

A tarefa técnica 627 tem `fixtureContract=PDE_DOCUMENTED_INPUT_COMPARISON_V3`, com dezoito
combinações, medições monotônicas e sinais segregados. `PdeInputComparisonScenarioMatrixV1`
reconhecia V3, mas o gate selecionava esse validador somente para V1/V2. V3 caía na regra
histórica de cinco cenários. Não houve falha na candidata privada ou necessidade de nova
inferência de Psique/Têmis. O histórico confirmou cinco cenários sem contrato nas tarefas
349/354/371 e dezoito sob V1 na 608; os caminhos anteriores válidos são preservados.

O teste local carregou os resultados persistidos 627–633 e reproduziu a mesma falha para
produtos 10 e 110, com referências distintas: matriz isolada válida, demais critérios aceitos,
gate técnico recusado. A fixture contém contratos sintéticos já persistidos, sem credenciais,
e nunca representa nova aprovação de produção ou opinião de uma pessoa.

## Alternativas consideradas

| Alternativa | Benefício | Risco e esforço | Decisão |
|---|---|---|---|
| Acrescentar V3 à lista do gate | Menor diff imediato | Mantém listas duplicadas e recorrência na próxima versão | Descartada |
| Centralizar versões no validador existente | Contrato único, pequeno ajuste, compatibilidade explícita | Requer testar roteamento e versões históricas | Adotada |
| Reescrever o despacho de todas as matrizes | Unificação mais ampla | Maior mudança e risco desnecessário ao legado | Descartada |

O gate consulta a política explícita do validador. V1/V2/V3 documentais são validadas integralmente;
versão desconhecida não reutiliza a regra antiga. Cinco provas sem contrato e a matriz estática
V1 de nove provas continuam pelo caminho histórico. Isso não relaxa identidade, segurança,
cronologia, revisão independente, custo zero dos cenários ou segregação dos dados de teste.

## Matriz de aceite local

- Caminho completo: resultados persistidos → gate real → recibo → contexto real → prontidão
  real de Íris → validador do executor de comunicação, sem modelo externo.
- Reutilização: produto 10/experimento 102 e produto 110/experimento 202, mantendo a mesma regra.
- Compatibilidade: V1, V2, V3, cinco cenários sem contrato e nove estáticos antes válidos.
- Falhas: versão desconhecida, sinais incompletos, relógio misturado, chamada de provedor,
  evidência duplicada e provas suplementares anteriores/incompatíveis permanecem recusadas.
- Integração: regressões dos contextos de ciclo, resolvedores de atividade e automação do
  processo, incluindo retomada, fila, subprocesso e passagem sem duplicação.
- Observabilidade: gate e recibo persistíveis preservam IDs, hashes e resultados; testes
  permanecem segregados de métricas comerciais. Não há mídia, compra ou vídeo pago.
- Dispositivos: a prova real 627 conserva desktop, iPhone 15 Pro e Pixel 7. O ajuste altera
  somente validação de contrato; não muda a experiência cuja matriz já foi aprovada.

A melhoria do harness troca a fixture histórica na regressão de passagem pelos contratos reais
V3, preservando seu conteúdo de origem e substituindo identidades somente dentro do teste. Não
cria ferramenta, fluxo ou infraestrutura paralela. O relatório de sete controles e suas seis
fontes não mudaram; não é necessário repetir provas válidas nem criar outra versão da experiência.

O consumo estimado conhecido antes desta correção é US$ 7,9663544, cumulativo sob o teto de
US$ 10 autorizado para preparar e revisar Mira. Registros sem custo informado não são tratados
como fatura zero. A tarefa 627 é determinística, com zero chamadas de provedor. Mercado,
ativação de cobrança, mídia e geração paga de vídeos continuam sem autorização.


## Resultado da validação local

Passaram 445 testes relacionados de backend, gate, contextos, arquitetura e automação, sem
falhas ou skips. O executor de Íris passou 42 testes e manteve um skip de reconexão opcional;
o cenário de entrada exportada pelo gate real foi executado e aprovado. Os 38 testes do
builder e do seletor passaram. O runner de automação consultado passou `bash -n` e ShellCheck.

As novas atestações Mira privada v6, Mira comercial v20 e Alcyone privada v12 preservam os
manifestos anteriores e solicitam somente atualização de evidências do backend. A candidata
continua `mira-private-candidate-v3`, fingerprint
`0a23dd41270f7befd1663a4388bf74b3bbf471fc364f6d6c5b94af8b40f60ac3`,
publicada pelo manifesto comercial v18. O seletor deve retornar `none`, sem deploy PDE.
A entrega segue PR, revisão, merge e workflows do SHA correto; a retomada deve consumir as
aprovações existentes e comprovar a passagem no mesmo ciclo, sem repetir inferência de revisão.


A rodada adicional de passagem entre processos passou 38 testes, totalizando 483 testes
relacionados de backend nesta entrega. Os nove testes de contrato de empacotamento passaram;
o JAR contém as mesmas 4.256 classes compiladas e testadas, com recursos externos íntegros
e catálogo de 519 cartões inicializado. A prova de controles e a matriz real V3 foram
reutilizadas, sem chamar provedor ou repetir cenários já válidos.
