# Mira — recuperação da tarefa #363

Data: 2026-09-10. Escopo: concluir pela tela a atividade **3.8 — Psique · fricção e
recuperação**, preservando a tentativa bloqueada e liberando a atividade seguinte no BPM.

## Diagnóstico e alternativas

A #363 foi bloqueada antes do parecer do modelo. O MCP confirmou `TECHNICAL_FAILURE` e
o log do PDE, em `2026-09-10T03:30:45.053Z`, recebeu simultaneamente `READY_RESULT_USED`
e `RECOVERY_COMPLETED`. A validação recusou a recuperação porque o uso ainda não havia
sido registrado. O executor aguardava somente o clique Playwright, que não confirma a
conclusão da requisição iniciada pela tela. As homologações #349 e #354 passaram pelo
mesmo cenário anteriormente: não se trata de requisito novo nem de falta permanente
de acesso ao protótipo.

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Repetir a tarefa ou adicionar espera fixa | Mudança pequena | Mantém a disputa de ordem e o custo das tentativas | Descartada |
| Aceitar recuperação antes do uso no PDE | Evita a rejeição | Invalida a evidência e afrouxa o gate funcional | Descartada |
| Aguardar a resposta persistida do evento disparado pela tela | Ordem comprovável, falha explícita, sem nova análise | Correção localizada no executor e teste com atraso controlado | Adotada |

## Matriz definida antes dos testes

| Controle | Critério |
| --- | --- |
| Reprodução | Atrasar o registro de uso reproduz o bloqueio com o executor anterior |
| Recuperação | Entrada incompleta bloqueada; erro de geração; recarga mantém entrada; rotina gerada e consultada; uso confirmado antes de recuperação |
| Caminho feliz e segurança | Cenários ADHERENT e SAFETY preservam seus gates |
| Falhas de integração | HTTP de erro, resposta inválida ou evento ausente nunca permitem conclusão |
| Auditoria | Erro identifica operação/status sem token; eventos, versão, artefatos e resultado permanecem correlacionados |
| Métricas | Tráfego AGENT_VALIDATION segregado; nenhuma evidência humana, compra ou campanha criada pelos testes |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação |
| Imagem | Dockerfile do repositório, módulo testado, script corrigido presente na imagem final |
| Operação | Nova tentativa criada pela UI; callback real completa a atividade e libera 3.9 no banco e na tela |
| Limpeza | Topologia local exclusiva encerrada com volumes e órfãos removidos |

Depois de qualquer defeito corrigido durante a homologação, são exigidas duas rodadas
locais completas e consecutivas sem falhas antes da publicação autorizada.

## Resultados

Na preparação da publicação, o health de Psique expôs uma segunda falha confirmada:
`esperado=7 implantado=6`. A curadoria realizada pela UI incrementou `agent.current_version`,
enquanto o executor permaneceu corretamente no contrato técnico 6 do manifesto versionado.
O backend comparava essas duas identidades diferentes. A autenticação e o backend estavam
acessíveis, e a tela continuava permitindo a retentativa.

Alternativas: aumentar manualmente a versão declarada (frágil e repete a falha na próxima
edição); fazer o worker declarar automaticamente qualquer versão esperada (prova fictícia);
ou separar a versão do cadastro da versão técnica publicada. Adotada a terceira. O mesmo
manifesto usado pelo gate de código passa a ser empacotado no backend. Ausência de contrato,
binário antigo, ausência de autenticação/rede e heartbeat vencido continuam bloqueando.

A matriz foi ampliada antes da nova rodada: cadastro 7/8/9 com executor 6, executor antigo,
autenticação/rede ausentes, releitura de prova persistida e recurso presente na imagem backend.
As primeiras duas rodadas de Psique passaram integralmente (7/7 cada). Depois da correção
adicional, a matriz ampliada foi executada duas vezes consecutivas, sem alteração de código
entre as rodadas, com **13/13 controles aprovados em cada uma**.

| Evidência por rodada final | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend completo, incluindo arquitetura | 2.595 casos; 5 ignorados preexistentes; nenhuma falha | Igual |
| Psique — suíte Java completa | 89 aprovados | 89 aprovados |
| PDE — serviços de protótipo, persistência e gates | 16 aprovados | 16 aprovados |
| Node / Chromium — contratos do worker | 6 aprovados | 6 aprovados |
| Frontend e backend PDE reais, MySQL 5.7 | 5 cenários, 3 perfis, 5 capturas; todos aprovados | Igual |
| Harness da imagem final, sem rede externa | 4 regressões aprovadas | 4 regressões aprovadas |
| Backend da imagem final, sem rede externa | Catálogo técnico e 156 cards carregados | Igual |
| Recursos externos do JAR | 359 íntegros | 359 íntegros |
| Contratos de Docker, health, CI e seleção de deploy | Aprovados | Aprovados |
| Revisão do diff | Sem erros de whitespace | Sem erros de whitespace |

A integração usa o frontend `dist-mira` construído do repositório, backend PDE executável,
MySQL 5.7 dedicado e proxy local com atraso de 500 ms antes da persistência. As regressões
isoladas também recusam HTTP 503, JSON inválido e resposta 200 sem evento. O modelo é
simulado nos testes unitários; o parecer operacional depende da execução real posterior.

Execução reproduzível da matriz: `infra/testing/mira-recovery/run-round.sh`. Topologia:
`infra/testing/mira-recovery/compose.yml`, no projeto exclusivo
`aihub-e58dcf01-e6db-4904-9962-7459823951fe-60a739088d`. Os serviços locais usam credenciais
sintéticas; nenhum teste local aponta para banco, pagamento ou campanha produtivos.

Os logs das rodadas desta sandbox estão em `/tmp/mira363/final1` e `/tmp/mira363/final2`.
Uma execução preparatória do backend foi descartada porque dois comandos Maven disputaram
o mesmo diretório `target`. A compilação foi limpa, e as duas rodadas finais foram
sequenciais; o resultado da execução descartada não integra a homologação.

Validações complementares aprovadas: Spotless dos Java alterados, Actionlint, ShellCheck e
regressão do verificador de recursos. Não houve migração Liquibase nesta correção.

## Publicação e confirmação operacional

As imagens foram construídas exclusivamente pelos Dockerfiles versionados de
`backend/ads-service` e `customer-agent-worker`, a partir da revisão base `0dc00be2` e do
diff local documentado. A publicação manual foi explicitamente autorizada pelo usuário.
Imagens aplicadas, com configuração e credenciais existentes preservadas:

- Backend em `191.252.181.168`: `marketinghub-backend:mira363-0dc00be2-r1`.
- Psique em `163.245.202.80`: `marketing-hub/customer-agent-worker:mira363-0dc00be2-r1`.
- As camadas e configurações das imagens recebidas foram comparadas às locais. No backend,
  o campo de usuário vazio/nulo é a mesma representação do usuário padrão da imagem.
- Runtime backend consultado via MCP publicou `commitId=0dc00be2+mira363-r1`.
- Psique publicou `READY`, versões técnica esperada/implantada `6/6`, autenticação e rede
  disponíveis, com `buildReference=0dc00be2+mira363-r1`. O cadastro editável permaneceu na versão 7.
- SHA-256 do harness confirmado dentro do container: `5de9ceaf6c98620ac9fc5a3eb6bd2efa7a7cb0b7c56f7eb82d80f3d136563e27`.

A retentativa foi enviada pelo botão **Reiniciar tarefa**, dentro da atividade 3.8 do Mira,
no processo 70 v8. A UI respondeu com a **tarefa #364**, mantendo a instância 228 e a
referência `product:10@agent-validation-v1`. A #363 permaneceu bloqueada como histórico.

O PDE confirmou para a nova sessão de evidência `e6111c34-8fc8-4148-9cac-d5d9514527c9`
a sequência `EXPERIENCE_STARTED → VALUE_MOMENT → READY_RESULT_USED → RECOVERY_COMPLETED
→ AGENT_SCENARIO_COMPLETED`.

**Resultado confirmado: tarefa #364 COMPLETED, parecer APPROVED, cenário RECOVERY,
versão `mira-private-v2`.** O backend registrou entrega em `2026-09-10T05:03:56` e a
instância 228 passou a `COMPLETED`, `objective_achieved=true`, sem motivo de bloqueio.
A evidência visual 93 e o parecer original estão persistidos na tarefa. Os nove checks
funcionais do parecer passaram e `requiredChanges` ficou vazio. O custo de IA informado
foi **US$ 0,292404**, com 55.566 tokens de entrada e 3.507 de saída.

A navegação real em Chromium desktop, iPhone 15 Pro e Pixel 7 confirmou:

1. Atividade **3.8 concluída**, com #364 COMPLETED e #363 BLOCKED preservadas no histórico.
2. Atividade **3.9 — Psique · limite e segurança** disponível, com botão **Executar atividade** habilitado.
3. Mensagem do BPM: a recuperação aprovou a mesma versão e a próxima atividade pode ser executada.
4. Página sem transbordamento horizontal nos três perfis.

Entrada da próxima atividade: [Processo 3 do Mira — atividade 3.9](http://191.252.181.168:5173/products/10/value-chain-history/processes/70/activities#activity-psiqueSafety).
Capturas e leituras desta sandbox: `/tmp/mira363/completed-*.png`, `next-*.png`,
`activities-after-*.json`, `task364-completed.json` e `activity228-completed.json`.

A avaliação permanece sintética, classe `AGENT_VALIDATION`; não comprova preferência,
satisfação, compras ou receita humanas. Pagamento, publicação comercial, campanha e gasto
de mídia ficaram desabilitados. A tarefa 3.9 foi apenas conferida, sem abrir outra execução.

Os Actions mais recentes de backend e Psique no SHA `586eff73` estavam verdes na consulta:
`34431721690`, `34431721620` e `34432798660`. Não foi disparado workflow para testar a correção.
Nenhum commit, push ou PR foi criado; o diff local permanece disponível para consolidação
posterior pelo usuário.

Limpeza concluída: Compose encerrado com `down --volumes --remove-orphans`, banco e
volumes locais removidos, processo PDE local encerrado e dependências temporárias retiradas.
Os serviços produtivos permanecem nas imagens validadas.
