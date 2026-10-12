# Captura complementar de Capella sem invalidar o produto — 2026-10-12

Antes de iniciar nova tarefa técnica, a leitura de IrisLearningCycleContext mostrou
que a tentativa posterior substituiria a prova homologada #685 e invalidaria o gate
já aceito. Não foi criada a tarefa: o formulário havia sido apenas preparado.

Reabrir a homologação completa repetiria pareceres e custo; limitar-se às capturas
antigas deixaria briefing e calendário sem demonstração; separar a finalidade na
referência existente preserva a prova e reutiliza o executor. A terceira opção
usa `experiment:<id>@video-proof-v1`, somente na homologação determinística de kits.
Não cria processo, tabela, agendamento ou aprovação comercial.

Matriz antes dos testes:

- Tela envia o contexto separado apenas por seleção explícita; tarefa comum mantém
  sua referência. Duas identidades e desktop/iPhone/Pixel.
- Consulta real de histórico mantém a prova técnica original mesmo com captura
  complementar posterior em andamento. Gate e pareceres permanecem nas referências
  canônicas, sem alteração de resultado/hash antigo.
- Worker aceita somente TECHNICAL e kit executável com linhagem exata; conserva os
  quinze momentos reais, sessão, arquivo e hash. Não chama modelo.
- Compositor exige resultado aprovado, contrato de momentos, mesmo produto/versão,
  experimento e tenant, mais gate original ainda válido. Gate ausente/alterado,
  versão ou produto diferente bloqueiam antes de consumo.
- Local: usar os relatórios reais dos dois perfis como fixtures do callback e testar
  isolamento JPA, interface, modos antigos e consumidores. Produção somente após PR.

A mudança permite corrigir o vídeo sem renovar parecer do produto. Métrica posterior:
menos revisões repetidas para a mesma versão; impacto comercial ainda não medido.

## Validação local concluída

- Backend: 286 testes, incluindo isolamento JPA real entre referência canônica e
  complementar, contrato de consumo e arquitetura; nenhuma falha.
- Psique: 183 testes, nove condicionais sem execução e nenhuma falha. O teste
  PrivateKitHarnessContractTest aceitou os relatórios reais de dois perfis com
  quinze capturas e recusou modo funcional ou experimento incorreto. Os dados são
  fixtures locais segregadas; não são novas provas de produção ou vendas.
- Frontend: oito testes, verificação TypeScript e build aprovados. Playwright
  confirmou envio único da referência complementar em desktop, iPhone 15 Pro e
  Pixel 7, sem erros de página ou transbordamento. As chamadas foram interceptadas
  localmente; nenhum teste criou tarefa produtiva.
- Spotless verificou os dez arquivos Java alterados; Prettier e diff sem erros.

A atestação sucessora Mira v23 preserva v22 e atualiza somente o hash do runner
compartilhado. Captura complementar continua limitada ao kit privado; os percursos
anteriores de Mira/Vega/Alcyone permanecem cobertos pela suíte do executor.
`automaticDeployOnMerge=false` e nenhuma versão do produto muda neste ajuste.


## Continuidade do kit na aprovação audiovisual — 2026-10-12

A tarefa complementar #732 foi recusada em `/internal/sessions` (HTTP 409), antes
de abrir o navegador ou executar IA. O ciclo #12 permanece OPEN/VIDEO_APPROVAL,
com os contratos da versão v3 preservados; a tarefa #685 havia homologado a mesma
versão antes da etapa audiovisual. `KitPrototypeCapabilities` ainda permitia
somente ADJUSTMENT/VALIDATION, divergindo da política já corrigida no contexto de
Íris (LOOP-APROVACAO-VIDEO-REJEITA-PROVA-DO-CICLO).

Recuar o ciclo reescreveria sua posição; usar somente as capturas antigas deixaria
as ações narradas sem prova; reconhecer as quatro etapas audiovisuais canônicas
na capacidade privada mantém a versão verificável. Escolhida a terceira opção,
sem mudar contratos, pareceres, limites ou autorização comercial.

Matriz antes da validação:

- Capella e outro perfil: seis etapas privadas, incluindo VIDEO_APPROVAL e o
  percurso anteriormente válido, mantêm capacidade e consulta do contexto exato.
- Tentativa posterior bloqueada, baseline, ciclo encerrado, planejamento e etapas
  comerciais continuam recusados antes de expor a sessão.
- Backend e compositor reais sobre MySQL 5.7 local, com o primeiro produto em
  VIDEO_APPROVAL e outro em CAMPAIGN_VIDEO; terceiro controle em ADJUSTMENT.
- Capturas de entrada/conteúdo/resultado em desktop, iPhone e Pixel; recuperação,
  isolamento, callback e ausência de efeitos externos conferidos pelo harness.
- Spotless deve realmente selecionar os arquivos: o filtro do runner local usa
  uma expressão regular, não uma lista separada por vírgulas.

Adoção: somente após os testes e a captura publicada da mesma versão. Reversão se
alguma etapa comercial ou contrato rejeitado passar a abrir a capacidade privada.
Impacto em conversão permanece desconhecido; o ganho verificável é manter a prova
executável quando o produto avança à preparação dos vídeos.

### Resultado da validação da etapa

A regressão inicial falhou nas quatro fases audiovisuais; com a lista canônica,
os 25 casos da capacidade passaram. A rodada relevante do backend totalizou
144 testes, incluindo 92 de arquitetura, sem falhas. O fluxo real local passou
18 percursos, seis callbacks de Psique, dois fluxos Têmis/gate e nove grupos de
controles; cada produto conservou quinze capturas. MySQL 5.7 aplicou a fixture
incremental; nenhum changelog anterior foi alterado. Topologia temporária removida.

Spotless do conjunto selecionado, bash -n, ShellCheck e diff passaram. Os 38 testes
do pacote/seleção e os 520 arquivos/113 manifestos permaneceram íntegros, sem nova
atestação ou mudança da versão do produto. Pareceres por modelo foram simulados
somente nessa validação local; não representam aprovação dos novos vídeos.


## Autorização da prova produzida pelo ciclo — 2026-10-12

A captura #733 concluiu nove cenários, quinze capturas e zero chamadas de provedor.
Os oito resultados canônicos permaneceram com os mesmos hashes. A pós-produção
recusou a captura (HTTP 409): o adaptador exigia `visualProofAuthorization`, mas
`IrisLearningCycleContext` só entregava o gate e o destino; o produtor do primeiro
experimento já emitia a autorização. Os testes anteriores simulavam esse campo e
não ligavam o produtor real ao consumidor.

Retirar a validação enfraqueceria o isolamento; consultar persistência de outro
módulo criaria um segundo caminho; emitir o contrato existente no produtor do ciclo
preserva a mesma autoridade. Escolhida a terceira alternativa, após as verificações
de gate, identidade e fase que já existem. Nenhum parecer é renovado por essa mudança.

Matriz da correção, antes da validação:

- Produtor real do ciclo → adaptador real de agentes → serviço real de prova do vídeo
  → leitura dos bytes por storage substituto local. Capella e outra identidade,
  em ADJUSTMENT e VIDEO_APPROVAL; nenhuma autorização simulada entre os módulos.
- Produto, versão ou experimento divergente, gate reprovado e ciclo encerrado
  recusam a montagem; nenhuma escrita ou consumo de IA no teste.
- Preservar contratos do experimento inicial, captura comum, recuperação da voz,
  montagem híbrida e arquitetura. Reutilizar os MP4 locais já renderizados com
  capturas reais #721/#722, sem repetir a matriz de navegador sem motivo.

Critério de adoção: sequência e leitura privadas aprovadas pelos contratos reais,
seguidas de nova revisão independente dos MP4 finais. Impacto comercial desconhecido.


### Resultado da validação do contrato visual

As quatro regressões de passagem falharam com HTTP 409 antes da correção.
Após emitir autorização e a lista de capturas do gate, os 218 testes relevantes
do backend passaram, incluindo arquitetura, leitura visual existente, contexto
inicial, contexto de ciclos e recuperação da voz. O teste atravessa os módulos
sem simular o campo antes omitido; não há escrita produtiva nem chamada de IA.

A atestação sucessora Mira v24 conserva v23 e atualiza somente os dois hashes de
código/teste compartilhados. A experiência de Mira permanece na mesma versão e
`automaticDeployOnMerge=false`. Os 38 testes de pacote e seleção passaram; o pacote
de revisão foi gerado sem divergência. A aprovação dos novos vídeos continua
dependente dos pareceres próprios sobre os MP4.
