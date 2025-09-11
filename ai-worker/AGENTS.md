# AGENTS.md — AI Worker

- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.

## Serviços existentes
- **Nicho** (`niche`): gera hipóteses para nichos com `hypothesesToGenerate > 0` usando o ChatGPT. Implementado por `NicheHypothesisService` e agendado por `NicheHypothesisScheduler`.
- **Criativos** (`creative`): gera criativos para experimentos com `creativesToGenerate > 0` usando o ChatGPT. Implementado por `ExperimentCreativeService` e agendado por `ExperimentCreativeScheduler`.
- **Produto de Sucesso** (`successproduct`): gera nicho e hipótese a partir de produtos com `novo=false` usando o ChatGPT. Implementado por `SuccessProductNicheHypothesisService` e agendado por `SuccessProductNicheHypothesisScheduler`.

## Orientação para novos serviços
- Siga o mesmo padrão do serviço de **nicho**:
  - criar um pacote com o nome do domínio (ex: `niche`, `creative`);
  - implementar uma classe `*Service` com a lógica de geração;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do ChatGPT dentro do mesmo pacote.
