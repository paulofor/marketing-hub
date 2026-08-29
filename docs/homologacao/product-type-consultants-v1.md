# Matriz de homologação — tipos Consultor PWA e Consultor WhatsApp v1

## Objetivo

Comprovar localmente que o Marketing Hub possui duas bases comerciais claras e reutilizáveis para
consultoria com IA, sem duplicar a Fluorita existente, sem misturar dados de clientes e sem depender
de PR, deploy, API direta da OpenAI ou publicação para descobrir defeitos.

## Gargalo, evidência e decisão

- Gargalo real: o catálogo possuía a Fluorita conversacional, mas não explicava na tela como
  construir o produto e não possuía um contrato distinto para experiência PWA.
- Evidência: `AI_SANDBOX_CONVERSATIONAL_PRODUCT` e o produto `Especialista no WhatsApp` já estavam
  persistidos; não existia `AI_PWA_CONSULTANT_PRODUCT`.
- Resultado esperado: Fluorita evolui sem perder identidade nem produtos; Turmalina nasce ativa;
  ambas mostram base completa e apontam para o mesmo SDK Java, com React somente na PWA.
- Continuar: duas bases completas, CRUD íntegro, mídia segregada, memória isolada e UI responsiva.
- Ajustar: qualquer base incompleta, erro de contrato, resposta genérica ou fricção de uso.
- Parar: mistura entre clientes, uso direto de API, perda de vínculo histórico ou migração não
  retomável.

## Alternativas consideradas

| Alternativa                                           | Benefício                                        | Risco/custo                                     | Decisão   |
| ----------------------------------------------------- | ------------------------------------------------ | ----------------------------------------------- | --------- |
| Criar dois tipos do zero                              | Nomes exatamente novos                           | Duplica a Fluorita e fragmenta métricas         | Rejeitada |
| Manter um único tipo multicanal                       | Menos cadastro                                   | Esconde diferenças de jornada, integração e SDK | Rejeitada |
| Evoluir Fluorita e criar Turmalina sobre núcleo comum | Preserva histórico e reutiliza segurança/memória | Exige adaptadores explícitos por canal          | Escolhida |

## Matriz local

| Área               | Caminho feliz                                                                 | Validações e falhas                                                                           | Evidência exigida                                      |
| ------------------ | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| Migração MySQL 5.7 | Adiciona blueprint, atualiza Fluorita e cria Turmalina                        | Reaplicação não duplica tipo/apelido; schema parcial bloqueia                                 | Liquibase aplicado duas vezes e consultas assertivas   |
| Backend            | Lista, inclui e edita base; calcula prontidão                                 | Tipo ativo novo incompleto é rejeitado; PWA sem SDK React é rejeitada; código em uso não muda | Testes unitários e HTTP                                |
| Catálogo React     | Exibe base, faltas, produtos vinculados e formulário completo                 | Loading impede duplo envio; falha continua visível; estado vem do backend                     | Vitest, typecheck e build                              |
| Responsividade     | Uso completo em desktop                                                       | Sem corte, overflow ou controle inacessível em iPhone 15 Pro e Pixel 7                        | Playwright e screenshots locais                        |
| SDK Java           | Executa texto e imagens via Codex App Server                                  | Hash, tipo, tamanho, symlink e escopo inválidos bloqueiam antes do modelo                     | Testes com servidor falso e handshake real sem turno   |
| SDK de consultores | Compartilha núcleo e separa PWA/WhatsApp                                      | Canal não pode alterar identidade do tipo; prompt de agente e atividade permanecem auditáveis | Testes de contrato e schema                            |
| SDK React          | Captura texto/foto e apresenta estados de processamento, erro e orientação    | Arquivo inválido e envio vazio bloqueados; não chama App Server nem banco                     | Typecheck, build e jornada visual com transporte falso |
| Memória e dados    | Mesmo cliente retoma contexto autorizado                                      | Cliente, produto, tenant ou conversa divergentes bloqueiam; workspace é descartado            | Suíte de isolamento do harness                         |
| Observabilidade    | Resultado contém modelo, versões, hashes, tokens e IDs                        | Falha conserva categoria e contexto sem expor memória ou mídia em log                         | Testes de auditoria                                    |
| Métricas           | Contrato lista entrada, orientação, utilidade, retorno, venda, custo e margem | Dados QA ficam segregados e não contam como vendas                                            | Blueprint persistido e inspecionado                    |

## Dados de teste

- usar somente identificadores `qa-tenant`, `qa-product`, `qa-customer-a` e `qa-customer-b`;
- usar imagem sintética pequena e sem pessoa real;
- nenhum evento QA pode atualizar métricas comerciais do Rigel ou ser interpretado como venda;
- servidor falso do App Server deve provar o envelope e a imagem sem abrir turno externo.

## Regra das rodadas

Se a primeira rodada revelar defeito, corrigir a causa-raiz e executar, após a última correção, duas
rodadas locais completas e consecutivas. Qualquer novo defeito reinicia a contagem.
