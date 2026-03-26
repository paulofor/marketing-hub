## Integração do AI Worker ao Framework de Vendas do Marketing Hub

### Princípio arquitetural
O **Marketing Hub** deve permanecer como **fonte de verdade** e camada de **gerenciamento do processo**.  
O **AI Worker** deve atuar como **camada de execução de jobs de IA**, consumindo tarefas geradas pelo Marketing Hub e devolvendo artefatos estruturados para revisão, persistência e uso operacional.

Em termos práticos:

- **Marketing Hub gerencia**
- **AI Worker executa**
- **Marketing Hub decide**
- **AI Worker propõe**

Essa separação mantém governança, rastreabilidade e controle do aprendizado dentro do produto principal.

---

## Papel de cada módulo

### Marketing Hub
Responsável por:

- cadastro e gestão de nichos
- mapeamento de dores
- criação e versionamento de hipóteses
- priorização de backlog
- criação de experimentos
- gestão de criativos, landing pages e ofertas
- abertura e acompanhamento de jobs de IA
- revisão humana das saídas
- aprovação, rejeição ou iteração
- persistência de histórico e aprendizado
- consolidação de métricas e decisão de negócio

### AI Worker
Responsável por:

- consumir jobs pendentes criados pelo Marketing Hub
- chamar os modelos da OpenAI via API
- gerar artefatos estruturados orientados pelo framework
- devolver saídas em formato previsível
- registrar falhas técnicas
- sinalizar jobs para revisão quando necessário

O AI Worker **não deve** ser responsável por decisões de negócio, publicação automática sem revisão ou gerenciamento da lógica comercial.

---

## Relação com o framework principal

O AI Worker deve operar sempre orientado pelo framework:

**Dor → Resultado → Mecanismo → Prova → Oferta**

O Marketing Hub fornece ao Worker o contexto de cada etapa.  
O Worker retorna saídas úteis para aumentar o sucesso das vendas ao longo da jornada:

**Anúncio Instagram → Página de coleta → Amostra personalizada → Venda**

---

## Tipos de jobs recomendados

### 1. Descoberta de dores
**Objetivo:** identificar dores de superfície, dores raiz, dores emocionais, dores sociais e ganhos desejados.

**Entrada do Marketing Hub:**
- nicho
- subnicho
- contexto do produto
- sinais coletados
- exemplos de comunicação existente

**Saída do AI Worker:**
- mapa de dores
- ranking de dores
- dores prioritárias
- ganhos desejados
- hipóteses preliminares

---

### 2. Hipótese de valor
**Objetivo:** converter o mapa de dores em hipóteses testáveis.

**Entrada do Marketing Hub:**
- nicho
- dor raiz priorizada
- objetivo de negócio
- contexto da solução

**Saída do AI Worker:**
- hipótese principal
- hipóteses alternativas
- promessa profunda
- resultado desejado
- mecanismo sugerido
- prova mínima recomendada

---

### 3. Geração de copy
**Objetivo:** produzir textos para anúncios, landing pages, amostras e venda.

**Entrada do Marketing Hub:**
- hipótese ativa
- estágio do funil
- variável testada
- regra do framework
- restrições de linguagem e posicionamento

**Saída do AI Worker:**
- headlines
- textos principais
- CTAs
- variações A/B
- racional da copy
- estrutura por dor, resultado, mecanismo, prova e ação

---

### 4. Planejamento de prova
**Objetivo:** definir como transformar a promessa em algo acreditável.

**Entrada do Marketing Hub:**
- hipótese ativa
- nicho
- oferta
- estágio da jornada

**Saída do AI Worker:**
- tipo de prova recomendado
- estrutura de amostra
- texto de entrega da amostra
- narrativa de antes/depois implícito
- orientação de posicionamento da prova

---

### 5. Amostra personalizada
**Objetivo:** gerar material personalizado para o lead com base nas informações coletadas.

**Entrada do Marketing Hub:**
- dados do lead
- nicho
- hipótese
- posicionamento desejado
- objetivo da amostra

**Saída do AI Worker:**
- briefing da peça
- copy da amostra
- texto de apresentação
- justificativa de personalização
- recomendação de uso comercial

---

### 6. Desenho de oferta
**Objetivo:** empacotar a solução em uma oferta mais vendável.

**Entrada do Marketing Hub:**
- hipótese validada ou promissora
- provas utilizadas
- sinais de interesse do mercado
- escopo real do produto

**Saída do AI Worker:**
- nome da oferta
- promessa de venda
- entregáveis
- estrutura da oferta
- objeções e respostas
- CTA final

---

### 7. Resumo de experimento
**Objetivo:** transformar dados e resultados em aprendizado operacional.

**Entrada do Marketing Hub:**
- experimento
- métricas
- variações
- resultados de funil
- feedbacks qualitativos

**Saída do AI Worker:**
- resumo executivo
- hipótese validada ou invalidada
- leitura do que funcionou
- leitura do que travou
- próxima recomendação de teste

---

## Modelo operacional de execução

### Fluxo recomendado
1. O Marketing Hub cria o job
2. O job recebe tipo, contexto, payload e schema esperado
3. O AI Worker consome o job pendente
4. O AI Worker chama a OpenAI
5. O AI Worker devolve resultado estruturado
6. O Marketing Hub persiste o artefato
7. O Marketing Hub exibe o resultado para revisão
8. Usuário aprova, rejeita ou itera
9. O resultado aprovado pode alimentar novos experimentos, assets ou ofertas

---

## Estados sugeridos para jobs

- `PENDING`
- `CLAIMED`
- `PROCESSING`
- `COMPLETED`
- `FAILED`
- `REVIEW_REQUIRED`
- `APPROVED`
- `REJECTED`

---

## Recomendação técnica para integração com OpenAI

### 1. Responses API
O AI Worker deve usar a **Responses API** como interface principal de geração, porque ela é a API recomendada para produzir respostas estruturadas e multimodais.

### 2. Structured Outputs
Sempre que possível, os jobs devem exigir **saídas estruturadas por schema**, para que o Marketing Hub receba objetos previsíveis, persistíveis e auditáveis, reduzindo parsing frágil e inconsistência entre execuções.

### 3. Function calling
Quando houver necessidade de acionar comportamentos internos com base na resposta do modelo, o fluxo pode usar **function calling**, mas o controle da orquestração deve continuar no Marketing Hub.

---

## Regra de contexto para o AI Worker

O AI Worker **não deve** receber prompts genéricos como:

- “gere uma copy”
- “faça uma oferta”
- “crie uma landing”

Ele deve sempre receber contexto completo do Marketing Hub, incluindo:

- nicho
- dor raiz
- resultado desejado
- mecanismo considerado
- prova esperada
- estágio do funil
- tipo de job
- variável que está sendo testada
- formato de saída esperado

Isso garante alinhamento com o framework e melhor qualidade das saídas.

---

## Como isso aumenta o sucesso nas vendas

Ao integrar o AI Worker dessa forma, o Marketing Hub passa a usar IA não apenas para “gerar textos”, mas para **operacionalizar um método de venda orientado por valor**.

Benefícios esperados:

- hipóteses mais claras
- copys mais orientadas a resultado
- provas mais convincentes
- ofertas mais alinhadas à dor do nicho
- menos esforço manual repetitivo
- mais velocidade para testar e iterar
- melhor rastreabilidade do aprendizado

---

## Regra final de produto

O Marketing Hub deve sempre manter esta hierarquia:

1. **definir a dor**
2. **definir o resultado**
3. **definir o mecanismo**
4. **definir a prova**
5. **definir a oferta**
6. **pedir ao AI Worker que gere artefatos dentro desse enquadramento**

Assim, o AI Worker não vira o centro do processo.  
Ele vira um acelerador de execução para um processo já bem definido pelo Marketing Hub.

---

## Resumo executivo

A integração ideal é:

- o **Marketing Hub** centraliza estratégia, contexto, gestão, persistência, revisão e decisão
- o **AI Worker** executa jobs de IA orientados por schema
- os modelos da OpenAI geram artefatos úteis para vendas dentro do framework
- o processo inteiro permanece governado pelo Marketing Hub

**Fórmula operacional final:**

**Marketing Hub gerencia → AI Worker gera → Marketing Hub revisa → Marketing Hub decide**

---

## Referências
- OpenAI — Responses API
- OpenAI — Structured Outputs
- OpenAI — Function Calling
