# Tarefa de correção na atividade de origem

Data: 2026-09-11. Escopo: cards de atividades do produto e contrato de recuperação do BPM.

## Diagnóstico confirmado

- A tela pública do Vega, produto 4, processo 70 v8, ciclo 2, experimento 92, repete o
  acompanhamento da tarefa 385 em `prototypeCorrection`, `psiqueAdherent`, `psiqueRecovery`,
  `psiqueSafety` e `commercialIntegrityReview`.
- O endpoint `GET /api/business-processes/70/products/4/activity-executions` coloca 385 somente
  em `prototypeCorrection.tasks`. Nos outros quatro cards ela aparece em
  `recoveryAction.latestTask`. A 3.7 tem a tarefa 384; 3.8, 3.9 e 3.10 têm zero tarefas.
- MCP `db_health` confirmou `marketinghubdb`. `db_query` confirmou `process_activity_id =
  prototypeCorrection`, agente 7 (Dédalo), instância 245 e status `BLOCKED` para 385.
  Nenhuma cobertura adicional existe para 377, 380, 382, 383, 384 ou 385. A correção 382 e a
  homologação 383 estão concluídas; 384 é o parecer posterior bloqueado de Psique.
- `java_module_logs` respondeu à consulta do backend por `385`, sem linhas na janela retida.
  A conclusão se apoia nos dados persistidos, na resposta HTTP e na reprodução da tela.
- Causa: o painel substitui a tarefa própria por `recoveryAction.latestTask` e oferece o mesmo
  comando de Dédalo em todos os cards dependentes. Não existe duplicação de tarefa no banco.
  O resolvedor também mantém a recuperação após sua conclusão quando existe tentativa histórica.

## Alternativas e escolha

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Identificar a tarefa repetida como referência de correção | Ajuste pequeno, mantém acompanhamento no local | Continua repetindo a execução em atividades sem tarefas | Não escolhida |
| Recolher a correção em um painel de dependência expansível | Preserva detalhe acessível em cada card | Mantém diversos pontos para disparar o mesmo trabalho | Não escolhida |
| Concentrar comando e acompanhamento na atividade responsável, com links nos demais cards | Torna a relação atividade/tarefa clara e preserva histórico e ciclo | Pequena evolução de contrato e navegação | Escolhida |

## Matriz definida antes dos testes

| Critério | Verificação local |
| --- | --- |
| Origem e dependências | Tarefa de Dédalo somente em 3.6; 3.7 mostra seu parecer; 3.8–3.10 sem tarefa de correção própria |
| Navegação | Link identifica 3.6, nome e responsável; mantém produto, processo, ciclo, cadeia e âncora |
| Execução | Apenas a atividade responsável cria tarefa; clique único, confirmação local e prevenção de duplicação |
| Acompanhamento | Fila, execução, bloqueio e conclusão recebidos do backend; rotação apenas em execução e movimento reduzido respeitado |
| Recuperação concluída | Backend deixa de oferecer correção concluída como pendência; tarefa histórica permanece preservada |
| Segregação | Tentativas de outro processo ou ciclo não alimentam recuperação; custos e contagens não são duplicados |
| Falhas | Falha de comando ou acompanhamento não fabrica tarefa/conclusão e permite atualização posterior |
| Integração | Testes do contrato Java, ciclo real de comando/fila/callback com H2 e executor simulado, frontend real com HTTP simulado |
| Dispositivos e observabilidade | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; capturas, console, ausência de overflow e de acessos externos |
| Regressão | Testes de página, painel, acompanhamento e recuperação; TypeScript, build, formatação e diff |

Dados locais sintéticos, sem IA paga, e-mail real, campanha, receita simulada em produção ou novas
tarefas produtivas. Não há alteração de schema; MySQL produtivo é consultado somente pelo MCP.
Se uma rodada revelar defeito, corrigir e obter duas rodadas completas consecutivas após a última
correção. Sem defeito, uma rodada completa basta.

## Resultado

Na preparação, o roteiro de navegador bloqueou corretamente a API padrão da porta 80: o bundle
produtivo resolve a API fora da porta da UI. O build de homologação foi configurado com
`VITE_API_URL=http://127.0.0.1:4173` e todas as APIs foram simuladas nessa origem. Nenhum acesso
produtivo ocorreu nos cenários sintéticos. A primeira tentativa de formatação usou um glob em
`spotlessFiles`, que exige expressão regular; o comando foi corrigido, sem alterar o projeto.

A regressão HTTP de homologação técnica ainda exigia apresentar a correção concluída como
dependência. A expectativa foi atualizada para o novo contrato canônico: `recoveryAction = null`
após objetivo atingido, mantendo tarefa, estado e histórico na atividade responsável. O teste
também verifica o número da atividade retornado pela API e a ausência da tarefa na lista da
atividade dependente. As duas rodadas completas são executadas após esse último ajuste.

### Reprodução local

Na raiz do repositório:

```bash
mvn -B -ntp -f backend/ads-service/pom.xml '-DspotlessFiles=.*(ProductProcessActivityRecovery(Resolver|Response|ResolverTest)|PdeTechnicalHomologationActivityExecutionTest)\.java' '-Dtest=ProductProcessActivityRecoveryResolverTest,ProductProcessRecoveryLifecycleTest,ProductProcessExecutionProgressTest,BusinessProcessActivityExecutionServiceTest,PdeTechnicalHomologationActivityExecutionTest' spotless:check test
```

Dentro de `frontend`, instalar as dependências com `npm ci --no-audit --no-fund`, executar os
testes abaixo e servir o build com `npm run preview -- --host 127.0.0.1 --port 4173`:

```bash
npm test -- --run src/pages/product/ProductProcessActivityExecutionPanel.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx src/pages/product/ProductProcessTaskTracking.test.tsx src/components/ProductNextActivitySummary.test.tsx
npm run typecheck
VITE_API_URL=http://127.0.0.1:4173 npm run build
EVIDENCE_DIR=../artifacts/task385/browser node e2e/activity-task-ownership-responsive.mjs
```

O roteiro verifica três dispositivos, estados PENDING/IN_PROGRESS/BLOCKED/COMPLETED, falha de envio,
falha e retomada do acompanhamento, retentativa, histórico após recarregamento, links com ciclo e
cadeia, isolamento de autoria, rotação e movimento reduzido. APIs inesperadas e requisições externas
são recusadas; nenhuma execução sintética recebe aparência de venda real.

### Resultado das duas rodadas

`final1` e `final2` concluídas, consecutivas e sem falhas após o último ajuste.

| Controle por rodada | Resultado |
| --- | --- |
| Backend: resolvedor, contrato HTTP, prontidão e ciclo real com H2 | 57/57 testes |
| Frontend: página, painel, acompanhamento e próxima atividade | 50/50 testes |
| Navegação e estados completos | Desktop, iPhone 15 Pro e Pixel 7 aprovados |
| Falhas deliberadas | Envio 503 e acompanhamento 503 recuperados; histórico e autoria preservados |
| Requisições externas/inesperadas e erros JavaScript | Zero |
| TypeScript, build, Spotless, Prettier, Swagger YAML e diff | Aprovados |
| Identidade dos bundles JS/CSS/mapas | Hashes idênticos nas duas rodadas |

Evidências locais em `artifacts/task385/final1/` e `final2/`, incluindo logs, `browser/results.json`,
capturas `*-dependency.png` e `*-origin.png`, `bundle-hashes.json` e contrato Swagger analisado.

Limites: celulares emulados no Chromium, sem Safari ou aparelhos físicos; APIs de navegador e
executor simulados, com teste separado de persistência e callbacks em H2. Nenhuma alteração de
schema ou necessidade de topologia Docker. O build mantém o aviso preexistente sobre tamanho do
bundle. Browsers e servidor de preview encerrados ao final.

Entrega somente local. Nenhum commit, PR, deploy, gasto ou tarefa produtiva foi criado. A tarefa
385 permanece bloqueada na atividade 3.6; esta correção trata a apresentação e a relação de
dependência, sem aprovar o protótipo ou encerrar artificialmente sua correção funcional.
