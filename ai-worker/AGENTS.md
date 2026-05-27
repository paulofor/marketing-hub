# AGENTS.md — AI Worker

- Sempre que precisar trabalhar com OpenAI API, ChatGPT Apps SDK, Codex ou documentação oficial relacionada, use o servidor MCP de documentação da OpenAI (`https://developers.openai.com/mcp`) antes de assumir detalhes técnicos não confirmados.
- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- 🚨 **Proibido acesso direto ao banco de dados.** Toda leitura ou escrita deve ser feita **exclusivamente via endpoints do backend**, reutilizando os serviços existentes ou solicitando novos quando necessário.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.

## Serviços existentes
- **Nicho** (`niche`): gera hipóteses para nichos com `hypothesesToGenerate > 0` usando o ChatGPT. Implementado por `NicheHypothesisService` e agendado por `NicheHypothesisScheduler`.
- **Criativos** (`creative`): gera criativos para experimentos com `creativesToGenerate > 0` usando o ChatGPT. Implementado por `ExperimentCreativeService` e agendado por `ExperimentCreativeScheduler`.
- **Produto de Sucesso** (`successproduct`): gera nicho e hipótese a partir de produtos com `generate_niche_hypothesis=true` usando o ChatGPT. Implementado por `SuccessProductNicheHypothesisService` e agendado por `SuccessProductNicheHypothesisScheduler`.

## Orientação para novos serviços
- Siga o mesmo padrão do serviço de **nicho**:
  - criar um pacote com o nome do domínio (ex: `niche`, `creative`);
  - implementar uma classe `*Service` com a lógica de geração;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do ChatGPT dentro do mesmo pacote.

## Compilação do módulo
- O `pom.xml` deste módulo fica em `ai-worker/pom.xml` e **não** existe reactor Maven na raiz do repositório.
- Para compilar corretamente a partir da raiz do projeto, use:
  - `mvn -f ai-worker/pom.xml -DskipTests compile`
- Alternativamente:
  - `cd ai-worker && mvn -DskipTests compile`
- Se ocorrer erro de dependência `com.marketinghub:ads-service:0.0.1-SNAPSHOT`, publique/instale primeiro a dependência localmente:
  - `mvn -f backend/ads-service/pom.xml -DskipTests install`

## Regra obrigatória de logs em integrações OpenAI (semelhante ao Gera Landing)
- Sempre que o Worker AI executar uma requisição para a OpenAI, registrar log com:
  - envio para a OpenAI contendo **request cru** + **jobId do Marketing Hub**;
  - resposta da OpenAI contendo **resposta crua** + **jobId do Marketing Hub**;
  - envio para o backend contendo **payload enviado** + **jobId do Marketing Hub**.

## Regra de isolamento do GeraLanding por etapa (obrigatória)
- Todo código relacionado ao **GeraLanding** deve permanecer dentro do pacote específico da própria etapa/domínio (`geralanding.<etapa>`).
- Evite mover classes, utilitários, DTOs, mappers, validadores e clientes para pacote comum/compartilhado quando forem usados pela etapa.
- Se houver necessidade prática entre etapas, **prefira duplicar código** dentro de cada pacote de etapa ao invés de centralizar em pacote comum.
- Objetivo: preservar isolamento arquitetural por etapa e evitar acoplamento transversal.
