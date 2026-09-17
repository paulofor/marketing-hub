# Recuperação da tarefa #435 — prazo econômico Opala

## Estado confirmado

- Produto Vega `4`, ciclo `2`, experimento `92`, processo `opala-commercial-preparation-v1` v1.
- Tarefa `435`, atividade `economics`, Plutus, preservada como `BLOCKED` em produção.
- Parecer funcional: `ADJUST`; custo auditado: USD 0,3851.
- Divergência: `economics.deadline=2026-09-17T02:59:00Z`, enquanto o consumidor exige
  `YYYY-MM-DD`.
- O contexto de entrada continha o mesmo instante em `opalaCommercial.windowEnd`; o prompt v1 não
  dizia como convertê-lo e o schema legado aceitava qualquer string.

## Decisão

| Alternativa | Benefício | Risco/esforço | Decisão |
|---|---|---|---|
| Aceitar timestamps em todo o domínio | Evita esta rejeição | Amplia contratos backend e gates sem necessidade; alto risco | Rejeitada |
| Truncar a resposta após o modelo | Recuperação simples | Altera o resultado auditado e mascara divergência de contrato | Rejeitada |
| Contrato Opala date-only antes da inferência | Evita consumo inválido e mantém auditoria | Migração coordenada de prompt, schema e worker; esforço médio | Escolhida |

## Matriz local definida antes da homologação

| Área | Casos |
|---|---|
| Caminho feliz | Opala válido conclui callback com prompt e schema fixados |
| Validação | Timestamp completo é recusado; `YYYY-MM-DD` é aceito; contribuição continua reconciliada |
| Falhas | Drift de Atena, contrato ausente, orçamento privado e contribuição divergente preservam bloqueio |
| Integração | `pending` canônico, prompt v2 ativo, callback correlacionado e schema empacotado |
| Observabilidade | resposta bruta, prompt, tokens, custo, tarefa e origem continuam auditáveis |
| Segregação | cenários usam IDs sintéticos e não escrevem em produção |
| Persistência | MySQL 5.7 aplica v1 + v2, preserva a versão anterior e ativa somente economics v2 |
| Comercial | nenhuma cobrança, publicação, acesso ou mídia é criada; `ADJUST` continua sendo risco comercial, não falha técnica |

## Critério de aprovação

Duas rodadas consecutivas sem falha do worker e da fixture MySQL 5.7, validação estática Liquibase,
formatação, empacotamento e revisão do diff. A tarefa produtiva não deve ser repetida antes da
publicação conjunta do backend e de Plutus.

## Resultado da homologação local

- Duas rodadas consecutivas aprovadas após a correção.
- Em cada rodada: 45 testes do Plutus e 20 testes de persistência do Catálogo Vivo em MySQL 5.7,
  todos sem falhas, erros ou testes ignorados.
- Validação estática do Liquibase e Spotless aprovadas.
- Imagem final do Plutus construída pelos arquivos versionados e aprovada em 12 cenários de smoke,
  incluindo Opala válido, rejeição do timestamp da falha original, drift, ausência de contrato,
  contribuição divergente e estouro do envelope privado.
- O Compose temporário e seus volumes foram removidos ao final.
- Nenhuma chamada paga, escrita em produção, cobrança, acesso, mídia, PR ou publicação foi feita.
