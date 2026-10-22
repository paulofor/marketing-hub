# Preparação comercial Opala v1

## Matriz definida antes dos testes

- Caminho feliz: chamada no processo de venda → subprocesso Opala → preparação de entrada, criativo, checkout e público → Plutus → revisões independentes → prontidão comprovada → retorno ao pai.
- Validações: tipo oficial PDE, produto/experimento/ciclo/versão coincidentes, aprovação de mídia, preço e checkout canônicos, público aprovado, orçamento válido.
- Falhas: entrada ausente, contrato inválido, mídia reprovada, parecer negativo, callback antigo, falta de publicação e recuperação sem duplicar ativos.
- Integrações: fila e callbacks BPM existentes; respostas dos modelos simuladas localmente, sem consumo pago. Publicação exige seu fluxo próprio.
- Observabilidade: tarefa, responsáveis, request/response, custo quando informado, resultado e motivo do bloqueio persistidos; consulta não cria trabalho.
- Segregação: fixtures locais, outro produto/ciclo/experimento e versão anterior recusados; nenhum resultado sintético representa venda.
- Interface: navegar pela chamada do pai e retornar mantendo contexto; Chromium desktop, iPhone 15 Pro e Pixel 7 emulados. Emulação não comprova Safari nativo.
- Persistência: MySQL 5.7, migração incremental, idempotência e preservação das definições/ciclos anteriores.

## Decisão de implementação

Usar o motor BPM existente, com contratos especializados e materialização no backend.
Um processo paralelo duplicaria fila, histórico e controles; uma tarefa genérica ocultaria
as pendências. Os agentes preparam instruções estruturadas; o backend valida as fontes e
aplica apenas configurações internas. Publicação da experiência, aprovação final de
criativo e ativação comercial continuam nos controles oficiais.

## Resultado

Validado localmente em 15–16/09/2026. Mudanças sem commit, PR ou publicação.

| Camada | Resultado final dos relatórios locais |
| --- | --- |
| Backend: testes relacionados, arquitetura e integração de criativos | 580 aprovados; 2 cenários opcionais não habilitados |
| Dédalo | 66 aprovados |
| Plutus | 41 aprovados |
| Psique | 107 aprovados; 1 cenário opcional não habilitado |
| Têmis | 92 aprovados; 1 cenário opcional não habilitado |
| Frontend | 22 aprovados; TypeScript sem erros |
| Navegação | Desktop, iPhone 15 Pro e Pixel 7 emulados: acesso ao filho, responsáveis, retorno ao pai, contexto preservado, zero escritas |
| MySQL 5.7 | 22 testes Opala aprovados pelo runner, incluindo migração real, reaplicação, rollback e preservação do catálogo anterior |
| Changelogs | Validação estática de includes, temporalidade e erro 1093 aprovada |

Os 22 testes do runner já estão contabilizados no backend; não são somados novamente.
Os totais refletem o último resultado de cada classe, sem somar repetições de diagnóstico.
A aplicação Spring Boot iniciou na integração de criativos, com banco H2 e fontes
externas simuladas. A migração do catálogo foi executada separadamente no MySQL 5.7.

### Evidências e prevenção

- `OpalaCommercialContextTest`: tipo oficial, identidade, isolamento de slot por
  experimento/versão e leitura após liberação sem autorizar novas mutações.
- `OpalaCommercialMaterializationTest`: slot PLANNED, checkout canônico, preço
  divergente, tenant do vídeo, público não aprovado e revogação de aprovação.
- `OpalaCommercialServiceTest`: callbacks da versão correta, rejeição de tarefa antiga,
  economia sem margem e revisão sem ativos comerciais reais.
- `OpalaCommercialGateTest`: conjunto completo de tarefas, reenvio idempotente,
  ativos alterados exigindo nova revisão/prova, ausência de tarefa e janela expirada.
- `ProcessExecutionGraphTest`: lê os dois grafos do SQL efetivo, percorre a sequência
  do filho e impede operação antes da preparação.
- `SalesFlowResolverTest`: chamada ao filho pronta para despacho, pai sem execução
  fictícia, retorno após conclusão e não aplicabilidade às cadeias anteriores.
- `PdeEconomicsBpmRoutingTest`: cenário Opala com HTTP local, fila canônica, modelo
  simulado, prompt próprio, resposta bruta, identidade e tokens no callback.
- Suites de Psique/Têmis/Dédalo: catálogo e recursos versionados dos novos contratos,
  junto das regressões existentes de validação e consumo.
- Navegação reproduzível em `infra/testing/opala-commercial/browser.mjs`, com o Vite
  iniciado usando `frontend/vite.learning-cycles-local.config.ts`. Imagens e resumo
  ficam em `artifacts/opala-commercial/` na sandbox.

Na investigação local, o estado IN_PROGRESS do pai impediria a delegação do filho,
pois `ProcessRunService` verifica trabalho em curso antes de despachar. A projeção
foi corrigida e recebeu regressão. A renovação da prova final também foi ajustada:
mesma versão com ativos alterados exige nova ocorrência após os pareceres atualizados.

O teste de rollback identificou cache otimista do comando update do Liquibase 4.26
quando vários comandos rodam na mesma JVM, inclusive com novas conexões. A fixture
usa a API `setFastCheckEnabled(false)` durante cada comando, restaura a configuração
no finally e confere os registros reais. O runner final passou partindo de banco novo.

### Limites e próximos ambientes

A homologação é composta por contratos locais: backend real com dependências
simuladas, HTTP/modelo simulado nos workers, catálogo no MySQL e UI real com respostas
HTTP controladas. Não representa campanha Meta, cobrança, tráfego ou venda real.
O subprocesso organiza e materializa a preparação; publicar a experiência, obter
aprovação final de criativo e ativar tráfego continuam exigindo os controles existentes.
Checkout ausente, contrato de outra versão ou mídia sem aprovação produzem bloqueio;
não são completados com dados inventados.

Quatro cenários opcionais preexistentes de outras jornadas não foram habilitados:
`videoCreative.browser`, `publicationRecovery.browser`, `VEGA_SCENARIO_LOCAL` e
`VEGA_GATE_FLOW_ARTIFACTS`. A navegação Opala foi testada separadamente nos três
perfis descritos. Safari nativo e dispositivos físicos não foram validados.

O runner `infra/testing/opala-commercial/run-mysql.sh` e o job
`validate-opala-commercial-preparation` do workflow Liquibase ficaram versionados.
O Actions não foi acionado. A aplicação publicada não foi alterada; a nova cadeia
vale para futuras execuções após o fluxo de PR/deploy do usuário. O ciclo atual
mantém sua cadeia e aprovações, sem migração silenciosa.

Ao encerrar, Vite e a topologia MySQL temporária foram removidos. Não foram criados
artefatos operacionais no Marketing Hub pela API ou por acesso ao banco.

## Alinhamento da primeira preparação — 22/10/2026

Vega 4/106 recebeu a candidata v14, mas o executor rejeitou a tarefa 795 antes do
modelo: o checkout canônico estava completo no contrato e ainda não estava gravado
no experimento. No caminho histórico 92, esse vínculo já existia. O backend e o
executor interpretavam de modo diferente a ausência esperada antes da materialização.

Foram comparados três caminhos: gravar o checkout antecipadamente pela operação
apenas contornaria a divergência; bloquear a abertura até haver checkout impediria
a atividade que deve prepará-lo; distinguir candidata completa de vínculo ainda
ausente preserva a ordem do processo e os controles. Foi escolhido o terceiro.

A validação anterior ao modelo aceita vínculo persistido nulo, ausente ou em branco
somente quando o checkout canônico continua declarado. Um vínculo diferente e uma
candidata sem checkout continuam recusados. O executor não grava checkout nem
avança o pipeline; o backend continua responsável por materialização e sequência.

O teste local reproduziu a falha original antes da alteração. As regressões incluem
outro produto/experimento, as atividades de entrada e checkout e um caminho antes
válido. São provas de contrato, sem chamada externa ou comprovação comercial.

### Passagens preparatórias sem vídeo obrigatório

A conferência do endereço v9 mostrou que instalar o software não publicava o contrato
candidato: a consulta ainda recebia o contrato global anterior. A política de preparação
agora reconhece compra única com sete dias e materiais. O botão existente pode publicar
essa candidata PLANNED sem promover o slot ou ativar o experimento. A promoção exige
checkout/destino materializados e preflight; o gate comercial permanece independente.

Também faltava ligar a peça privada aceita ao anúncio do experimento. A entrada do
backend preserva os pixels e o texto de Íris em DRAFT, com origem, SHA, pareceres e
aceite privado no histórico. Ela não concede aprovação comercial. A revisão só é
enfileirada após configurar o checkout, evitando parecer pago sobre entrada incompleta.
Callbacks repetidos conservam o anúncio existente; outra ocorrência não herda as provas.

Os testes percorrem publicação e visão do painel com os serviços reais, preservando
PLANNED; rejeitam contexto/preço/acesso divergentes e campanha já liberada. A passagem
criativa usa armazenamento simulado, verifica pixels, ambiguidade, origem e aceite,
e mantém os gates comerciais pendentes. Testes sintéticos não comprovam vendas.

O fluxo integrado local usa o verificador real de identidade, pareceres e aceite
privado antes da cópia, com armazenamento e inferência substituídos por doubles.
Foram aprovados 363 testes relacionados do backend, incluindo inicialização completa
e arquitetura, e 103 do executor Dédalo. Dois replays opcionais preexistentes de
contexto privado exportado não foram habilitados por ausência de arquivo; o verificador
real e a nova passagem são exercitados pelos testes locais independentes desse export.

A ampliação inclui compatibilidade de pagamento visual e roteamento de Mira/Alcyone.
As fontes compartilhadas alteradas ganharam novas atestações imutáveis, limitadas à
compatibilidade e com publicação automática desabilitada; os pareceres anteriores não
foram renovados. O pacote de evidências foi reconstruído e validado localmente.
