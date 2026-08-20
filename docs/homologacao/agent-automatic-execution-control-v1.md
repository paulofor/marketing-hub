# Homologação — controle PLAY/STOP dos agentes v1

## Gargalo e resultado esperado

A tela de Gestão de agentes não possuía um comando operacional capaz de impedir novas execuções
automáticas por agente. O resultado esperado é: `STOP` bloqueia toda nova reserva automática do
agente e `PLAY` restaura o consumo, preservando health, telemetria e reconexão.

## Decisão

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Reutilizar `ACTIVE/TEST/PAUSED` | Pouca mudança | Mistura contrato, maturidade e operação | Rejeitada |
| Parar ou iniciar o container | Isolamento físico | Perde diagnóstico e não é estado auditável do agente | Rejeitada |
| Estado operacional persistido + gate no executor | Auditável, reversível e sobrevive a reinício | Exige contrato em cada executor | Escolhida |

## Matriz ponta a ponta

| Área | Cenário | Evidência esperada |
| --- | --- | --- |
| Caminho feliz | Clicar Stop em um agente em PLAY | Backend persiste `STOP`, tela atualiza e executor não consulta pendências funcionais |
| Caminho feliz | Clicar Play em um agente em STOP | Backend persiste `PLAY`, tela atualiza e o próximo ciclo pode consultar pendências |
| Idempotência | Repetir o mesmo estado | Nenhum evento auditável duplicado |
| Execução corrente | Acionar Stop com job já iniciado | Job corrente termina; nenhum job novo é reservado |
| Falha | Backend indisponível na checagem | Executor falha fechado e não trabalha automaticamente |
| Integração | Psique, Plutus, Hermes, Atena, Têmis, Dédalo, Apolo e Argos | Todo scheduler funcional consulta o estado antes da fila |
| Observabilidade | Agente em STOP | Health, telemetria e reconexão continuam ativos; tela exibe a verdade persistida |
| Concorrência | Dois comandos próximos | Bloqueio pessimista serializa a decisão; último estado confirmado fica vigente |
| Dados de teste | Jobs e agentes locais | Nenhuma campanha, venda, gasto ou dado de produção é criado |
| Desktop | Chromium 1366x768 | Botão, estado, loading e erro permanecem legíveis |
| Mobile | iPhone 15 Pro e Pixel 7 | Tabela rolável, botão acessível e sem overflow da página |

## Critérios

- continuar: todos os oito executores bloqueiam em `STOP` e retomam em `PLAY`;
- ajustar: algum scheduler acessa fila antes do gate, a tela infere estado ou o erro permite execução;
- parar: `STOP` cancela ou corrompe execução corrente, desativa diagnóstico ou produz efeito externo.
