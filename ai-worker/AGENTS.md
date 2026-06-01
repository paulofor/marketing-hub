# AGENTS.md — AI Worker

- Sempre que precisar trabalhar com OpenAI API, ChatGPT Apps SDK, Codex ou documentação oficial relacionada, use o servidor MCP de documentação da OpenAI (`https://developers.openai.com/mcp`) antes de assumir detalhes técnicos não confirmados.
- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- 🚨 **Proibido acesso direto ao banco de dados.** Toda leitura ou escrita deve ser feita **exclusivamente via endpoints do backend**, reutilizando os serviços existentes ou solicitando novos quando necessário.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.


## Orientação para novos serviços
- Para serviços de domínio que não fazem parte do núcleo OpenAI por etapa, siga o mesmo padrão do serviço de **nicho**:
  - criar um pacote com o nome do domínio (ex: `niche`, `creative`);
  - implementar uma classe `*Service` com a lógica de geração;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do ChatGPT dentro do mesmo pacote.
- Para etapas que chamam OpenAI de forma assíncrona por fila/callback, use o núcleo `com.marketinghub.worker.openai.core` como arquitetura primária.


## Regra obrigatória de logs em integrações OpenAI
- Sempre que o Worker AI executar uma requisição para a OpenAI, registrar log com:
  - envio para a OpenAI contendo **request cru** + **jobId do Marketing Hub**;
  - resposta da OpenAI contendo **resposta crua** + **jobId do Marketing Hub**;
  - envio para o backend contendo **payload enviado** + **jobId do Marketing Hub**.

## Regra de isolamento por etapa no OpenAI core (obrigatória)
- Toda etapa assíncrona baseada em OpenAI deve ficar no pacote específico da própria etapa dentro de `com.marketinghub.worker.openai.core.<etapa>`.
- O core genérico (`openai.core`, `openai.core.model`, `openai.core.port`, `openai.core.prompt`, `openai.core.exception` e clientes compartilhados) não deve depender de etapas concretas.
- Cada etapa concreta deve declarar explicitamente sua configuração, propriedades, scheduler, adapter de backend, prompt builder, validador e handler; evite `@Component`/`@Service` soltos fora da configuração da etapa.
- Evite mover DTOs, mappers, validadores, prompts builders e clients específicos para pacote comum/compartilhado quando forem usados por apenas uma etapa.
- Se houver necessidade prática entre etapas, **prefira duplicar código** dentro de cada pacote de etapa ao invés de centralizar prematuramente em pacote comum.
- Objetivo: preservar isolamento arquitetural por etapa, evitar acoplamento transversal e impedir recriação de namespaces legados de landing no Worker AI.
