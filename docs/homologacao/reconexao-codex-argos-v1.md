# Matriz de homologação — reconexão Codex de Argos v1

## Objetivo

Permitir criar ou renovar a sessão individual de Argos pelo painel sem expor credenciais de marketplaces e sem interferir nas pesquisas em andamento.

## Cenários obrigatórios

| Área | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Operador seleciona `Reconectar Codex` em Argos | Backend cria um pedido auditável; somente `market-radar` o reserva; URL e código temporário aparecem no painel; confirmação conclui o pedido. |
| Validação | Não existe pedido pendente | Worker não inicia processo nem altera a sessão. |
| Concorrência | Já existe autenticação em andamento | Backend reutiliza o pedido vigente e o worker não inicia dois App Servers. |
| Falha | Backend ou App Server indisponível | Erro completo é registrado e o pedido termina sem declarar autenticação. |
| Integração | `CODEX_HOME` individual | Volume é gravável e persiste a sessão entre reinícios. |
| Segurança | Credenciais Hotmart e ClickBank | Não entram no processo, callbacks, logs, prompts ou volume Codex. |
| Observabilidade | Estado da reconexão | Pedido, início, device code temporário, conclusão e detalhe ficam auditáveis no backend. |
| Métrica | Operação de Argos | Sessão autenticada deve refletir no health; reconexão não conta como pesquisa, oportunidade ou venda. |
| Segregação | Outros agentes Codex | Argos consome apenas `market-radar`; pedidos de Apolo, Atena, Dédalo, Hermes, Psique, Plutus e Têmis permanecem isolados. |
| Interface | Desktop e mobile | A ação usa o painel responsivo já compartilhado pelos agentes e não cria tela ou credencial paralela. |

## Critérios de decisão

- Continuar: pedido único concluído, sessão persistida e health autenticado.
- Ajustar: device code aparece, mas confirmação ou persistência falha.
- Parar: qualquer credencial de marketplace é exposta, outro agente reserva o pedido ou mais de um processo é iniciado.
