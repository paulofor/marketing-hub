# AGENTS.md — AI Worker

- Sempre que precisar trabalhar com OpenAI API, ChatGPT Apps SDK, Codex ou documentação oficial relacionada, use o servidor MCP de documentação da OpenAI (`https://developers.openai.com/mcp`) antes de assumir detalhes técnicos não confirmados.
- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- 🚨 **Proibido acesso direto ao banco de dados.** Toda leitura ou escrita deve ser feita **exclusivamente via endpoints do backend**, reutilizando os serviços existentes ou solicitando novos quando necessário.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.


## Orientação para novos serviços
- Siga o mesmo padrão do serviço de **nicho**:
  - criar um pacote com o nome do domínio (ex: `niche`, `creative`);
  - implementar uma classe `*Service` com a lógica de geração;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do ChatGPT dentro do mesmo pacote.


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
