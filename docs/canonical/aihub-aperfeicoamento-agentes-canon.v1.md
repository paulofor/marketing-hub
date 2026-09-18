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

O pedido autoriza as correções locais causalmente relacionadas, inclusive nos workers
envolvidos. Melhorias adicionais ficam como sugestões fundamentadas. Uma candidata
validada localmente só pode ser publicada pelo fluxo de PR executado pelo usuário, com
imagem produzida pelos arquivos versionados do repositório. Não publicar por SSH nem
usar publicação como teste. Esta regra substitui a exceção antiga no prompt de ajuda.

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
Estas instruções orientam futuras correções; a edição do prompt não implementa esses controles
nem autoriza publicação, gasto ou retomada produtiva sem as condições do processo.

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
| Gate, promotion e rollback (13–15)                      | Qualidade, segurança, regressão e custo condicionam a aceitação. Registrar rejeições, preservar a versão anterior e definir retorno; aprovação local não autoriza publicação.                                                                                                                                       |
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

Encerrar ao cumprir o escopo e comprovar os critérios locais; evidenciar limitações reais.
Não transformar aperfeiçoamento em pesquisa ilimitada ou refatoração de todos os agentes.
A publicação segue o usuário e o fluxo do repositório, sem PR automático.
