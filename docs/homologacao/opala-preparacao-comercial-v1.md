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
