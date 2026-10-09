# Decisão do operador nas entradas dos agentes — 09/10/2026

## Evidência e causa

No sucessor de Capella, ciclo 11/experimento 104, as tarefas 668, 669 e 670
receberam prompts de 80.715, 98.601 e 119.031 caracteres. Nenhum continha
`commercialNotes`, o marcador da aplicação explícita no sucessor ou o consumo
anterior informado na ficha. Plutus #669 aprovou apenas preparação privada e
declarou que não conseguira conferir o alcance e o consumo do teto. A decisão
estava persistida no produto; faltava transportá-la pelo contrato canônico.

## Alternativas e escolha

| Caminho | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Copiar decisão para cada tarefa | Correção imediata pontual | Repasse manual, fonte duplicada e próxima tarefa perde a decisão | Não |
| Criar outro controle financeiro | Automatização mais ampla | Duplica persistência e exige contrato de reservas fora deste defeito | Não |
| Acrescentar a fonte existente ao pending e à lease | Mesma ficha, identidade explícita, recuperação sem nova reserva | Campo JSON opcional e consulta isolada | Sim |

`operatorGuidance` contém `PRODUCT_OPERATOR_GUIDANCE_V1`, produto, referência,
horário e notas completas. Não interpreta texto como saldo conciliado, concessão
automática ou novo limite. O executor continua conferindo decisão, escopo e custos
na fonte; autorização de um produto não se transfere para outro. Nenhum campo
anterior, alvo ou contexto de experiência é substituído. A reconsulta da lease
recebe as notas atuais sem nova inferência. Fontes fixadas por atestações de outros
produtos permanecem intactas.

## Matriz definida antes dos testes

| Caso | Prova exigida |
| --- | --- |
| Decisão de Capella | Texto e teto cumulativo chegam no JSON do pending |
| Outro produto/referência | Somente suas próprias notas são entregues |
| Alvo inconsistente ou ausente | Nenhuma consulta por nome ou vazamento de notas |
| Fonte sem notas | Formato anterior preservado; autorização não é inventada |
| Atualização e recuperação | GET da lease recebe a nova fonte sem reservar outra tarefa |
| Compatibilidade de consumidores | Campos originais continuam no nível superior; contexto e versão preservados |
| Auditoria real após publicação | Próxima atividade autorizada persiste o marcador em execution_prompt |

Executar unidades e MVC reais do backend, arquitetura e empacotamento das evidências
vigentes. Conferir o prompt da próxima tarefa natural; não abrir chamada paga só
para testar o transporte. Não há alteração de experiência, hipótese ou oferta por
esta mudança de entrada; o mesmo ciclo 11 continua. QA continua segregado de venda.

Validação local concluída: 222 testes de backend/MVC/arquitetura aprovados, seis
regressões específicas, formatação dos arquivos alterados e as 93 atestações
vigentes preservadas. Uma verificação sem rede serializou o DTO HTTP novo e o
entregou ao compositor real de prompt `PdeEconomicsBpmTaskConsumer` de Plutus:
produto, fonte e decisão chegaram ao texto operacional, com zero inferências.
O request real da próxima atividade continua sendo a conferência pós-publicação.
