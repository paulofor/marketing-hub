# Matriz de homologação — Orquestrador de Agentes v1

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Três pareceres concluídos no mesmo experimento | `READY_FOR_HUMAN_DECISION`, sem ação automática |
| Validação | Plano sem experimento | Conflito explícito, sem caso persistido |
| Dependências | Parecer ausente ou em execução | `WAITING_FOR_AGENTS` com próximo bloqueio factual |
| Falha | Estrategista ou Operador falha | `BLOCKED` com causa auditável |
| Gate comercial | Criativo `ADJUST`, `REJECTED` ou `FAILED` | `BLOCKED`; publicação não liberada |
| Integração | Snapshot do Operador aponta para outro experimento | `BLOCKED`; parecer não reutilizado |
| Idempotência | Sincronizar novamente plano e experimento | Mesmo caso atualizado; nenhuma duplicidade |
| Observabilidade | Toda sincronização | IDs, estados, aprovação humana e horário em JSON |
| Métricas | Casos por estado e divergência | Consultáveis pelos registros persistidos |
| Segregação | Dois experimentos | Chaves e evidências independentes |
| Segurança | Todos os pareceres concluídos | Ainda exige decisão humana para gasto/publicação |
| Navegadores/dispositivos | API backend sem nova interface | Não aplicável à v1; futura UI deverá cobrir desktop e mobile |

Uma rodada é aprovada apenas quando testes, compilação, formatação dos arquivos alterados e revisão
do diff passam em conjunto. Qualquer defeito reinicia a contagem das cinco rodadas consecutivas.
