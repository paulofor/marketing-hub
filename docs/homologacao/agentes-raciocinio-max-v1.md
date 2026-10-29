# Raciocínio máximo dos agentes — 29/10/2026

## Evidência e decisão

A inspeção de código e dos containers confirmou Psique e Dédalo em `max`,
Argos em `medium` no planejamento e na síntese, e Íris, Plutus, Têmis e Hermes
em `high`. Atena dependia de um default Java/Spring `high`. Apolo já usava
`max` no planejamento, mas a análise de referências usava `medium`.
O bridge Codex do Worker IA omitia o esforço quando o solicitante não o enviava.

Foram comparadas três opções: alterar apenas variáveis de deploy; criar uma nova
biblioteca global de configuração; alinhar as configurações e os validadores já
existentes em cada executor. A terceira foi escolhida: cobre chamadas independentes
e por atividade e evita redução silenciosa sem criar dependência entre módulos.

## Matriz de aceite local

| Caminho | Comprovação |
| --- | --- |
| Nove agentes | Compose renderizado com ambiente isolado e valores de teste; todos resolvem `max`. |
| Atena, Plutus, Íris, Têmis e Hermes | Suítes Java, comandos Codex existentes e validação de sobrescritas inferiores ou ausentes. |
| Argos | Planejamento e síntese simulados usam `max`; overrides inferiores bloqueiam antes do subprocesso. |
| Apolo | Requisição HTTP simulada de análise contém `max`, Flex e 32.000 tokens; override inferior não envia request. |
| Psique e Dédalo | Defaults máximos preservados; executores locais de revisão seguem a mesma regra. |
| Worker IA | App Server simulado recebe `turn/start.effort=max`; solicitado e efetivo constam no recibo. |
| Recuperação e isolamento | Recibos históricos mantêm esforço original; repetição do mesmo job não abre nova inferência; jobs diferentes permanecem separados. |
| Publicação | Publicadores fixam `max`; contrato de CI cobre todos os Compose/Spring relevantes e mantém seleção de modelo. |
| Segurança e operação | Ambiente de teste sem secrets reais; `bash -n`, ShellCheck, Actionlint, arquitetura e lock de publicação validados. |

A alteração não muda telas ou fluxos de produto; testes de navegador não são
critério desta configuração. A tela pública de agentes foi consultada antes da
mudança e os nove executores serão conferidos após a publicação.

## Resultados locais

- Cinco suítes dos executores Java e a suíte de Apolo aprovadas. A regressão
  adicional de Apolo foi executada após sua inclusão.
- 180 testes de Argos aprovados pelo comando `npm test`, incluindo geração local
  da biblioteca de pesquisa antes da suíte.
- Suíte Java do Worker IA aprovada: 309 cenários, sem falhas e três exclusões já configuradas.
  A dependência do backend foi compilada localmente; os demais artefatos vieram
  do Maven Central, sem usar a credencial GitHub rejeitada.
- 39 testes de conexão/transporte Codex do Worker IA aprovados, com processo
  JSON-RPC real simulado e replay histórico sem nova inferência.
- 20 contratos de configuração e publicação aprovados; executor comercial com
  11 testes aprovados; política máxima do executor local de construção aprovada.
- Validações de arquitetura, proteção da publicação, shell e workflows aprovadas.

Uma comparação legada em `run-pde-construction-v2-agent.test.mjs` já falha na
`main`: confronta o contrato MUSA atual com uma migração histórica imutável.
A falha foi reproduzida com o teste original, sem as mudanças desta entrega.
A validação do raciocínio desse executor foi executada isoladamente e aprovada;
nenhum contrato de produto ou changelog histórico foi alterado para esta tarefa.

## Histórico, limites e aprendizado

O loop conhecido de saída consumida pelo raciocínio foi consultado. A análise de
referências passa de 8.000 para 32.000 tokens como limite técnico padrão, mantendo
auditoria de `incomplete`, reservas financeiras e proibição de repetição automática.
Nenhuma chamada paga foi necessária na homologação. Imagem, voz, escuta e etapas
determinísticas mantêm seus contratos especializados.

A lacuna reutilizável corrigida é a divergência entre defaults, configuração de
container, comando e auditoria. O teste compartilhado antes cobria somente Dédalo
e Apolo; passou a cobrir os nove agentes e as duas fases de Argos. Antes do ajuste,
14 casos novos detectaram divergências; após o ajuste, todos os 20 contratos passam.

A decisão de adoção é o pedido explícito do usuário. Não foram modificados tetos,
processos, produtos ou campanhas, nem reexecutadas tarefas para mudar seu histórico.
`max` pode aumentar latência e consumo; eventual ganho de qualidade, redução de
retrabalho e resultado comercial dependem de medição posterior. Reversão exige
nova decisão de política, mantendo a auditoria das execuções já realizadas.
