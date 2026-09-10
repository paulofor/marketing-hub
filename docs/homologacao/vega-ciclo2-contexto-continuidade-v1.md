# Vega — contexto e continuidade do segundo ciclo

Data: 10/09/2026. Escopo: mostrar a passagem atual e o aprendizado anterior nas atividades,
corrigir a seleção do contrato de construção e executar a próxima atividade pela interface.

## Evidência inicial

MCP e UI confirmaram ciclo #2, experimento #92, etapa ADJUSTMENT. Processo 2 concluído nas
tarefas #359, #361 e #362; atividade 3.1 concluída na #366, após bloqueio técnico na #365.
O ciclo ainda apontava para 2.3. A tarefa #368, criada pela UI para 3.2, repetiu o bloqueio
antes do modelo: contrato PDE ausente. A #366 recebeu o contrato privado completo do #92,
mas o cadastro comercial do produto continua com contrato `v1` e experiência histórica v7.
A comparação do prompt bem-sucedido, código e dados confirma seleção inadequada da fonte.

## Alternativas e decisão

| Alternativa para o contrato | Benefício | Risco / esforço | Aderência e escolha |
| --- | --- | --- | --- |
| Evoluir o cadastro do produto com snapshot imutável da v7 | Reaproveita o caminho existente de construção | Exige conciliar todos os consumidores do cadastro; esforço alto e risco de misturar versões | Viável, mas amplia o escopo para preparar este sucessor |
| Persistir outro artefato consolidado por ciclo | Oferece contrato independente para os executores | Introduz nova sincronização entre parecer e artefato; esforço médio | Útil para uma materialização futura, com mais pontos de consistência |
| Compor o contexto no backend a partir das aprovações canônicas | Preserva identidade e referências sem nova inferência ou cópia editável | Exige validação de responsáveis, estado e ordem; esforço menor | **Escolhida**: menor custo e aprendizado rastreável, bloqueando dados incompletos |

| Alternativa para orientação | Benefício | Risco / esforço | Aderência e escolha |
| --- | --- | --- | --- |
| Indicador do ciclo com link para detalhes | Ocupa pouco espaço; esforço baixo | Aprendizado e próximo passo continuam a outra navegação | Adequada para listas compactas |
| Aba de contexto no processo | Agrupa memória e decisões; esforço médio | Informação essencial pode ficar escondida em outra aba | Adequada para histórico extenso |
| Contexto integrado à tela de atividades | Mostra passagem, memória e próximo trabalho no ponto de execução | Requer limitar o conteúdo e validar mobile; esforço médio | **Escolhida**: reduz dúvida e evita repetir atividades sem aprendizagem |

## Matriz definida antes da implementação

| Dimensão | Aceite |
| --- | --- |
| Contrato | Sucessor recebe estratégia, economia, arquitetura, hipótese, fontes e versão do próprio experimento |
| Falhas e isolamento | Contrato ausente, rejeitado, de outra cadeia/produto/experimento ou anterior ao ciclo não é usado; histórico e checkout produtivo não vazam |
| Continuidade | Processo 2 concluído aponta à primeira atividade pendente do Processo 3; bloqueio é visível; retornos preservados; navegação não cria tarefas |
| Tela | Número do ciclo distinto da versão BPM; predecessor, hipótese, evidências e limitações legíveis; comando conserva ciclo e cadeia |
| Métricas | Tarefas/custos da passagem selecionada separados do histórico; testes sem tráfego, venda ou cobrança produtivos |
| Integração | Backend real local com dependências controladas; respostas dos agentes simuladas; teste do contrato no worker |
| Dispositivos | Chromium desktop e emulações iPhone 15 Pro e Pixel 7, sem erro JS nem transbordamento |
| Regressão | Testes relevantes backend/worker/frontend, tipagem, build, formatação, revisão do diff |
| Operação | Só depois da validação local, retentar pela UI; confirmar conclusão e próxima atividade no banco e na tela |

Após a última correção, executar duas rodadas locais completas consecutivas sem falhas.
Registrar resultados e limitações reais; não usar publicação como mecanismo de teste.

## Execução local

- Rodada `accepted1`: 2.607 testes backend (cinco ignorados preexistentes), 58 testes Dédalo e
  539 testes frontend, sem falhas. MySQL 5.7: dez controles de identidade, memória, continuidade,
  bloqueio e segregação; três perfis Chromium (desktop, iPhone 15 Pro, Pixel 7).
- Tipagem, build, formatação e diff aprovados. O contrato produzido pelo backend foi validado pelo
  consumidor real de Dédalo para jornada, componentes e acesso, incluindo recusa de versão
  incompatível. O mesmo validador aceitou a composição dos pareceres reais #359/#361/#362,
  consultados somente para leitura, sem nova chamada ao modelo.
- A preparação revelou duas lacunas adicionais, corrigidas antes destas rodadas: dependência
  de pesquisa ausente na aplicação de teste e possibilidade de reaproveitar aprovação antiga
  quando uma tentativa mais recente estivesse bloqueada. A última tentativa precisa estar
  concluída pelo responsável correto; execução pendente, bloqueada ou cancelada não é aprovação.
- A aplicação funcional local utiliza MySQL real com produto `91001`, experimentos `91001/91002`
  e ciclos `92001/92002`. Navegar não gravou tarefas, tráfego, pedido, mensagem ou custo externo.
- O backend empacotado contém as classes novas. A imagem usa o Dockerfile do repositório,
  Java 21 e exatamente o JAR validado (SHA-256 abaixo). A inspeção de imagem não substitui os
  testes funcionais da aplicação local; o processo principal do container de inspeção fica parado.

Comandos reproduzíveis: `infra/testing/vega-cycle-context/run-round.sh`,
`integration.py`, `browser.cjs` e `images.compose.yml`. Os logs e screenshots ficam em
`artifacts/vega-cycle2/` (ignorados pelo Git; sem credenciais).

## Limite de interpretação comercial

O contrato de uma tarefa concluída por Dédalo não comprova, isoladamente, software publicado ou
experiência utilizável. A #366 registrou desenho da jornada em modo somente leitura. A atividade
3.2 chama-se **Produzir componentes do protótipo PDE**; o artefato e suas pendências devem ser
conferidos antes de comunicar o alcance da conclusão. Homologação e autorização do #92 continuam
independentes, com eventos separados do #91 e sem inferir causa de abandono a partir da amostra.

## Segunda rodada e artefatos finais

A rodada `accepted2` repetiu integralmente os mesmos controles: 2.607 testes backend (cinco
ignorados preexistentes), 58 Dédalo, 539 frontend, dez controles MySQL, três perfis de navegador,
tipagem, build, formatação e diff. Zero falhas. A imagem do frontend foi aberta adicionalmente
nos três perfis contra a API local, preservando identidade, memória, navegação e isolamento.

Base: `36ded1f97742a1783f6896bdd710b108547a4678` (sincronizada por fast-forward, sem commit criado nesta solicitação).

Fingerprint dos fontes alterados de runtime: `ade2c3dbb283b201663b9dfda7df2ea7d0598256e59e24004e8c8ae63c5f29db`.

JAR: `433139d58a59325c351f64e8eb75b8e653f158d8d3c4d324935a008886df5c59`.

Imagens: `marketinghub-backend:vega-cycle2-ade2c3dbb283` e `marketinghub-frontend:vega-cycle2-ade2c3dbb283`, criadas somente com os Dockerfiles versionados.

A configuração produtiva foi conferida sem alteração antes da transferência: 34 variáveis,
cinco mounts e duas portas do backend; seis variáveis e uma porta do frontend. A conferência
não imprime valores de credenciais. A aplicação da troca exige os IDs das imagens revisadas;
se o runtime mudar entre revisão e execução, o comando para antes de alterar os serviços.

## Resultado operacional pela tela e pelo MCP

- Imagens aplicadas no host `191.252.181.168` com a autorização já existente para recuperar o
  fluxo de Vega. Backend e frontend saudáveis; configuração, mounts, portas e versões de retorno
  preservados. Backend: `sha256:c8374dad746e18128d178ea3c0aa523b0cada8c0376cd54e2c00ad2980b7991b`;
  frontend: `sha256:1277dbaa3855e8c4169044c933524c45108a67c752db09c1ae9b098b9d5de79a`.
- O comando **Reiniciar tarefa** da atividade 3.2 criou a **#369** pela interface. A #368 ficou
  bloqueada no histórico, sem reescrita de seu resultado. Nenhum registro de conclusão foi
  inserido por SQL ou fabricado por chamada administrativa.
- #369: `COMPLETED`, parecer `READY`, sem `requiredChanges` ou erro. Instância BPM #232:
  `COMPLETED`, `objective_achieved=true`, fonte `experiment:92`; encerramento registrado em
  `2026-09-10T09:16:14.651703`, conforme banco. Uso reportado: 39.759 tokens de entrada, 10.542 de
  saída, custo estimado US$ 0,369876, modelo `gpt-5.6-sol`.
- O prompt preservou a versão alvo v8, `inheritedLearning`, os IDs de estratégia/economia/
  arquitetura e a fonte `experiment:92`. Dédalo reconheceu a jornada #366 e a presença do contrato
  aprovado; o bloqueio histórico por contexto ausente não voltou a ocorrer.
- O pacote especifica entrada privada, geração e validação, cartão utilizável com retomada,
  checkout simulado e ledger/relatório. A evidência informa **READ_ONLY**, sem efeitos externos.
  O conteúdo dos componentes é descritivo: esta conclusão no BPM **não comprova implementação
  executável da v8**, nem leitura humana, homologação ou autorização comercial. É o mesmo limite
  de interpretação protegido por `LOOP-DEDALO-CONTRATO-MARCADO-COMO-PROTOTIPO`.
- Processo 3 passou a apresentar duas atividades concluídas e a próxima **3.3 — Produzir
  audiovisual quando previsto**, disponível. A arquitetura #362 declara `audiovisualRequired=false`:
  essa necessidade deve ser resolvida explicitamente no BPM, evitando gerar vídeo sem requisito
  para o protótipo. A implementação de acesso e a homologação posteriores não foram executadas
  nesta solicitação.
- Nova conferência em desktop, iPhone e Pixel confirmou o segundo ciclo e o aprendizado do #91
  na tela do Processo 2, navegação para o Processo 3 e próxima atividade 3.3 após a conclusão,
  sem erros JavaScript ou novos comandos de execução. Foi necessário usar o prazo de navegação
  operacional de 30 segundos; a consulta observada de contexto levou aproximadamente nove segundos
  no host, enquanto a fixture local respondeu rapidamente.
- Ciclo #2 permanece `OPEN/ADJUSTMENT`, predecessor #1 e retorno original à arquitetura do
  Processo 2 preservados. #91 permanece `USER_STOPPED`; #92, `PLANNED`.

A topologia Compose da sandbox foi encerrada com `down --volumes --remove-orphans`.
Não houve commit, push ou criação de PR nesta solicitação.
