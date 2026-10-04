# Alcyone — preparação comercial e contexto de Atena — 04/10/2026

## Escopo e critérios de aceite

Produto #11, tipo Safira/AI_PRODUCT; Alcyone é nome interno. Processo 4 v11 (#113),
plano #34, origem de descoberta #71/dossiê #46. Reutilizar homologação interna #583–587;
não declarar prova comercial, alterar preço, autorizar mídia ou reescrever pareceres antigos.
Atena #17 recomendou “Look para a Ocasião”, mantendo INSUFFICIENT_EVIDENCE por ausência
de produção comercial e economia integral e falha técnica ao confirmar fontes em Chromium.

## Matriz local, definida antes dos testes

| Caso | Aceite |
| --- | --- |
| Plano carregado assincronamente | Pergunta de Atena corresponde ao plano selecionado; nenhum diagnóstico do fallback |
| Troca de plano, entrada manual e atualização | Não transportar pergunta de outro plano; preservar edição no mesmo plano |
| Plano ausente ou leitura com falha | Não exibir outro plano nem permitir iniciar pesquisa sem identidade |
| MCP initialize/list/call | Ferramenta explícita somente leitura; correlação da execução e JSON válido |
| Fonte pública válida | Chromium retorna URL final, data, status, texto e hash auditáveis |
| Fonte indisponível e timeout | Falha por URL preservada, sem pesquisa/modelo repetidos automaticamente |
| URL privada, credencial, redirecionamento ou escrita | Bloqueio antes do acesso; não enviar POST nem acessar rede interna |
| Empacotamento e processo | MCP chama navegador próprio sem shell do modelo; sandbox do Codex preservada |
| Regressões | Unitários dos módulos afetados, build, formatação e contratos de isolamento |
| Interface | Desktop, iPhone 15 Pro e Pixel 7 com dados simulados e sem consumo pago |
| Produção | PR, checks, merge, deploys e identidade/saúde; depois validar formulário e ferramenta publicada |

QA usa planos sintéticos isolados, servidor local e navegador sem credenciais, sem eventos de
venda, chamadas de modelo, publicação de campanha ou pagamento. Teste de navegador do MCP usa
interceptação local de uma origem pública fictícia; não relaxa o bloqueio de rede privada.

## Evidência e alternativas

A pergunta inicial da tela reteve o diagnóstico fixo do fallback após a chegada do plano real.
O parecer #17 registrou falha bwrap ao executar o navegador por shell dentro do Codex read-only.
As três URLs decisivas responderam HTTP 200 no Chromium da sandbox.

Alternativas: anexar apenas um relatório manual resolve esta execução, mas mantém recorrência;
pré-capturar toda fonte limita a pesquisa adaptativa e pode repetir acesso; expor o navegador
existente por uma ferramenta MCP de leitura permite seleção de fontes com contrato limitado.
Escolhida a terceira, preservando a sandbox e sem criar outro serviço ou liberar shell genérico.

Fontes de integração consultadas em 04/10/2026:
- https://learn.chatgpt.com/docs/extend/mcp?surface=cli
- https://developers.openai.com/plugins/build/mcp-server

## Validação local

- Frontend: suíte completa com 857 testes aprovados; após acrescentar a proteção de ID
  não persistido, cinco regressões específicas do painel passaram (total vigente: 858).
- Worker Java: 45 testes, nenhum erro/falha; Spotless aprovado. Métodos e classes alterados
  mantêm comentários de responsabilidade em português.
- Node/MCP: seis testes aprovados, incluindo Chromium real com respostas locais, falhas,
  URL privada, POST e redirect bloqueados. Erro UTF-8 da fixture foi corrigido; limites
  de processos da sandbox exigiram rodar navegador depois da suíte, sem relaxar proteção.
- TypeScript, build de produção, Prettier, sintaxe Node e diff aprovados.
- Imagem construída pelo Dockerfile versionado, com MCP e navegador efetivos do pacote:
  as três fontes reais retornaram READ/HTTP 200. Nenhuma chamada de modelo ou provedor pago.
- Matriz visual: `infra/testing/atena-plan-question/browser.cjs` passou contra o bundle
  em desktop, iPhone 15 Pro e Pixel 7. Confirmou contexto, endpoint, edição manual,
  erro visível e bloqueio de campo vazio, com zero chamadas reais de agentes.

Hashes SHA-256 do texto completo lido pela ferramenta empacotada em 04/10/2026:

| Fonte | Hash |
| --- | --- |
| Resolva Meu Look / book-online | `0e8e6dd4d7bdb32b3b8295af7a87f42ede9ee47e889b49ae0aaf101b57af1174` |
| Google Play / Style DNA pt-BR | `c6e510d41261ab098bc2ebfdb13309bd87b353e13c1ff8ac507c095b11152698` |
| Google Play / Dressly | `cf1ad2d2879d2d8dfa315d5d0a118619e7e23728a990ca558b11fe7721c9761d` |

Antes, Atena não conseguia confirmar as fontes pelo shell aninhado. Depois, a ferramenta
empacotada lê as três e preserva falhas parciais sem novo modelo. O parecer #17 não foi
reescrito nem repetido: corrigir acesso não supre a prova comercial e os custos faltantes.
Custo incremental da pesquisa Codex não informado; não atribuir zero ao desconhecido.
Ainda não há amostra para comparar custo por tarefa concluída ou ganho de vendas.

## Estado comercial confirmado

O cadastro do produto #11 usa “Look para a Ocasião”; Alcyone continua como nome interno.
Experimento #97 PLANNED, preço hipotético R$79, sem mídia, janela, amostra, checkout ou campanha.
Plano #34 v3 BLOCKED vinculado ao #97; custos privados anteriores foram preservados no histórico,
e custos comerciais desconhecidos permanecem ausentes. A revisão financeira inicial #11
(versão 1) foi preservada; a atual #12 (versão 2) confirma três resultados incluídos e mantém
MISSING_INPUTS, sem solicitação de análise. Não houve mudança no contrato interno homologado.

Processo #113 v11, referência `experiment:97`, 0/4 atividades: contrato de comunicação depende
agora de parecer econômico e prova funcional comercial. Íris permanece bloqueada; não existe
aprovação comercial fabricada a partir do teste de interface. STOP preservado. A preparação
e as correções não comprovam venda, contribuição ou preferência humana. PR, SHA e evidências
de publicação devem ser conferidos no registro de entrega, após os workflows concluírem.
