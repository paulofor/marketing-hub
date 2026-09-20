# Radar de IA Autônoma — 2026-09-20

## Rodada 17:58 — America/Sao_Paulo

### Resumo executivo

Nesta rodada, apenas **um desenvolvimento novo passou o filtro de relevância**: **OASIS**, publicado online em *Molecular Plant* em 18 de setembro de 2026 e indexado/publicamente exposto nos dias 19–20. O caso é relevante porque combina um sistema multiagente científico, atualização automática de conhecimento e um módulo explícito de **Self-Evolving Experience Learning (SEEL)** que destila trajetórias históricas em **regras procedurais reutilizáveis**. Não há evidência de atualização autônoma dos pesos do foundation model nem de recursive self-improvement forte.

Não encontrei nesta rodada um novo caso de produção no nível de LinkedIn/Tencent/Warp em que o sistema feche autonomamente, em tráfego real, o ciclo completo `experiência → variante → avaliação → promoção → persistência`, nem um novo caso convincente de continual/online learning de pesos em produção.

## Classificação

| Caso | (1) Pesos persistentes | (2) Código/scaffold/harness | (3) Prompts/retrieval/workflows/tools/skills/estratégia | (4) Só memória/contexto | (5) Otimização humana |
|---|---:|---:|---:|---:|---:|
| **OASIS / SEEL** | Não demonstrado | Não demonstrado como autoedição de código | **Sim — regras procedurais + atualização do conhecimento** | Parcial, mas vai além de simples memória | **Sim — arquitetura, domínio e validação definidos por humanos** |

## 1. OASIS — experiência científica convertida em regras procedurais reutilizáveis

OASIS (*OASIS, a self-evolving AI scientist that integrates omics data and literature knowledge for plant stress research*) coordena seis agentes especializados em **planejamento, hybrid retrieval, evidence distillation, data interrogation, review e synthesis**. Sua base de conhecimento cobre seis espécies vegetais e quatro grandes categorias de estresse e incorpora automaticamente estudos recém-publicados.

A peça que o torna relevante para este radar é o **Self-Evolving Experience Learning (SEEL)**. Segundo o trabalho, o SEEL **destila trajetórias históricas em regras procedurais reutilizáveis**, fazendo com que experiência anterior possa alterar a forma de trabalhar em tarefas posteriores. Isso é mais forte do que simplesmente guardar conversas ou fatos em memória: a experiência é transformada em orientação operacional reaplicável.

### O que o sistema melhorou sozinho

- Reutilização de experiência de trajetórias anteriores por meio de regras procedurais.
- Atualização contínua da base de conhecimento com literatura nova.
- Combinação mais sistemática de evidência bibliográfica e dados ômicos/genéticos em tarefas posteriores.

### O que persiste entre execuções

A evidência pública mostra persistência de dois tipos:

1. **Conhecimento atualizado** incorporado à base do sistema.
2. **Experiência procedural destilada pelo SEEL**, reutilizável em execuções futuras.

Não há demonstração pública de que o OASIS reescreva autonomamente o código do harness, altere os pesos do modelo ou promova novas versões do próprio Evolver.

### Intervenção humana

A intervenção continua substancial. Humanos definem o domínio científico, arquitetura multiagente, fontes de dados, benchmark e critérios de validação. O sistema automatiza a incorporação de conhecimento e a destilação/reutilização de experiência dentro dessa estrutura.

### Métricas e evidência

No **PlantStressQA**, benchmark de 200 perguntas curadas por especialistas, OASIS obteve **84,0/100**, superando os LLMs baseline reportados em **33,9 a 50,2 pontos**. Os maiores ganhos ocorreram nas questões que exigiam integração em múltiplas etapas entre literatura e evidência de dados.

O caso mais interessante vai além do benchmark textual. Em um estudo de tolerância ao sal em arroz, o sistema integrou evidências regulatórias de *Arabidopsis*, ortologia e loci genéticos para priorizar cinco candidatos. A perda de função de **OsPP2a** produziu fenótipos de resposta ao sal acompanhados por alteração da homeostase Na+/K+, fornecendo validação experimental de uma hipótese priorizada pelo sistema.

### Por que importa

O avanço principal não é simplesmente “mais um agente científico”. Ele mostra uma arquitetura em que:

`trajetórias históricas → destilação procedural → regras reutilizáveis → nova execução`

Isso se aproxima diretamente da categoria de **evolução persistente de skills/estratégias**, mesmo com pesos congelados. Também reforça a ideia de separar duas coisas que frequentemente são confundidas:

- `memória`: guardar fatos/episódios;
- `aprendizado procedural`: extrair uma regra que muda o modo de agir em tarefas futuras.

### Padrão arquitetural reutilizável

Um padrão adaptável a agentes próprios seria:

```text
Execution Trace
      ↓
Outcome / Evidence
      ↓
Experience Distiller
      ↓
Candidate Procedural Rule
      ↓
Validation / Contradiction Check
      ↓
Procedural Registry
      ↓
Retrieval na próxima execução
```

Para um harness com skills, isso pode ser implementado sem rebuild da imagem:

```text
trace
  ↓
regra procedural candidata
  ↓
CANDIDATE no banco
  ↓
replay + regression gate
  ↓
ACTIVE
  ↓
próximas execuções recuperam a regra
```

Isso combina bem com uma arquitetura de **estado evolutivo externo ao container**: o runtime fica estático na imagem, enquanto regras, skills e experiência aprovada ficam em registry/banco versionado.

### Limitações

- O trabalho não demonstra alteração autônoma dos pesos do foundation model.
- Não demonstra autoedição persistente do código/harness.
- Não demonstra RSI forte: o mecanismo que produz regras melhores não é mostrado ficando progressivamente melhor em produzir a própria próxima versão.
- O score agregado de 84/100 mede o sistema completo; a evidência pública acessível nesta rodada não fornece uma ablação isolando quantitativamente **quanto do ganho vem especificamente do SEEL**.
- A validação experimental do caso OsPP2a é forte como evidência científica, mas não prova que o ciclo de autoevolução seja aberto, indefinido ou totalmente autônomo.

## Leitura arquitetural da rodada

A distinção mais útil desta rodada é:

```text
MEMÓRIA
"isso aconteceu antes"

vs.

EXPERIÊNCIA PROCEDURAL
"quando uma situação desse tipo acontecer novamente, trabalhe desta forma"
```

OASIS cruza parcialmente essa fronteira ao transformar trajetórias em regras procedurais reutilizáveis. Para agentes próprios, isso sugere um passo intermediário entre `trace` e `skill`: **procedural rule candidates**. Regras bem-sucedidas podem permanecer dinâmicas no registry; só regras muito estáveis e recorrentes precisam ser posteriormente consolidadas em código, tool ou skill mais rígida.

## Situação das cinco categorias nesta rodada

1. **Mudança persistente de pesos:** nenhum avanço novo relevante encontrado nesta varredura.
2. **Mudança persistente de código/scaffold/harness:** nenhum novo caso convincente encontrado hoje.
3. **Evolução persistente de prompts/retrieval/workflows/tools/skills/estratégias:** **OASIS/SEEL entra aqui**, pela destilação de experiência em regras procedurais reutilizáveis e atualização contínua do conhecimento.
4. **Mera memória/contexto:** OASIS possui memória/base de conhecimento, mas o SEEL vai além ao proceduralizar experiência; portanto não o classifico apenas como categoria 4.
5. **Fine-tuning/otimização essencialmente humana:** a arquitetura e o processo permanecem fortemente delimitados por humanos, embora a aquisição e reutilização de experiência sejam automatizadas.

## Fontes públicas

- Han, Y. et al. **OASIS, a self-evolving AI scientist that integrates omics data and literature knowledge for plant stress research.** *Molecular Plant*, online ahead of print em 18/09/2026. DOI: https://doi.org/10.1016/j.molp.2026.09.005
- Registro bibliográfico / abstract derivado do PubMed (PMID 42760786): https://www.lifescience.net/publications/2212812/oasis-a-self-evolving-ai-scientist-that-integrates/
- Plataforma oficial OASIS — Chinese Academy of Agricultural Sciences / Agricultural Genomics Institute at Shenzhen: https://www.oasis.ac.cn/
