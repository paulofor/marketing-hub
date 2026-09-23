# Aperfeiçoamento de agentes nos prompts do AIHUB — v1

Decisão de 14/09/2026. Objetivo: transformar impedimentos comprovados em melhorias
reutilizáveis dos agentes do Marketing Hub, preservando qualidade, custo, evidências e
autorizações. A melhoria deve favorecer a entrega de valor e a operação comercial;
sucesso em testes não demonstra, por si só, aumento de vendas.

## Contrato do prompt

O modelo compartilhado fica em
`frontend/src/pages/product/prompts/process-aihub-help.v1.md`. O componente
`ProductProcessContextCopy.tsx` acrescenta a fotografia oficial do processo uma única vez.
O botão, a prévia e a cópia manual usam esse mesmo texto. Esta alteração orienta futuras
solicitações ao AIHUB; não instala novos agentes nem muda pesos, filas ou modelos.

O pedido autoriza as correções causalmente relacionadas, inclusive nos workers envolvidos,
e sua entrega completa pelo próprio modelo após validação local. Melhorias adicionais
ficam como sugestões fundamentadas. Imagens produtivas devem ser produzidas pelos arquivos
versionados do repositório. Não publicar por SSH nem usar publicação como teste.

## Cinco pontos em toda oferta — decisão de 22/09/2026

O texto compartilhado dos botões **Prompt para AIHUB**, incluindo prévia e cópia manual,
deve lembrar explicitamente os cinco critérios de
[`cadeia-produtos-pde-canon.v1.md`](cadeia-produtos-pde-canon.v1.md#cinco-critérios-concretos-de-criação-e-avaliação--decisão-de-22092026):
desejo reconhecido, primeiro passo fácil, valor antes do compromisso, continuidade paga
compreensível e repetição com margem. Para cada um, orientar entregável concreto, evidência
de aceite e métrica na atividade responsável, reutilizando resultados válidos e respeitando
tipo, ficha, oferta e orçamento aprovados. O lembrete não fixa produto, processo ou versão
histórica nem cria novos gates ou subprocessos automaticamente.

Degustação depende do plano aprovado, com benefício, limites e custos explícitos; quando
não aplicável, exigir demonstração ou amostra real adequada ao produto e registrar o motivo.
Resultado exibido não comprova utilidade nem exposição efetiva à oferta. Participação e
cadastro são sinais intermediários, não vendas rentáveis; custos ou métricas sem fonte
permanecem desconhecidos. Os critérios orientam criação e avaliação, sem alegar eficácia
causal comprovada pelo experimento que motivou o aprendizado.

## Autonomia até o objetivo do processo — decisão de 21/09/2026

O prompt autoriza explicitamente o modelo a criar ou atualizar manualmente os PRs necessários
pelo conector GitHub, API oficial ou `gh`, sem novo pedido ou clique em **Pedir PR**. Esta decisão
substitui, para esse pedido, as orientações antigas de PR executado pelo usuário, publicação
somente após nova solicitação e CLI apenas para leitura. Restrições explícitas de somente local
ou não publicar continuam válidas. Revisões obrigatórias e proteções da branch permanecem:
consultar revisões reais, aprovar somente quando a identidade tiver permissão e não for autora,
nunca usar bypass nem desabilitar checks.

Antes da execução, publicar um checklist com `update_plan` e objetivos verificáveis; atualizar
o mesmo plano a cada conclusão ou mudança de escopo. Se a ferramenta estiver ausente, declarar
essa limitação e manter checklist textual, sem afirmar que chamou uma ferramenta indisponível.
Resumos públicos contêm apenas ação e **Objetivo:**, sem raciocínio interno. Confirmar produto,
tipo, processo/versão, cadeia/ciclo, execução, atividades/tarefas e subprocessos com retorno ao pai
no backend. Campo ausente permanece desconhecido até consulta; nomes não substituem IDs.

Executar atividade por atividade e tarefa por tarefa até comprovar todos os objetivos do
processo corrente e dos subprocessos necessários. O backend continua decidindo o avanço.
Para cada impedimento, resolver todos os defeitos relacionados localmente, validar o fluxo
completo e revisar o diff antes de commit/push. Reutilizar o PR da correção; só abrir outro
se o anterior já foi integrado ou se tratar de entrega distinta. Após merge na `main`,
acompanhar workflows do PR, da main e encadeados, correlacionados ao HEAD/SHA correto,
até conclusão e verificação da versão/saúde e do comportamento publicado. Só então retomar
pela tela a atividade afetada e seguir para as próximas; não usar PR/deploy como teste.

Falhas de Actions exigem logs, correção e validação local antes da atualização ou PR de
correção. Rerun sem mudança exige evidência de transitoriedade. Fluxo obrigatório ausente,
pendente, cancelado, expirado, com falha ou aguardando aprovação impede declarar entrega;
`skipped` só é aceito por condição documentada não aplicável. Sem acesso ou com revisão
obrigatória de outra identidade, preservar evidências e informar a ação mínima necessária.

Ao retomar, confirmar entregas já integradas/publicadas antes de produzir efeito externo.
Preservar o vínculo solicitação → PR → SHA na main → runs/deploys → evidência funcional.
Histórico raso deve ser completado antes de concluir divergência; `No commits between`/422
exige conferir diff, PR e publicação, nunca criar commit vazio ou PR redundante. Separar falha
funcional, pendência de deploy e erro de encerramento; não sobrescrever resultado comprovado
com erro de tentativa redundante nem declarar sucesso sem comprovação. Essas instruções não
alteram o reconciliador/status das solicitações do AI Hub nem corrigem registros históricos.

### Aprendizado fundamentado no acervo

Consulta de 21/09/2026 ao [radar de 18/09/2026](../../pesquisas/agentes-inteligentes/2026-09-18-agentes-inteligentes.md),
seções 2–4, com verificação das fontes primárias:

- [RAFT](https://arxiv.org/abs/2609.20754): recuperar histórico pela etapa e estado do caso,
  além do tema. Aplicação proposta: comparar tentativas equivalentes antes de reaproveitar solução.
- [SkillAA](https://arxiv.org/abs/2609.20455): contrastar falhas e sucessos, localizar a parte
  responsável e validar alteração localizada. Aplicação proposta: candidata pequena, regressões e rollback.
- [SoL-Pi](https://nvlabs.github.io/SoL-Pi/): avaliar melhorias do harness contra uma referência,
  preservando capacidade. Aplicação proposta: comparar qualidade, custo e latência sem aceitar
  economia que degrade o objetivo funcional.

Registrar experiência com objetivo/estado, fonte/data, versões, causa confirmada, tentativa,
resultado, teste preventivo, limites de validade e critério de revisão. Recuperar somente
experiências pertinentes e tratar contradições antes de reutilizá-las. Avaliar a candidata
contra a anterior com falhas históricas, sucessos preservados e casos independentes quando
disponíveis; registrar ausência destes. Aceitar/rejeitar com evidência e retorno disponível.
Não persistir hipótese como fato, alterar pesos ou prometer aprendizado automático só pela
edição do prompt. Os resultados externos dos artigos não são resultados medidos do Hub.

Na descoberta de produtos, a memória de Argos deve ser recuperada pela candidata, pergunta
pendente e estado da etapa. A execução seguinte recebe candidatas e evidências anteriores, relatos
consentidos, consultas já executadas e limites; não recebe permissão para renomear candidatas,
reiniciar a pesquisa ampla ou converter entrevista qualitativa em prevalência. Comparar resultado
anterior, nova evidência, contraponto e ausência de progresso torna a melhoria observável sem criar
aprendizado automático fictício.

Referência interna consultada em 23/09/2026:
`pesquisas/agentes-inteligentes/2026-09-10-agentes-inteligentes.md`, especialmente AgentGrad e
Q2D-Web. A aplicação adotada é localizada: modificar apenas a etapa de Argos cuja lacuna foi
confirmada e auditar as consultas que o próprio agente formulou, separando plano, execução e
resultado. Os benchmarks citados pertencem a outros domínios e não comprovam ganho de vendas nem de
desempenho no Marketing Hub.

## Foco no produto e no processo correntes — decisão de 18/09/2026

O template compartilhado deve ser direto e independente de um caso: produto, tipo, formato,
identificadores, versões, tarefas e impedimento concreto vêm do contexto oficial, cuja
atualidade deve ser confirmada. Retirar exemplos obrigatórios de um tipo e listas extensas
de conceitos do texto copiado; os detalhes permanecem nos cânones para consulta pertinente.
Não cortar o contexto oficial nem inferir estado de negócio na interface.

O pedido autoriza investigação, implementação e ajustes locais causalmente relacionados,
sem reconfirmar a cada defeito. Resolver o fluxo atual e prevenir a mesma causa nas fontes
compartilhadas, melhorando os agentes envolvidos quando houver evidência e avaliação.
Melhorias fora do escopo ficam como sugestões; não exigir nova arquitetura ou envolver
todos os agentes em toda recuperação. Preservar rentabilidade, tipo/ficha aprovados,
autoridade do backend, gates e consentimento para ações externas.

A síntese conceitual abaixo serve como referência seletiva. O template não precisa enumerar
os conceitos para continuar aplicando o ciclo diagnóstico, candidata, avaliação e aprendizado.

## Compatibilidade da entrega antes da revisão paga — decisão de 18/09/2026

O prompt compartilhado deve exigir versões esperadas e observadas do produto, evidências e
agentes na execução, com compatibilidade comprovada, sem exigir o mesmo commit entre módulos.
Comparadas as alternativas de apenas reforçar Watchdog, apenas centralizar instruções no
Catálogo Vivo ou verificar a entrega integrada, adota-se a terceira: maior esforço de integração,
mas cobre a divergência entre instruções, artefato e experiência; as duas primeiras são apoio.
Evitar falsos bloqueios por commits diferentes usando os contratos compatíveis como critério.

Antes de inferência paga, verificar identidade do build, vínculo entre manifesto, artefato
imutável e capturas, e comportamento essencial. Hash, health ou sucesso do deploy isolados
não comprovam funcionamento. Divergência deve gerar bloqueio técnico persistido no backend,
com versões, responsável e ação, sem reprovação comercial paga. O backend libera a continuação
após comprovação, preservando resultados válidos, orçamento, aprovações e idempotência.

Homologar localmente construção, empacotamento, entrega e captura como fluxo integrado,
incluindo divergência, bloqueio pré-inferência e recuperação. Revisar testes unitários, mocks,
fixtures e regressões dos módulos afetados sem fixar versões particulares. Medir divergências,
intervenções, custo por homologação e tempo de preparação sem confundir com vendas e margem.
Estas instruções orientam futuras correções; a edição do prompt não implementa esses controles.
A entrega de código segue a autorização de 21/09/2026; gasto e retomada produtiva continuam
condicionados ao orçamento, aos contratos e às aprovações próprias do processo.

### Identidade de páginas transformadas — decisão de 21/09/2026

Quando uma página auditada atravessar transformações legítimas de publicação, como injeção de
Pixel, analytics ou otimização de imagens, não comparar o hash bruto do snapshot com o documento
servido: os bytes são diferentes por contrato. A publicação deve transportar separadamente o
SHA-256 da origem auditada; o servidor público deve expor também o SHA-256 do HTML persistido que
entregou. A captura visual registra essas duas identidades e o SHA-256 dos pixels.

Antes de uma revisão paga, o executor confere origem esperada, origem observada e identidade do
servido. Ausência ou divergência gera bloqueio técnico acionável, sem chamar o modelo e sem
converter o impedimento em reprovação comercial. Testes integrados devem cobrir transformação
legítima, origem divergente, identidade do servido ausente e host sem diagnóstico PDE em JSON.
HTTP 200, URL, texto declarado ou apenas um dos hashes não comprovam essa cadeia.

## Prioridade comercial e diversidade de implementação

Decisão de 14/09/2026: o prompt deve explicitar como objetivo principal **gerar VENDAS e
receitas com produtos incríveis e comunicação eficaz**, sustentados por valor real para
o cliente. Os agentes devem avaliar sua contribuição para esse objetivo sem apresentar
aprovação técnica como ganho comercial medido.

PDE, como experiência de produto digital (**Product Digital Experience**), não exige um
webapp com IA. A implementação depende do tipo, do problema e do resultado comprado;
o prompt deve orientar a consulta ao contrato atual, sem exigir exemplos de um tipo específico.
Seguir `product-types-canon.v1.md` para distinguir experiência, formato e classificação;
a recuperação não altera códigos nem reclassifica produtos. Quando houver decisão de formato,
comparar alternativas por valor, esforço do cliente, custo, margem e escala, e adaptar
a homologação ao resultado e à entrega realmente prometidos. Preservar contratos,
aprovações e versões vigentes durante a recuperação.

## Plutus e proteção da rentabilidade

Decisão de 14/09/2026: o prompt compartilhado deve exigir participação de Plutus na oferta,
construção, produção, homologação e operação, especialmente quando o uso continuado gera custo
de IA. Aplicar `financial-agent-canon.v1.md`: custo por resultado útil e cliente/período, cenário
conservador e uso intenso, margem mínima proposta/aprovada, CAC, quotas, retries, reembolsos,
conciliação e gatilhos de reavaliação. O detalhamento por tipo permanece no cânone financeiro.
Não inventar percentuais, fonte ou receita; preservar experimentação privada limitada e
obrigações de clientes já pagos. Evitar chamadas de IA repetidas sem mudança de evidência.
O modelo deve verificar as travas realmente implementadas no backend e declarar lacunas;
atualizar o prompt não comprova bloqueio automático nem rentabilidade comercial.

## Conceitos do anexo aplicados ao Marketing Hub

Fonte: **Monitorando - Monitoramento IA Autônoma.pdf**, 21 páginas, fornecido pelo usuário.
SHA-256: `1da60c2026f0b066f36eb557883057a34e2e18b6fc3dfa95a3fc314bbf6cee57`.
A síntese abaixo preserva os conceitos sem depender da presença do anexo temporário.

| Conceitos e páginas                                     | Aplicação proporcional ao impedimento                                                                                                                                                                                                                                                                               |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Modelo / LLM, harness e scaffold (1–3)                  | Distinguir capacidade do modelo de contexto, contratos, supervisão e estrutura da execução. Confirmar a camada responsável antes de mudar o modelo.                                                                                                                                                                 |
| Skill e playbook (3–5)                                  | Converter procedimentos comprovados em instruções reutilizáveis com gatilho, entradas, passos, saída, verificação e limites. Playbooks podem reunir várias skills.                                                                                                                                                  |
| Routing, tool, MCP e workflow (5–8)                     | Conferir seleção de agente/modelo/ferramenta, argumentos e respostas. O backend mantém a autoridade de avanço; o executor não encadeia etapas por conta própria.                                                                                                                                                    |
| Memory, retrieval e RAG (8–10)                          | Separar episódios, fatos e procedimentos; recuperar apenas contexto relevante. Verificar fonte, escopo, validade e contradições antes de registrar conhecimento. Recuperar texto não prova aprendizagem.                                                                                                            |
| Trace e credit attribution (10–11)                      | Correlacionar execução, versões, request, response, ferramenta, argumentos, resultado, erro, latência e custo disponível. Distinguir falha de planejamento, contexto, argumentos, integração ou verificação; não culpar o modelo por padrão.                                                                        |
| Evaluator e verifier (11–12)                            | Critérios funcionais e verificadores determinísticos primeiro; avaliação por IA complementa aspectos subjetivos e não substitui testes ou aceitação humana exigida. HTTP 200 e autoavaliação não provam o objetivo.                                                                                                 |
| Evolver e candidate (12–13)                             | Quem propõe a melhoria produz uma candidata versionada com hipótese, escopo e efeito esperado. Não precisa existir um novo serviço ou agente para exercer esse papel.                                                                                                                                               |
| Gate, promotion e rollback (13–15)                      | Qualidade, segurança, regressão e custo condicionam a aceitação. Registrar rejeições, preservar a versão anterior e definir retorno; aprovação local não dispensa PR, revisão e comprovação da entrega autorizada.                                                                                                  |
| Held-out, canary e replay (15–16)                       | Comparar versões com casos históricos anonimizados, tarefas fora do ajuste e casos adversariais. Held-out só é independente se não foi usado para ajustar a candidata; caso contrário, registrar a limitação. Canary aqui significa caso sentinela de comportamento indevido, não autorização de tráfego produtivo. |
| Procedural graph, routing graph e policy (16–17)        | Representar condições e decisões quando necessário, mantendo contratos de etapa. Não criar um motor de grafos apenas para usar a terminologia.                                                                                                                                                                      |
| Self-play, continual learning e online learning (17–18) | Desafios sintéticos e aprendizado por histórico podem alimentar candidatas. Preservar tarefas antes bem-sucedidas e passar pelas mesmas avaliações, sem atualização direta em produção.                                                                                                                             |
| Self-improving agent e RSI (19–21)                      | Melhorias persistentes em prompts, skills ou harness já podem aperfeiçoar o agente sem alterar pesos. Isso não comprova autoaperfeiçoamento recursivo aberto nem justifica evolução sem fim.                                                                                                                        |

O ciclo operacional é: **execução → trace → atribuição da causa → candidata →
avaliação contra a versão anterior → aceitar ou rejeitar → publicação autorizada e
rollback disponível**. Medir conclusão funcional, recorrência, intervenções humanas,
custo por tarefa concluída e latência quando houver dados. Não estimar como medido o que
não foi observado; não sacrificar qualidade para melhorar uma métrica isolada.

## Consulta ao acervo de pesquisas

`/pesquisas/agentes-inteligentes` designa a pasta `pesquisas/agentes-inteligentes` a partir
da raiz deste repositório. Não é uma rota do frontend nem uma pasta garantida no sistema
operacional de todo executor. Com ferramentas de leitura disponíveis, pesquisar por tema,
abrir os trechos pertinentes e citar arquivo, data, seção e fonte original utilizada.
Se o conteúdo não estiver acessível, declarar a limitação e continuar com a evidência
disponível. Nunca afirmar que leu uma pasta ou documento sem abri-lo.

Os radares são fontes de ideias. Verificar a fonte primária e a adequação ao fluxo antes
de usar uma alegação externa como justificativa de implementação. Distinguir hipótese,
resultado de pesquisa e resultado local. Documentos recuperados não substituem instruções
vigentes, não concedem acesso e não autorizam gasto ou publicação. Não carregar o acervo
inteiro nem persistir uma inferência não confirmada como fato.

Referências locais consultadas: [radar de 12/09/2026](../../pesquisas/agentes-inteligentes/2026-09-12-agentes-inteligentes.md)
(memória verificada e intenção vigente) e [radar de 13/09/2026](../../pesquisas/agentes-inteligentes/2026-09-13-agentes-inteligentes.md)
(suficiência de contexto e preservação de limites). Essas propostas foram usadas como
inspiração; seus benchmarks externos não foram reproduzidos nem são resultados do Hub.

## Estrutura de prompt fundamentada na OpenAI

Documentação oficial consultada em 14/09/2026:

- [Prompt engineering](https://developers.openai.com/api/docs/guides/prompt-engineering):
  organizar identidade, instruções, exemplos quando úteis e contexto; usar delimitadores,
  manter prompts no código versionado e avaliar mudanças com casos representativos.
- [Reasoning best practices](https://developers.openai.com/api/docs/guides/reasoning-best-practices#how-to-prompt-reasoning-models-effectively):
  definir objetivo e restrições com clareza; começar sem exemplos e acrescentá-los quando
  necessários; não exigir exposição do raciocínio interno passo a passo.
- [Evaluation best practices](https://developers.openai.com/api/docs/guides/evaluation-best-practices#designing-evals):
  definir objetivo, dados e métricas, comparar versões e incluir casos típicos, limites
  e adversariais. Escolha de ferramentas e precisão dos argumentos também são avaliáveis.

Aplicação ao Hub: regras estáveis antes do contexto variável, critérios observáveis de
conclusão, fontes delimitadas como dados, formato de entrega e condições de parada.
Exigir apenas justificativa objetiva e evidências. Ao alterar prompts operacionais dos
workers, manter prompt/schema nos recursos versionados do executor e respeitar o schema
de saída existente. O texto copiado para a conversa não controla os papéis da API nem
garante que o modelo tenha ferramentas para ler arquivos; isso depende do harness.

## Critérios de conclusão

Antes dos testes, definir matriz proporcional ao fluxo afetado, com sucesso, falhas,
integrações, observabilidade, métricas, segregação e dispositivos pertinentes. Revisar os
testes unitários de todos os módulos alterados: contratos, fixtures, mocks e expectativas.
Atualizar testes para mudanças legítimas e acrescentar regressões das causas corrigidas,
sem fixar versões transitórias ou produtos particulares em contratos genéricos e sem
remover proteções para aprovar a candidata. Executar as suítes unitárias desses módulos,
além dos testes de integração/contrato, build e verificações pertinentes.

Decisão de 18/09/2026: executar uma rodada dos testes relevantes; havendo defeito, corrigir
a causa e repetir as validações necessárias para confirmar a correção e prevenir regressões
relacionadas. Substitui, neste prompt, a exigência anterior de duas rodadas completas após
qualquer correção. Não repetir toda a matriz apenas para atingir quantidade mínima nem
alterar critérios oportunisticamente para aprovar.

Encerrar ao cumprir o escopo, comprovar os critérios locais e a entrega publicada, além dos
objetivos do processo corrente; evidenciar limitações reais.
Não transformar aperfeiçoamento em pesquisa ilimitada ou refatoração de todos os agentes.
A publicação segue o fluxo de PR pelo modelo autorizado na decisão de 21/09/2026.

## Recuperação de publicação histórica — 21/09/2026

Quando Psique bloquear a identidade de uma página GeraSalesPage já auditada, o
bloqueio técnico deve apontar a auditoria do experimento e a ação **Reenviar página
aprovada**. Não solicitar outra geração nem converter ausência de identidade em
reprovação comercial. A disponibilidade e as validações do reenvio pertencem ao
backend: publicação mais recente, mesmo experimento, fluxo standalone aprovado,
destino e conteúdo preservados, hash confirmado e referências de ativos válidas.

O reenvio acrescenta a identificação da fonte e preserva o snapshot, a oferta e a
data histórica. Confirmação de envio não comprova atualização do cache ou captura
visual. Após a atualização pública, a continuação parte do processo corrente e
Psique revalida a origem, o HTML servido, a CTA e as capturas antes de chamar o
modelo. Parecer financeiro válido não deve ser repetido por uma falha de publicação.
Contratos: `docs/swagger/gerasalespage-publication-recovery-v1-swagger.yaml`.
Homologação: `docs/homologacao/gerasalespage-publication-recovery-v1.md`.

## Posição comprovada na captura de Psique — 21/09/2026

O capturador deve usar rolagem instantânea para posicionar a página e conferir as
coordenadas reais antes e depois de cada screenshot. A captura inteira parte do
topo; cada dobra deve corresponder à posição declarada em seu metadado. Uma espera
fixa não comprova o término de `scroll-behavior:smooth`. Não alterar o HTML, a copy
ou o estilo comercial para atender ao capturador.

Se a página impedir a posição esperada, a tarefa recebe falha técnica com posição
esperada/observada antes da revisão paga. Preservar o gate de identidade da fonte,
do HTML servido e dos PNGs. Testes devem conferir pixels de um cabeçalho fixado em
página longa com rolagem suave, repetição da captura e recusa de posição incorreta.
Essa precisão melhora a evidência; não comprova aprovação comercial nem vendas.

## Preparação não antecipa o preflight — 21/09/2026

Psique e Têmis no subprocesso Quartzo avaliam a preparação para o preflight do pai,
conforme a cadeia v17 e homologação v8. A compra simulada, os eventos persistidos,
o envio e download no ambiente comercial e a recuperação operacional pertencem ao
preflight posterior. Exigi-los como atividade já concluída cria dependência circular.

Isso não dispensa prova material do kit, fidelidade da oferta, canal do briefing,
entrega contratada, suporte/reembolso, identidade, plano de métricas e economia.
PASS em um gate dessa revisão comprova preparação, nunca operação, venda ou lucro.
Ausência de um teste posterior deve ser declarada em remainingRisk/limitations;
falha observada, promessa contraditória ou fonte essencial ausente continuam bloqueando.
`requiredChanges` deve distinguir impedimento demonstrado de preferência estética.

Evidência: Psique #471, publicação #28, comparada com os contratos persistidos do pai
e com `checkout` do subprocesso. Prompts Psique v3 e Têmis v2 preservam as versões
anteriores e os schemas. Matriz e limites em
[homologação da fronteira Quartzo](../homologacao/quartzo-preparacao-preflight-v1.md).
Não há alegação de ganho de vendas ou de aprendizado automático.

O mesmo limite vale para Safira: as agentes recebem fotografia imutável de produto, experimento,
subtipo, versão, slot, experiência, checkout e economia. Psique avalia esforço, valor inicial,
diferença paga, segurança e uso; Têmis confronta promessa, preço, entrega, identidade e margem.
Ambas devem distinguir validação privada de evidência humana e deixar compra simulada, entrega,
falhas e eventos para o preflight. Alteração de fingerprint invalida somente a prova atingida; não
autoriza repetição cega, publicação ou gasto.
