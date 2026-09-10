# Curadoria do Harness para tarefas — 2026-09-10

## Escopo e decisão antes da homologação

Revisão do catálogo operacional: 156 cartões, 155 dentro da validade, nove coleções.
O catálogo roteava automaticamente quatro coleções para quatro agentes em contexto audiovisual.
As entradas editáveis do cadastro de agentes eram versionadas, mas não participavam da seleção
entregue pelo `pending` das tarefas. Não bastava cadastrar uma referência para afirmar consumo.

Alternativas comparadas:

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Copiar pesquisa nas descrições BPM | Usa editor existente | Duplica conteúdo e exige novas versões de processos | Descartada |
| Referenciar cards nas entradas versionadas dos agentes | Cadastro existente, fonte única e orientação por responsabilidade | Exige conectar leitura ao contrato entregue ao executor | Adotada |
| Criar CRUD e tabela próprios de vínculos | Gestão dedicada | Duplica cadastro e aumenta manutenção sem necessidade atual | Adiada |

Contrato: entrada de agente com tipo `HARNESS_RESEARCH_CARD`, nome igual ao `cardId` e descrição
curta com uso, atividade sugerida e critério. O schema real confirma `agent_input.description`
como `TINYTEXT`; as orientações devem caber em 255 bytes UTF-8. Entradas anteriores são preservadas.
Referências são consultivas; não alteram o BPM, autoria, autoridade, aprovação ou orçamento.

## Matriz local definida antes dos testes

| Critério | Verificação |
| --- | --- |
| Caminho feliz | Entrada salva pelo formulário existente; leitura pelo backend; seleção serializada no contrato da tarefa |
| Cobertura | Nove identidades, incluindo Atena/Dédalo/Hermes fora do audiovisual; isolamento entre agentes |
| Compatibilidade | Rotas de vídeo continuam com cobertura das coleções obrigatórias e máximo de quatro cards |
| Validações | IDs duplicados, ausentes, vencidos e fontes futuras não geram evidência válida |
| Conteúdo | Seções de evidência, hipótese interpretativa, aplicação possível e limites chegam ao card |
| Auditoria | ID, SHA da fonte, orientação da curadoria e impressão digital na seleção enviada ao executor |
| Falhas | Fonte/banco indisponível não produz seleção fictícia; consulta sem vínculos preserva comportamento anterior |
| Interface | Catálogo mostra vínculos persistidos e permite chegar ao editor do agente; filtros preservados |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação local |
| Métricas | Nenhuma venda, evento de cliente, tarefa paga, campanha ou conclusão fictícia criada pela homologação |
| Segregação | Fixtures locais; chamadas de escrita produtiva somente pelo formulário autorizado, após validação |

Uma rodada sem defeitos conclui a homologação. Se a rodada revelar defeito e houver correção,
executar duas rodadas locais completas consecutivas após a última correção.

## Resultado

Validação de backend, frontend e Argos aprovada em duas execuções consecutivas com o código final:

| Camada | Resultado por execução |
| --- | --- |
| Backend | 248 testes, zero falhas/erros/ignorados; inclui 92 regras de arquitetura |
| Frontend | 537 testes em 151 arquivos; tipagem e build aprovados |
| Argos | 124 testes; referências presentes uma vez no planejamento e na síntese |
| Formatação | Spotless nas dez classes Java alteradas/novas e Prettier nos arquivos TS/JS alterados |
| Fontes | 14 hashes conferidos contra os Markdown versionados; 25 orientações dentro de 255 bytes |

O navegador usa o frontend real em Vite e respostas HTTP simuladas com o catálogo/seleções
exportados pelo serviço Java real, sob relógio fixo em 10/09/2026. Os testes Java simulam os
repositórios e executores. Não foi executado um Spring/MySQL integrado nem um modelo pago;
não houve mudança de schema ou migration. O schema produtivo foi consultado pelo MCP.

O roteiro de navegador cobre cadastro e releitura dos nove agentes, preservação dos contratos
anteriores, 25 vínculos, filtro de Dédalo, link para edição, catálogo indisponível e ausência de
rolagem horizontal. As duas execuções finais terminaram com código zero em desktop, iPhone 15 Pro e Pixel 7. Uma execução anterior cumpriu
os três dispositivos, mas recebeu SIGTERM no encerramento; foi descartada do par de execuções
com encerramento normal. As primeiras correções do roteiro ajustaram interceptação de `/api/`
e a localização do seletor de agente; não representavam falhas do frontend publicado.

Comando Maven em `backend/ads-service`; demais comandos a partir da raiz do repositório:

```bash
mvn -q -Dtest='ResearchIntelligence*Test,AgentServiceTest,AgentTaskServiceTest,ProductDiscoveryServiceTest,LearningCycleDecisionServiceTest,ArquiteturaTest' -Dharness.curation.fixture=/tmp/harness-curadoria/local-contract.json test
npm --prefix frontend test -- --run
npm --prefix frontend run typecheck
npm --prefix frontend run build
npm --prefix product-discovery-worker test
HARNESS_BACKEND_FIXTURE=/tmp/harness-curadoria/local-contract.json node scripts/test-harness-curation-ui.cjs
```

O roteiro UI pressupõe Vite em `http://127.0.0.1:5511`; a base pode ser alterada por
`HARNESS_UI_BASE`. Os arquivos de saída ficam em `HARNESS_UI_OUTPUT`. Evidências temporárias
nesta sessão: `/tmp/harness-curadoria/backend-final{1,2}.log`, `frontend-round{1,2}.log`,
`argos-round{1,2}.log`, `ui-final{2,3}.log` e capturas por dispositivo. O conteúdo durável está
nos testes/fixture e na [curadoria com fontes](../marketing/harness-curadoria-agentes-2026-09-10.md).

## Aplicação operacional

Os nove cadastros foram atualizados pela UI pública, com 25 referências e uma nova versão por agente.
O MCP confirmou o conteúdo atual de `agent_input` e os snapshots da versão vigente em `agent_version`.
Uma comparação independente com a captura anterior à edição confirmou a preservação dos contratos
anteriores. O editor normalizou os índices antigos de Íris de base um para base zero, preservando a
ordem relativa e o conteúdo; o verificador operacional foi ajustado para comparar essa equivalência,
sem nova gravação dos cadastros já concluídos. A comparação de contratos desconsidera metadados de
workflow, pois a listagem os enriquece e o endpoint individual não faz esse enriquecimento.

Também passou `AgentHarnessCatalogTest`, protegendo a correspondência dos prompts reais dos workers
com a biblioteca empacotada pelo backend. Sintaxe dos dois Swagger e diff foram validados.

A lista de versões e fontes está no relatório de curadoria. As respostas e capturas operacionais
ficam em `/tmp/harness-curadoria/production`, `mcp-associations.json` e `mcp-versions.json` nesta sessão.
O uso ampliado exige publicação do backend, frontend e Argos pelo fluxo de PR do usuário; nenhuma
tarefa produtiva foi executada para afirmar consumo antes dessa publicação. Não houve commit,
push, PR, publicação de imagem, deploy ou topologia Docker temporária.
