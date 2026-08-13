# Matriz de homologação — reconexão Codex dos agentes v1

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Operador inicia, executor coleta, App Server emite código e confirma | Estados `REQUESTED`, `STARTING`, `AWAITING_CONFIRMATION` e `AUTHENTICATED`; health volta a comprovar autenticação |
| Validação | Agente sem executor ou operação concorrente | Bloqueio explícito ou reutilização da operação ativa, sem duplicar login |
| Falha | App Server falha, expira ou é interrompido | Estado `FAILED`, detalhe seguro e nenhuma retomada tratada como sucesso |
| Integração | Backend indisponível no callback | Executor registra stack trace correlacionado e não declara autenticação |
| Segurança | Inspeção de API, banco e tela | Nenhum access token, refresh token ou `auth.json`; somente URL e código temporário |
| Observabilidade | Auditoria da operação | Agente, operador, estados e horários persistidos |
| Retomada | Execução Dédalo falhou por OAuth | Uma única reabertura automática após o executor voltar a consultar pendências |
| Métrica | Operação administrativa | Reconexões por SSH igual a zero e tempo entre solicitação e autenticação mensurável |
| Dados de teste | Backend e App Server simulados | Agente e códigos sintéticos, sem conta ou credencial real |
| Desktop | Chromium em viewport desktop | Link, código, estados e loading legíveis e acionáveis |
| Mobile | Chromium em iPhone 15 Pro e Pixel 7 | Painel sem overflow e ação utilizável por toque |

Se uma rodada completa revelar defeito, a correção reinicia a exigência de duas rodadas completas consecutivas sem falha.
