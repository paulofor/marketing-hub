# Matriz de homologação — amostra direta consentida do Rigel v1

## Objetivo, gargalo e limite

Comprovar ponta a ponta que a atividade `task-2` do processo
`operacao-otimizacao-experimento` acumula exatamente a amostra comercial autorizada de 15 contatos
reais, consentidos e aderentes do experimento 89, sem transformar visita, QA, clique ou nova chamada
ao modelo em contato ou venda.

- **Gargalo real:** as tarefas #294 e #295 verificaram repetidamente um volume que não possuía
  contrato persistido de entrada; ambas bloquearam em 0/15 e consumiram juntas US$ 0,6939976.
- **Métrica esperada:** contador oficial `recordedContacts` avança de 0 a 15; uma nova tarefa de
  Hermes só fica disponível em 15/15; compra e receita permanecem zero até evento financeiro real.
- **Continuar:** registrar somente abordagem já realizada com consentimento anterior e aderência ao
  público.
- **Ajustar:** falha de deduplicação, privacidade, temporalidade, responsividade, persistência ou
  leitura do worker.
- **Parar:** identidade em claro no backend, contato sem consentimento, amostra acima de 15, mistura
  com QA, criação automática de mensagem, campanha, gasto ou venda.

## Alternativas consideradas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Contar visita da landing como contato | nenhuma tela nova | fabrica o significado comercial e mistura QA | rejeitada |
| Reusar evento genérico do funil | menor implementação inicial | não comprova consentimento, pessoa única nem abordagem | rejeitada |
| Registro próprio, pseudonimizado e auditável | sustenta decisão e evita repetição paga | exige backend, tela, migração e contrato MCP | escolhida |

## Matriz ponta a ponta

| Dimensão | Cenário | Evidência esperada |
| --- | --- | --- |
| Caminho feliz | Registrar contato real consentido | telefone/e-mail é normalizado e transformado em SHA-256 no navegador; placar passa a 1/15 |
| Conclusão | Registrar o 15º contato válido | placar 15/15, formulário bloqueado e retentativa de Hermes liberada |
| Validação | Consentimento posterior à abordagem, horário futuro ou evidência textual | HTTP 400 e nenhuma linha persistida |
| Canal | Experimento diferente de `DIRECT_ONE_TO_ONE` | endpoint específico recusa; gate não interfere na operação paga |
| Estado | Experimento fora de `RUNNING` | novo contato recusado sem alterar o histórico |
| Duplicidade | Mesmo telefone/e-mail com outra formatação | mesmo fingerprint, HTTP 409 e contador inalterado |
| Concorrência | Duas requisições disputam a última vaga | lock do experimento mantém o teto em 15 |
| Limite | Chamada após 15/15 | HTTP 409, sem ampliar silenciosamente a amostra |
| Integração BPM | `task-2` bloqueada em 0–14/15 | botão de retentativa ausente e motivo informa número restante |
| Integração Hermes | Worker revisa a atividade em 15/15 | MCP usa `consultar_amostra_direta`; não deriva contato de visita ou QA |
| Observabilidade | Consultar contato e tarefa | datas, referência de consentimento, operador, sufixo pseudonimizado, prompt, resposta e custo auditáveis |
| Privacidade | Inspecionar request, banco e resposta | identidade original não sai do navegador; hash é segregado por experimento e não é persistido ou devolvido em claro |
| Métricas | Contato sem checkout/pagamento | contato avança; compra, receita, entrega e venda permanecem zero |
| Segregação | Fixtures locais | experimento e contatos sintéticos não alteram Rigel ou métricas produtivas |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | formulário, progresso, validação e histórico utilizáveis sem overflow |
| Falha de dependência | Backend ou banco indisponível | tela explica falha, não limpa campos como se tivesse salvo e não cria tarefa |
| Estabilidade | Plano com centenas de respostas extensas do Operador | GET carrega 10 itens, aceita no máximo 20, mantém totais por SQL e não grava plano ou marcos |

Como a investigação revelou defeitos antes da homologação final, depois do último ajuste devem passar
duas rodadas locais completas e consecutivas. Qualquer nova falha reinicia a contagem.

## Resultado local final — 2026-09-01

Na verificação pré-publicação, logs, transações e banco revelaram reinícios do backend causados pela
leitura integral de 362 execuções do plano 2, que somavam 232,23 MB de campos de auditoria. Depois
de limitar o histórico, separar contagens SQL, retirar escrita de GET e segregar o fingerprint por
experimento, duas novas rodadas completas e consecutivas passaram sem alteração entre elas.

Cada rodada cobriu 2.173 testes do backend, 31 testes do Hermes, cinco contratos MCP, 443 testes do
frontend, Spotless, TypeScript, build, contratos Liquibase, migração e reaplicação física em MySQL
5.7, três imagens Docker e jornadas em Chromium desktop, iPhone 15 Pro e Pixel 7. A validação
confirmou 0/15 como estado inicial, request sem telefone/e-mail em claro, hash diferente entre
experimentos, avanço até 15/15 apenas com fixture segregada, formulário bloqueado no teto e ausência
de retentativa de Hermes antes do gate.

A publicação manual permaneceu bloqueada antes de qualquer alteração porque o host principal
`191.252.181.168` recusou a chave SSH disponível. Hermes continuou na imagem anterior para evitar
uma implantação parcial. O banco produtivo confirmou somente #294 e #295, ambos `BLOCKED`; nenhuma
tarefa #296 foi criada, pois o estado comercial real continua sem os 15 contatos comprovados.
