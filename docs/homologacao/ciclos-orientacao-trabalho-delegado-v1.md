# Orientação após registrar a janela — homologação v1

Escopo: tornar a continuação do ciclo compreensível, sem concluir preparação por declaração.
Não implementa o compositor de Capella, não renova parecer, não inicia tarefa paga e não ativa mídia.

## Evidência anterior

Consulta em 06/10/2026, 18:42–18:48 UTC: Capella, produto 7, cadeia 26, ciclo 5, experimento 98,
revisão 3, ADJUSTMENT/OPEN. O evento 34 registrou DEFINE_INITIAL_WINDOW em 17:39:07 UTC,
para 08/10/2026 03:00 UTC até 16/10/2026 03:00 UTC, orçamento zero e sem gasto autorizado.
Execução 47: WAITING_INPUT na homologação técnica. Tarefas 596, 597 e 599 declaram explicitamente
especificações prontas; URL, implementação, artefatos e testes ainda não produzidos. Nenhuma tarefa
de Psique está em andamento nessa referência. A consulta MCP aos logs atuais não encontrou linhas
de homologação; o estado está comprovado pelos registros persistidos e pelas telas oficiais.

Alternativas consideradas: manter formulário e explicar em texto conserva o risco de avanço
declaratório; retirar comandos somente no frontend não protege outros consumidores;
expor trabalho delegado e aplicar o mesmo bloqueio no comando protege UI, API e harness. A terceira
é adotada, reutilizando o resolvedor existente sem criar orquestração paralela.

## Matriz local definida antes da execução

| Cenário | Aceite | Verificação |
|---|---|---|
| Datas salvas | Confirmar período em Brasília e explicar que campanha não começou | React + Chromium |
| Capella aguardando protótipo | Pendência antes da memória, dependência de Dédalo, próximo gate de Psique | Contrato Java + React + Chromium |
| Outro produto/ciclo | Identificadores substituídos; mesma regra, nenhum tratamento por nome ou ID | Teste parametrizado Java/React |
| Conclusão declaratória | COMPLETE indisponível na leitura e rejeitado na API; nenhuma gravação/evento | Serviço Java |
| Preparação comprovada | Caminho anterior avança para VALIDATION; não pula homologação ou autorização | Serviço Java |
| Atividade em andamento | Orientar acompanhamento, sem simular espera por nova decisão | React |
| Ação excepcional | Alterar/encerrar acessível sob demanda; conclusão pendente ausente | React |
| Registro da janela → releitura | Exibir próximo trabalho depois da resposta, sem novo POST de etapa | React + Chromium com API simulada |
| Dados ausentes, ciclo histórico, falha de leitura | Preservar estado explícito e formulários aplicáveis já existentes | Suite de ciclos |
| Desktop, iPhone 15 Pro e Pixel 7 | Botão e pendência legíveis; sem rolagem horizontal | Playwright Chromium |
| Observabilidade e segregação | Eventos/datas anteriores preservados; fixtures sem chamadas produtivas ou métricas comerciais | Mocks + leitura MCP |

Limite comercial: melhorar orientação não comprova produto pronto, venda ou margem. Capella continua
dependendo de implementação executável da mesma versão; o controle existente não executa esse
trabalho apenas porque uma janela foi definida. Não prometer continuidade de agentes ausente.

## Resultado

Rodada local de 06/10/2026:

- Backend: 447 testes nos relatórios locais, sem falhas, erros ou skips, incluindo os
  testes relacionados aos ciclos, ao gate de protótipo e à arquitetura. O comando e a
  leitura foram exercitados pelo serviço real com dependências simuladas.
- Frontend: 90 testes relacionados aprovados. Após corrigir a espera pelo catálogo em
  dois testes novos, repetida apenas a suíte alterada (24 testes), aprovada.
- TypeScript, build Vite, Spotless, Prettier e revisão do diff aprovados; contrato Swagger
  YAML lido e validado. Nenhuma migração ou script shell foi alterado.
- Chromium desktop, iPhone 15 Pro e Pixel 7: período de 08/10 a 16/10 em Brasília,
  pendência da atividade 3.6 antes da memória histórica, link com produto/ciclo/cadeia
  preservados, formulário excepcional fechado e ausência de rolagem horizontal.
  Desktop também exercitou registro da janela e releitura com API simulada; dispositivos
  móveis fizeram somente leitura. Nenhum erro de página ou chamada produtiva foi observado.
- As emulações usam Chromium; não equivalem a teste no Safari nativo. Fixtures e respostas
  simuladas não são artefatos aceitos do produto nem evidência de venda, custo ou margem.

Complemento de integração: o primeiro CI identificou que a fixture REST antiga usava COMPLETE
sem registrar a entrega delegada. A mesma recusa 409 foi reproduzida no MySQL local. A fixture
passa a simular os callbacks anteriores com o padrão sintético já existente, testar a recusa
sem evento antes de cada comprovação e só então completar a etapa. Não foi flexibilizada a guarda.
O runner local usa `LEARNING_CYCLES_DB_HOST=sandbox-docker` nesta sandbox; a tentativa com
localhost não alcançava a porta publicada pela engine dedicada. Com esse endereço correto,
passaram a preparação do runner, a integração de analytics e a regressão de continuidade.

Após a correção, repetidos os cenários REST/MySQL afetados: 20 cenários aprovados, contexto de
ciclo segregado aprovado, rollback/reaplicação/idempotência das migrações aprovados, zero
chamadas externas. Topologia removida com volumes ao final. Bash e ShellCheck aprovados para
o runner inspecionado e para a orquestração temporária; Python compilado sem erro.

Capella permanece com a implementação pendente. A conferência publicada e os links dos
workflows serão registrados no PR depois do merge e da conclusão dos deploys.
