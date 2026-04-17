# Mechanism Discovery Service
## Especificação do módulo de descoberta de mecanismos baseados em evidência

## Objetivo

Definir um módulo independente responsável por transformar:

- dor real de mercado
- resultado desejado
- contexto do nicho
- evidência científica e técnica relevante

em:

- hipóteses de mecanismo
- componentes ativos do mecanismo
- limitações e riscos
- nível de confiança da evidência
- conhecimento simplificado e prático
- artefatos reutilizáveis para criação de produto

Este documento foi escrito para orientar implementação futura pelo **codex-gpt**, em etapas, quando as urgências operacionais do sistema atual estiverem estabilizadas.

---

## 1. Problema que o módulo resolve

O Marketing Hub já possui um pipeline que:

1. identifica nichos e dores de mercado
2. propõe resultados desejados
3. precisa definir um mecanismo que realmente ajude a pessoa a melhorar

O problema é que esse mecanismo não deve nascer apenas de:
- intuição
- senso comum
- copywriting
- analogia frouxa
- “prompts criativos”

O mecanismo precisa ser construído com base no **melhor conhecimento disponível**, mas traduzido para algo:
- simples
- aplicável
- ensinável
- útil para o cliente final
- coerente com o contexto real do nicho

Este módulo existe para preencher exatamente essa lacuna.

---

## 2. Nome e posição arquitetural

### Nome recomendado
**Mechanism Discovery Service**

### Alternativas aceitáveis
- Evidence-Based Mechanism Service
- Scientific Mechanism Engine
- Product Mechanism Discovery Service

### Posicionamento no ecossistema
Esse módulo deve ser um **serviço independente**, com:
- projeto próprio
- container próprio
- imagem própria
- API interna própria
- observabilidade própria

Ele **não** substitui o PromptResolver.
Ele **não** substitui o Worker AI.
Ele **não** substitui o backend de domínio.

### Relação com os demais módulos
- **antes do PromptResolver**: produz conhecimento e artefatos de base
- **antes do Worker AI**: estrutura o material que será consumido nos prompts
- **antes do pipeline de produto**: ajuda a formular o mecanismo real do produto

---

## 3. Missão principal do módulo

A missão do módulo é:

> descobrir, qualificar e traduzir evidência científica e técnica em mecanismos de produto práticos, plausíveis e rastreáveis.

Em outras palavras:

- não é só busca bibliográfica
- não é só sumarização de artigo
- não é só review acadêmico
- não é só RAG em papers

É uma camada de **tradução mecanística orientada a produto**.

---

## 4. Base conceitual que inspira o módulo

Este módulo deve se inspirar em algumas linhas fortes da prática baseada em evidência:

### 4.1 Evidence-based practice
Usar o melhor conhecimento disponível para apoiar decisões, sem tratar qualquer estudo isolado como verdade final.

### 4.2 Translational science
Transformar conhecimento científico em intervenção útil no mundo real.

### 4.3 NIH Stage Model
Pensar em desenvolvimento de intervenções em estágios, com atenção a mecanismo, refinamento, contexto real e escalabilidade.

### 4.4 PRISMA
Trazer transparência sobre:
- como fontes foram encontradas
- como foram filtradas
- por que entraram ou saíram

### 4.5 GRADE
Explicitar o grau de confiança da evidência.

### 4.6 Implementation science / CFIR
Olhar não só “funciona em tese”, mas:
- para quem
- em que contexto
- com quais barreiras
- com quais facilitadores
- com quanta chance de adoção real

### 4.7 Behaviour Change frameworks
Para problemas ligados a comportamento, adesão, engajamento, rotina, retenção e mudança prática, usar estruturas como:
- COM-B / Behaviour Change Wheel
- BCT Taxonomy

Esses referenciais não precisam ser implementados por completo na primeira versão, mas devem orientar o desenho do módulo.

---

## 5. O que o módulo deve fazer

## 5.1 Formular perguntas de mecanismo

A partir de um problema de mercado, o módulo deve conseguir transformar a dor em pergunta investigável.

### Exemplo
Dor:
“o aluno entra e some nas primeiras semanas”

Pergunta de mecanismo:
“quais componentes de onboarding, acompanhamento inicial e percepção de progresso têm melhor evidência para aumentar engajamento nas primeiras semanas em serviços recorrentes?”

### Saída esperada
Uma ou mais perguntas de mecanismo, estruturadas para busca e síntese.

---

## 5.2 Buscar evidência em fontes científicas e técnicas confiáveis

O módulo deve consultar fontes de alta qualidade e com API ou acesso estruturado quando possível.

### Fontes prioritárias
- PubMed / NCBI
- PMC
- Europe PMC
- Crossref
- OpenAlex

### Fontes secundárias
- repositórios de preprints, quando relevante
- diretrizes e handbooks confiáveis
- literatura técnica de implementação e mudança de comportamento

### Regra
A busca deve ser transparente e rastreável.
Não basta “o modelo lembrar”.

### Observação crítica sobre custo e acesso automatizado
As fontes de descoberta e metadados não devem ser tratadas como equivalentes a acesso livre ao texto completo.

#### Classificação operacional recomendada
- **descoberta/metadados gratuitos**: PubMed / NCBI E-utilities, Crossref, Europe PMC
- **texto completo aberto**: PMC Open Access Subset e conteúdos open access identificados no Europe PMC
- **metadado + abstract, mas full text restrito**: artigos encontrados em bases como PubMed/Crossref/OpenAlex sem rota aberta licenciada
- **freemium/pago conforme escala**: OpenAlex API pode operar com camada gratuita, mas possui limites e cobrança acima do uso gratuito

#### Regra de arquitetura
O módulo deve separar duas etapas:
1. **descoberta e normalização bibliográfica**
2. **acesso permitido ao conteúdo integral**

Ou seja:
- achar um artigo no PubMed **não significa** poder baixar o texto completo
- encontrar DOI e abstract **não significa** permissão para text mining do artigo inteiro
- o sistema só deve baixar full text automaticamente quando a licença e a rota de acesso permitirem isso claramente

#### Política de acesso automatizado
Para cada documento encontrado, o módulo deve classificar:
- `open_access`
- `metadata_only`
- `restricted`

E registrar também um `permissionState`, por exemplo:
- `can_download`
- `can_text_mine`
- `link_only`

#### Consequência prática
A primeira versão do módulo deve assumir como fluxo padrão:
- usar PubMed / Europe PMC / Crossref / OpenAlex para descoberta
- usar PMC Open Access Subset e outras rotas open access para texto completo quando disponível
- trabalhar apenas com metadados e abstract quando o texto integral estiver sob restrição
- não tentar contornar paywall por scraping ou rotas não licenciadas

---

## 5.3 Normalizar e deduplicar resultados

O módulo deve consolidar metadados como:
- DOI
- PMID / PMCID
- título
- autores
- periódico
- ano
- tipo de publicação
- URL canônica
- abstract, quando disponível

### Objetivo
Evitar duplicata, ruído e rastreamento inconsistente.

---

## 5.4 Fazer triagem de relevância

O módulo deve classificar resultados por:
- aderência à dor investigada
- aderência ao resultado desejado
- adequação ao nicho/contexto
- atualidade
- tipo de estudo
- plausibilidade de aplicação prática

### Resultado
Não devolver “tudo que achou”, e sim um conjunto priorizado de evidências candidatas.

---

## 5.5 Avaliar qualidade e força da evidência

O módulo deve atribuir um nível de confiança, pelo menos em escala simples, por exemplo:
- alta
- moderada
- baixa
- muito baixa

### Critérios que podem influenciar
- tipo de estudo
- consistência
- replicação
- tamanho/escopo
- proximidade do problema investigado
- contexto de aplicação

### Regra
A resposta do módulo deve deixar claro quando uma ideia é:
- bem sustentada
- promissora mas incerta
- fraca
- especulativa

---

## 5.6 Extrair componentes ativos do mecanismo

Esse é o coração do módulo.

Ele deve responder perguntas como:
- o que parece produzir a melhora?
- quais componentes aparecem recorrentemente?
- o que é central vs periférico?
- o que parece ser condição necessária?
- o que pode ser adaptação superficial?
- quais sequências ou combinações parecem mais eficazes?

### Exemplo de saída
- clareza do caminho
- vitórias rápidas no começo
- feedback frequente
- redução de fricção inicial
- acompanhamento com checkpoints
- autoeficácia e percepção de progresso

---

## 5.7 Traduzir evidência em mecanismo de produto

O módulo deve converter o conhecimento técnico em algo útil para design de produto.

### Deve produzir
- hipótese causal simplificada
- descrição do mecanismo
- componentes essenciais
- componentes opcionais
- restrições e limites
- risco de exagero comercial
- como isso pode ser ensinado ao cliente final
- formato prático sugerido

### Importante
O módulo não deve apenas “resumir artigo”.
Ele deve produzir **conhecimento operacionalizável**.

---

## 5.8 Publicar artefatos reutilizáveis

O módulo deve gerar artefatos reutilizáveis compatíveis com o ecossistema do Marketing Hub.

### Artefatos sugeridos
- `sourceDocument`
- `evidenceItem`
- `mechanismSpec`
- `evidencePack`
- `mechanismDiscoveryReport`
- `practicalKnowledgePack`

### Observação
Esses novos artefatos devem respeitar e expandir o modelo canônico já existente, não competir com ele.

---

## 5.9 Expor limitações e riscos

O módulo deve explicitar sempre:
- o que a evidência não permite afirmar
- onde há extrapolação
- onde o contexto do nicho é diferente do estudo
- quando existe risco de oversimplificação
- quando o mecanismo pode depender de implementação mais cuidadosa

---

## 5.10 Traduzir para linguagem prática

O objetivo final do Marketing Hub não é publicar revisão sistemática.
É gerar produtos eficazes e aplicáveis.

Então o módulo deve saber produzir, além do material técnico:
- versão técnica
- versão executiva
- versão prática para design de produto
- versão simplificada para consumidor final

---

## 6. O que o módulo NÃO deve fazer

Para evitar desvio de escopo, o módulo não deve:

- escrever copy final de anúncio
- escrever landing page final
- gerar promessas comerciais sozinho
- decidir posicionamento final de marketing
- chamar a OpenAI para gerar assets do pipeline
- publicar HTML no portal
- disparar tracking ou funil
- substituir revisão humana estratégica quando houver alto risco
- mascarar incerteza científica para parecer mais vendável
- transformar correlação fraca em mecanismo “comprovado”

### Regra simples
Esse módulo é dono da **descoberta e tradução do mecanismo**.
Ele não é dono da comunicação final nem da operação comercial.

---

## 7. Responsabilidades centrais resumidas

O módulo deve ser dono de:

1. formular perguntas de mecanismo
2. buscar evidência relevante
3. filtrar e deduplicar resultados
4. classificar qualidade e aplicabilidade
5. extrair componentes ativos
6. traduzir isso em mecanismo de produto
7. publicar artefatos estruturados
8. preservar rastreabilidade e confiança

---

## 8. Entradas do módulo

As entradas mínimas devem incluir algo como:

- nicho
- problema principal
- resultado desejado
- perfil do público
- contexto de aplicação
- restrições do produto
- tipo de entrega desejada
- idioma prioritário
- horizonte temporal da intervenção
- hipóteses já existentes, se houver

### Exemplo de entrada
```json
{
  "market": "Personal Trainers",
  "problem": "alunos somem nas primeiras semanas",
  "desiredOutcome": "maior engajamento inicial e percepção de valor",
  "deliveryConstraint": "conhecimento simples e aplicável",
  "context": "serviço recorrente com comunicação digital",
  "evidencePreference": "literatura moderna e aplicável"
}
```

---

## 9. Saídas do módulo

## 9.1 Saída técnica
- lista de fontes encontradas
- seleção final de evidências
- score/nível de confiança
- mecanismos candidatos
- componentes ativos
- limitações

## 9.2 Saída de produto
- mecanismo recomendado
- por que ele faz sentido
- em que condições tende a funcionar
- versão simplificada do mecanismo
- possíveis formatos de entrega prática

## 9.3 Saída de sistema
- artefatos publicados para reuso
- referências rastreáveis
- metadados de versão

---

## 10. Artefatos sugeridos

## 10.1 `mechanismDiscoveryRequest`
Representa a pergunta de descoberta.

## 10.2 `mechanismEvidenceSearch`
Registra estratégia de busca:
- fontes
- queries
- filtros
- janelas de tempo
- idioma

## 10.3 `sourceDocument`
Documento fonte estruturado.

## 10.4 `evidenceItem`
Unidade de evidência extraída.

## 10.5 `mechanismCandidate`
Hipótese mecanística candidata, com:
- descrição
- componentes
- limitações
- confiança

## 10.6 `mechanismSpec`
Mecanismo escolhido para uso no pipeline.

## 10.7 `practicalKnowledgePack`
Versão prática e simplificada do mecanismo para uso futuro em prompts e produtos.

---

## 11. Fluxo recomendado

### Etapa 1
Receber dor, resultado e contexto.

### Etapa 2
Transformar em pergunta de mecanismo.

### Etapa 3
Executar busca estruturada.

#### Observação da Etapa 3
A busca estruturada deve ser dividida em duas trilhas:
- **trilha de descoberta**: encontrar e normalizar estudos e metadados
- **trilha de acesso**: determinar se há permissão real para recuperar o full text

O sistema não deve assumir acesso automático ao texto completo só porque encontrou o estudo em uma base indexadora.

### Etapa 4
Triar, deduplicar e priorizar estudos.

### Etapa 5
Avaliar confiança e aplicabilidade.

### Etapa 6
Extrair componentes ativos e padrão causal.

### Etapa 7
Construir mecanismos candidatos.

### Etapa 8
Selecionar mecanismo recomendado com justificativa.

### Etapa 9
Traduzir para formato prático e reutilizável.

### Etapa 10
Publicar artefatos no ecossistema.

---

## 12. Interfaces internas recomendadas

## 12.1 `POST /internal/mechanism-discovery/search`
Executa busca inicial.

## 12.2 `POST /internal/mechanism-discovery/analyze`
Analisa corpus recuperado e extrai evidências.

## 12.3 `POST /internal/mechanism-discovery/build-mechanism`
Constrói mecanismos candidatos e propõe um mecanismo recomendado.

## 12.4 `POST /internal/mechanism-discovery/publish-pack`
Publica evidence packs e practical knowledge packs.

## 12.5 `GET /internal/mechanism-discovery/reports/{id}`
Recupera relatório de descoberta.

## 12.6 `GET /internal/mechanism-discovery/actuator/health`
Healthcheck.

---

## 13. Observabilidade

O módulo deve registrar:
- quais fontes foram consultadas
- quantos documentos retornaram
- quantos foram aceitos/rejeitados
- razões de exclusão
- tempo por etapa
- mecanismos candidatos gerados
- mecanismo final selecionado
- nível de confiança atribuído

### Campos mínimos em log
- `requestId`
- `market`
- `problem`
- `desiredOutcome`
- `searchSources`
- `selectedEvidenceCount`
- `mechanismCandidateCount`
- `chosenMechanismId`
- `confidenceLevel`

---

## 14. Guardrails importantes

### 14.1 Guardrail de evidência
Nunca apresentar hipótese especulativa como conclusão firme.

### 14.2 Guardrail de contexto
Nunca assumir que o que funciona em um contexto clínico ou acadêmico funciona igual em outro contexto comercial.

### 14.3 Guardrail de simplificação
Simplificar sem destruir o núcleo causal do mecanismo.

### 14.4 Guardrail comercial
Não transformar “promissor” em “comprovado” no texto que alimentará marketing.

### 14.5 Guardrail de atualização
Quando o tema for dinâmico, o módulo deve priorizar evidência moderna, mas sem descartar revisões clássicas relevantes.

---

## 15. Integração com o restante do ecossistema

## Entra antes de:
- criação do mecanismo do produto
- knowledge packs científicos
- prompts de produto baseados em ciência

## Alimenta:
- PromptResolver
- pipeline de design de produto
- pipeline de copy e oferta
- biblioteca de conhecimentos

## Não substitui:
- PromptResolver
- Worker AI
- backend do experimento
- camada de execução de marketing

---

## 16. Primeira versão recomendada

A primeira versão do módulo já deve conseguir:

1. receber dor + resultado + contexto
2. formular pergunta de mecanismo
3. buscar em fontes confiáveis
4. deduplicar e classificar
5. extrair evidências relevantes
6. propor mecanismos candidatos
7. selecionar um mecanismo recomendado
8. explicar limitações e confiança
9. publicar um `mechanismSpec`
10. publicar um `practicalKnowledgePack`

---

## 17. O que pode ficar para depois

Não precisa entrar no primeiro ciclo:
- ranking muito sofisticado por ML
- painel visual de revisão
- crowdsourcing de curadoria
- integração com dezenas de bases
- score epidemiológico detalhado
- revisão semi-automática por equipe humana
- benchmark competitivo automático amplo

---

## 18. Resumo executivo

O Mechanism Discovery Service não é um buscador de papers.

Ele é a camada responsável por pegar:
- dor real de mercado
- resultado desejado
- evidência científica moderna
- contexto de aplicação

e devolver:
- mecanismo de produto plausível
- baseado em evidência
- simplificado
- ensinável
- rastreável
- pronto para alimentar o restante do Marketing Hub

A missão dele é garantir que o “mecanismo” do produto não seja um chute bem escrito, mas uma síntese prática do melhor conhecimento disponível para aquele problema.
