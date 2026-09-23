# Homologação — comunicação privada compatível de Mira

Data: 23/09/2026.

## Escopo confirmado

- Produto Mira #10, tipo oficial `AI_PRODUCT`, referência
  `product:10@agent-validation-v1`.
- Cadeia #18, processo `pde-communication-sales-journey` #85/v8, execução #15.
- Comunicação de Íris: tarefa #411, instância #273, processo #63/v7, custo
  estimado USD 0,285604.
- Criativos: subprocesso #64/v8 concluído e reutilizado pela instância #352.
- Nenhuma publicação, cobrança, campanha ou gasto de mídia pertence a este escopo.

## Causa comprovada

A cadeia v18 manteve o contrato executável da v7 e detalhou apenas os objetivos dos
cinco critérios comerciais. A tela reconhecia #411 como prova vigente, enquanto o
executor do destino exigia o ID #85 e recusava a origem #63. A integração, por sua
vez, reconhecia ciclo privado, mas não a referência privada anterior ao experimento.

## Alternativas comparadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Reexecutar Íris na v8 | Produz novo ID | Repete custo com entradas e resultado ainda válidos | Rejeitada |
| Remover a exigência de versão | Mudança pequena | Pode aceitar processo ou grafo incompatível | Rejeitada |
| Compatibilidade estrutural com linhagem | Reutiliza a prova e falha fechada em mudança material | Exige validação determinística adicional | Adotada |

## Matriz local

| Caso | Aceite |
| --- | --- |
| v7 → v8 canônica | Reutiliza tarefa/instância e registra as duas definições |
| Mesma v8 | Mantém comportamento anterior e idempotência |
| Processo, versão, agente, recurso, subprocesso ou fluxo divergente | Bloqueia antes de gravar |
| Resultado ou SHA-256 substituído | Bloqueia antes de gravar |
| Destino seguido de integração privada | Conclui sem plano, experimento, slot ou avanço comercial fabricado |
| Rota comercial/ciclo existente | Preserva validações e avanço anteriores |
| Repetição do comando | Não duplica ocorrência nem tarefa paga |

## Resultado local

- 42 testes focados dos contratos, destino, integração, persistência e replay:
  42 aprovados, sem falhas ou erros.
- Suíte completa do backend: 3.424 testes executados, sem falhas ou erros;
  21 cenários condicionais foram ignorados pelas condições documentadas de suas fixtures.
- Spotless e verificação de whitespace do diff aprovados.
- Nenhuma interface foi alterada; a validação visual em desktop e celular fica reservada à
  retomada da mesma tela publicada após o deploy do backend.

Os testes locais comprovam mecanismo e prevenção de recorrência; não comprovam
venda, utilidade humana ou lucro.
