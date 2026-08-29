# Matriz de homologação — auditoria universal de tarefas dos agentes v2

## Objetivo

Garantir que toda nova execução de agente seja compreensível e acionável na própria tela, sem
depender de logs técnicos: modo de execução, modelo, tipo de raciocínio, prompt integral, causa do
bloqueio, orientação de correção, links de ajuda e URLs realmente acessadas quando o executor
navegar.

## Decisão de arquitetura

Foram comparadas três alternativas:

1. campos livres dentro da evidência de cada agente: menor esforço inicial, mas mantém contratos
   divergentes e permite novas omissões;
2. diário completo de todos os eventos do runtime: máxima granularidade, com custo e complexidade
   desproporcionais para a tela atual;
3. contrato híbrido tipado: auditoria principal na tarefa e links normalizados, segregados pelo
   identificador da própria tarefa.

A alternativa 3 foi escolhida porque fecha a causa-raiz com contrato único, preserva consultas
simples e permite evoluir a telemetria sem serializar JSON técnico dentro de outro JSON.

## Dados de teste e segregação

- tarefas locais usam referências `audit-v2:test:<caso>` e nunca reutilizam produto, cliente ou
  execução produtivos;
- cada URL e orientação pertence a uma única `agent_task`; consultas de outra tarefa não podem
  retorná-las;
- callbacks repetidos não duplicam links nem alteram uma tentativa já terminal;
- nenhuma chamada de teste publica, gasta, envia comunicação ou altera métricas comerciais.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Auditoria de modelo | tarefa conclui com modo `MODEL`, modelo, raciocínio e prompt resolvido integral | callback de modelo sem qualquer um dos campos é recusado | banco, resposta da API e cartão mostram os mesmos valores |
| Execução sem modelo | rotina determinística declara modo e raciocínio `NOT_APPLICABLE` | ausência ambígua não vira execução determinística | tela explica que não houve modelo sem inventar prompt |
| Bloqueio funcional | agente registra categoria, ação concreta e ao menos um link útil | orientação vazia ou link inseguro é recusado | cartão diferencia ajuste funcional de falha técnica |
| Falha técnica | prompt já montado continua auditado; falha anterior ao modelo usa `NOT_STARTED` | falha não pode fingir que o modelo respondeu | causa, ação de retomada e estado do modelo ficam explícitos |
| Psique e URLs | eventos terminais de pesquisa web e observações Playwright estruturadas, quando usadas pela atividade, são vinculados à tarefa | URL apenas autorizada, mas não acessada, não é registrada; segredo na URL é bloqueado | lista mostra URL, título/método e horário disponíveis, ou explicita que nenhuma URL foi aberta |
| Integrações | callbacks centrais, Argos, Plutus e importação de pacote preservam o contrato | caminho legado não pode encerrar nova tarefa de modelo sem auditoria | testes de contrato em backend e workers |
| Observabilidade | prompt, resposta, modelo, raciocínio, tokens, links e decisão são correlacionáveis pelo `taskId` | logs não são usados para recompor a tela | endpoint devolve dados persistidos e auditáveis |
| Métricas | cobertura de auditoria das novas tarefas terminais é 100% | qualquer tarefa nova sem contrato falha no gate local | teste de cobertura por agente e tipo de término |
| Desktop | conteúdo longo expande sem perder hierarquia | prompt e URLs quebram linha sem overflow | Chromium desktop |
| iPhone 15 Pro | orientação, prompt e links permanecem legíveis e tocáveis | nenhum overflow horizontal | Playwright com emulação mobile |
| Pixel 7 | mesma verdade do backend e links em nova aba | nenhum conteúdo fica oculto pelo cartão | Playwright com emulação mobile |

## Critério de conclusão

Uma rodada local integral sem defeito conclui a homologação. Se a rodada revelar defeito, a
causa-raiz deve ser corrigida e duas rodadas integrais consecutivas precisam passar depois da última
correção; qualquer novo defeito reinicia a contagem.
