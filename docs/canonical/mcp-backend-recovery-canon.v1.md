# Recuperação controlada do backend via MCP — cânone v1

## Objetivo

Permitir que o MCP recupere a disponibilidade do backend principal sem SSH interativo, shell genérico, deploy ou troca de imagem.

## Contrato obrigatório

- A operação aceita somente o módulo canônico `backend`.
- Host e container são definidos por configuração versionada e precisam pertencer à allowlist operacional.
- A chamada exige a confirmação literal `RESTART_BACKEND` e justificativa auditável.
- O health canônico deve ser consultado antes; backend saudável não pode ser reiniciado.
- Reinícios devem respeitar cooldown para evitar loops de recuperação.
- Após o restart, o MCP deve aguardar e consultar novamente o health, retornando evidências anteriores e posteriores.
- A ferramenta não pode receber comando, host ou nome de container livres.
- Recuperação operacional não autoriza deploy, alteração de imagem, publicação, gasto ou mudança de dados comerciais.

## Critério de sucesso

A operação termina como `recovered` somente quando o backend volta a responder com HTTP 2xx. Caso contrário, retorna `restart_unhealthy`, preservando o diagnóstico para intervenção posterior.
