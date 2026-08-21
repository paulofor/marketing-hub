# Skills ligadas a entidades e pontos cegos de avaliadores — 2026-08-21

## Decisão executiva

Aplicar imediatamente somente a recuperação de memória ligada à entidade, em piloto no Operador de
Crescimento. No Marketing Hub, a primeira entidade escolhida é a ferramenta MCP: ao consultar
cockpit, funil, sessões ou campanhas, Hermes recebe até três cuidados operacionais pertinentes
àquela ferramenta no mesmo momento da leitura.

Não iniciar agora mutação sintética autônoma do repositório nem permitir que modelos instalem código
de avaliação. Os dois trabalhos são preprints experimentais recentes e o EvalCEGAR foi medido em
problemas Python com testes ocultos exatos, não em decisões abertas de marketing.

## Evidência primária verificada

### SkillForge

O [artigo SkillForge](https://arxiv.org/abs/2608.18933) foi submetido ao arXiv em 19 de agosto de
2026. Ele cria tarefas sintéticas a partir de funcionalidades cobertas por testes, aprende com
trajetórias bem e malsucedidas e organiza o resultado em dois níveis: orientação diagnóstica global
e intervenção local ligada a entidades do repositório. A orientação local é recuperada just-in-time
quando o agente acessa a entidade correspondente.

Nos experimentos relatados pelos autores, o Mini-SWE-Agent melhorou 5,8 pontos percentuais com
DeepSeek-V3.2 e 5,6 pontos com GPT-5-mini no SWE-bench Verified; no SWE-bench Pro, os ganhos foram
5,8 e 4,1 pontos. São resultados de engenharia de software e não comprovam aumento de vendas.

### EvalCEGAR

O [artigo Metrics That Write Themselves](https://arxiv.org/abs/2608.18744) também foi submetido em
19 de agosto de 2026. O mecanismo encontra colisões em que o avaliador trata igualmente uma saída
correta e outra incorreta, usa o contraexemplo para propor um operador e só admite código que
melhora a decisão final no conjunto de treino.

O melhor operador relatado tem 55 linhas e fechou 15,4% da distância disponível até o filtro
perfeito em 428 tarefas não vistas. Seis de oito execuções admitiram um operador, e os seis ajudaram
fora da amostra. O próprio estudo mostra limites importantes: muita duplicação comportamental,
necessidade de rótulos exatos e operadores específicos do domínio.

## Estado já existente no Marketing Hub

O sistema já possui memória premium append-only, estados `CANDIDATE`, `CONFIRMED`, `CONTRADICTED` e
`RETIRED`, deduplicação, procedência, confiança, replay, holdout, promoção externa e rollback. Apolo
já possui skill candidata governada. Portanto, criar outra base de memória ou um segundo fluxo de
promoção aumentaria complexidade sem resolver a lacuna real.

A lacuna encontrada estava na recuperação: a memória era pedida pelo escopo amplo de planejamento
e o executor BPM por experimento nem expunha as ferramentas de memória. O conhecimento não chegava
automaticamente quando Hermes acessava exatamente a ferramenta afetada.

## Alternativas comparadas

| Alternativa | Benefício | Risco | Custo/esforço | Aderência a vendas |
|---|---|---|---|---|
| Reproduzir SkillForge completo no repositório | Pode antecipar falhas de código ainda não observadas | Gera muitas trajetórias, custo de modelo e risco de falsa confiança em testes incompletos | Alto | Indireta |
| Permitir avaliadores escritos automaticamente | Pode descobrir verificações determinísticas baratas | Marketing não possui oráculo exato amplo; código gerado pode premiar proxy errado | Alto | Indireta e ainda experimental |
| Recuperar memória por ferramenta no Operador | Evita redescobrir armadilhas no funil/cockpit durante operação de venda | Memória ruim pode enviesar análise se for confundida com prova atual | Baixo | Direta no fluxo de otimização |

## Piloto implementado

- Memória especializada disponível em tarefas por plano e por experimento.
- Registro opcional no escopo `MCP_TOOL/<ferramenta>`.
- Recuperação automática de até três memórias ao usar ferramentas de consulta.
- Payload oficial preservado em bloco separado.
- `CANDIDATE` explicitamente tratado como hipótese e conteúdo interno como não confiável.
- Falha de memória não bloqueia cockpit, funil, sessões ou campanhas.
- Escopo limitado por allowlist; o modelo não inventa nomes de ferramenta.
- Teste de contrato cobre injeção, segregação, indisponibilidade e bloqueio de escopo arbitrário.

## Matriz de homologação local

| Dimensão | Cenário e critério |
|---|---|
| Caminho feliz | consulta devolve payload oficial intacto e bloco separado com memória da ferramenta |
| Validações | ferramenta fora da allowlist é rejeitada antes de persistir candidato |
| Falhas | backend de memória indisponível não bloqueia a consulta comercial |
| Integração | fluxo JSON-RPC MCP conversa com backend HTTP simulado e preserva contrato |
| Observabilidade | sucesso ou indisponibilidade da memória gera auditoria sem expor conteúdo sensível |
| Métricas | limite de três itens e contagem persistida pelo backend evitam contexto ilimitado |
| Segregação | plano, experimento, execução e ferramenta recebem chaves distintas |
| Compatibilidade | modo legado por plano continua disponível e tarefas BPM ganham memória sem mutação |
| Navegadores/dispositivos | não aplicável: a mudança não possui interface visual nem altera artefato de cliente |

## Métricas do piloto

Medir por 20 tarefas elegíveis do Operador:

1. reincidência do mesmo erro operacional por ferramenta;
2. tarefas concluídas sem correção ou reexecução;
3. tokens e custo por diagnóstico aceito;
4. taxa de memórias posteriormente confirmadas, contraditas e realmente reutilizadas;
5. tempo do gargalo detectado até uma ação validada por evento comercial.

**Continuar:** queda de retrabalho sem aumento de decisão incorreta ou custo por diagnóstico.

**Ajustar:** memória é recuperada, mas não usada, cria ruído ou aumenta tokens sem melhorar aceite.

**Parar:** contaminação entre experimentos, candidato tratado como fato, decisão comercial pior ou
qualquer ampliação de autoridade.

## Próxima etapa condicionada a evidência

Registrar colisões reais dos avaliadores atuais: dois resultados com o mesmo score/decisão, mas com
vereditos independentes opostos. Somente após formar amostra rotulada e holdout deve-se testar um
detector determinístico candidato em sandbox. A geração automática do código continua fora do
runtime e sujeita a revisão humana e Pull Request.
