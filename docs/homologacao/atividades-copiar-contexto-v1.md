# Copiar contexto dos cards de atividades

Solicitação: 10/09/2026. Validação: 11/09/2026 UTC.
Escopo: cards da tela de atividades do produto na Cadeia de Valor.

## Decisão e contratos

O ícone deve ficar imediatamente ao lado do título, conforme a imagem do usuário.
O texto identifica processo e atividade com seus números oficiais, produto pelo nome
interno, agente pelo nome do responsável cadastrado e ciclo quando identificado.
IDs, versão do processo, experimento, versão do produto e link contextual ajudam a
distinguir passagens sem enviar prompts, resultados ou dados pessoais das tarefas.

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Copiar somente o título | Implementação mínima | Perde produto, processo e ciclo | Não |
| Abrir formulário para escolher os campos | Permite personalização | Mais cliques e risco de omitir contexto | Não |
| Um clique com contexto oficial e confirmação local | Pronto para colar no prompt | Exige tratar HTTP e bloqueio da área de transferência; esforço baixo | Sim |

O frontend reutiliza contratos existentes: `activity-executions` de
`businessprocess.execution.controller.BusinessProcessActivityExecutionController`,
com service canônico e records em `service.productProcessExecutions`;
`/api/products/value-chain-positions/{productId}` para a numeração oficial;
`/api/business-process-chains/learning-cycles/v1/products/{productId}/process-context`
para a identidade do ciclo. A cópia apenas formata esses dados no navegador, sem
comando de backend, criação de tarefa ou alteração de estado de negócio.

## Matriz definida antes da implementação e dos testes

| Controle | Critério de aceite |
| --- | --- |
| Localização | Ícone junto ao título de cada card, inclusive bloqueados e históricos |
| Contexto | Processo e atividade numerados; nomes internos de produto e agente; IDs e versão distintos dos números ordinais |
| Ciclo | Copia o ciclo efetivo informado pelo backend, inclusive entrada sem parâmetro; mantém cadeia, experimento e versão; não inventa ciclo ausente |
| Link | Abre o produto, processo e card copiado com cadeia e ciclo, mesmo se a página estiver ancorada em outro card |
| Ausências | Nome interno ou número não informado aparece explicitamente; atividade humana/backend não se transforma em agente |
| Cópia real | Colar reproduz o texto completo com acentos e quebras de linha em contexto seguro e HTTP comum |
| Falhas e observabilidade | Confirmação só após sucesso; erro visível com texto selecionável, nova tentativa e foco preservado |
| Acessibilidade | Nome acessível, teclado, confirmação anunciada e alvo de toque em mobile; sem deslocar a navegação |
| Integração e isolamento | Build real e APIs locais simuladas conforme contratos; nenhuma escrita, modelo, campanha ou métrica comercial de teste |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação; card sem overflow |
| Regressão | Testes da tela e componentes afetados, TypeScript, build, formatação e revisão do diff |

Uma rodada completa sem defeitos encerra a homologação. Se uma rodada revelar defeito
e houver correção, serão executadas duas rodadas completas consecutivas após a última
correção. A publicação não é usada como teste.

## Evidência da investigação

A abertura da página pública do Vega pela sandbox confirmou `Atividade 3.6`,
`Corrigir o protótipo a partir do parecer` e `Responsável: Dédalo`. Os três contratos
acima responderam HTTP 200. O navegador confirmou `isSecureContext=false` e
`navigator.clipboard` ausente, justificando a cópia alternativa por seleção no HTTP.
Não foi necessário alterar banco, backend, worker ou contrato de API.

O texto usa `productInternalName`, `activityOwnerName`, `sequenceLabel`, a sequência
da atividade e a identidade oficial do ciclo. O link é montado para o próprio card,
com uma lista explícita de parâmetros (`learningCycleId` e `chainId`); não reaproveita
a âncora de outro card nem parâmetros desconhecidos da página. IDs e versão técnica
aparecem separados dos números ordinais do BPM. Responsáveis humanos e backend são
identificados como tal, sem inventar agente.

## Ajuste encontrado na validação

A primeira execução dos testes existentes encontrou três falhas: o componente adicionava
um `role=status` vazio para cada card, deixando ambígua a confirmação de criação de
tarefa. A região de anúncio permanece montada, mas o papel de status só aparece quando
há confirmação real de cópia. O acompanhamento existente continuou passando sem alteração
dos seus testes unitários. O roteiro anterior de ação única foi ajustado para contar apenas
botões da região de execução, conservando sua regra de uma ação operacional por card.

## Resultado local

Runner reproduzível: `bash infra/testing/activity-context-copy/run-round.sh <rodada>`.

| Verificação | round1 | round2 |
| --- | --- | --- |
| Testes da tela, painel, acompanhamento e consulta | 32 aprovados | 32 aprovados |
| TypeScript e build do frontend | Aprovados | Aprovados |
| Clipboard real em desktop, iPhone e Pixel; HTTP e contexto seguro | 6 combinações aprovadas | 6 combinações aprovadas |
| Formatação e diff | Aprovados | Aprovados |

As duas rodadas completas e consecutivas passaram após a última correção. A inspeção
visual confirmou o ícone ao lado do título no desktop e a área de toque ampliada no
celular. Confirmação e seleção manual ficam junto ao comando. Os servidores locais
e navegadores de teste foram encerrados ao final das rodadas.

Cada combinação de navegador verifica ciclo identificado automaticamente, produto interno,
link do próprio card e sua reabertura, agente, atividade humana, atividade histórica,
responsabilidade do backend, outro produto sem ciclo, ausência de campos, carregamento,
teclado, foco, layout, erro explícito e nova tentativa. Em contexto seguro também é negada
a permissão moderna para comprovar que a cópia alternativa funciona de verdade.
Colar por `Control+V` comprova o conteúdo da área de transferência; não é apenas um spy
de `writeText`. Em todos os cenários de erro, o texto completo continua selecionável.

Os contratos de API são simulados localmente e o frontend usa seu build real. O teste
recusa escritas e qualquer conexão externa; não há tarefas, custos de IA, sessões,
vendas ou métricas comerciais de teste em produção. O navegador não consulta o banco.
Não houve alteração Java ou Liquibase, nem necessidade de topologia Docker.

Logs, texto efetivamente colado, capturas e `results.json` ficam em
`artifacts/activity-context-copy/<rodada>/`, ignorados pelo Git. Os roteiros ficam
versionados. iPhone e Pixel são emulações de Chromium, sem validação em Safari físico.
O build apresenta avisos já existentes de tamanho do bundle e API CJS do Vite, sem
falha de compilação. Não houve commit, PR, deploy ou publicação.
