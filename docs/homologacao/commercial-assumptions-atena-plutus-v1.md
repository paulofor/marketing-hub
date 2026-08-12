# Matriz de homologação — premissas comerciais Atena + Plutus v1

| Área | Caminho feliz | Validação/falha | Evidência e observabilidade |
|---|---|---|---|
| Entrada | plano com premissas ausentes inicia Atena pela tela | plano inexistente é rejeitado | execução estratégica, versão e snapshot congelado |
| Atena | pesquisa fontes e compara três alternativas | evidência insuficiente mantém valores nulos | fontes, proposta, resposta bruta, modelo e custo |
| Integração | conclusão de Atena enfileira Plutus depois do commit | falha de Atena não cria validação financeira | IDs correlacionados no contexto e tarefas dos agentes |
| Plutus | valida coerência e retorna `APPROVE` | margem inválida ou premissa incompleta retorna `REJECT` | riscos, ponto de equilíbrio, resposta bruta, modelo e custo |
| Persistência | somente campos vazios recebem valores aprovados | valor existente do usuário nunca é sobrescrito | nova versão `ATENA_PLUTUS` com snapshot auditável |
| Governança | hipótese versionada fica disponível a projeções | aprovação não libera gasto, campanha ou preço público | teto e realizado permanecem inalterados |
| Segregação | cada plano usa seu snapshot e histórico | MUSA não altera Agenda Cheia e vice-versa | IDs do plano em todas as execuções |
| Frontend | desktop e mobile mostram comando e andamento | falha exibe diagnóstico sem simular conclusão | atualização periódica e histórico persistido |

