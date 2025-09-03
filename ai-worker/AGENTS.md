# AGENTS.md — AI Worker

- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.

## Serviços existentes
- **Nicho** (`niche`): gera hipóteses para nichos com `hypothesesToGenerate > 0` usando o ChatGPT. Implementado por `NicheHypothesisService` e agendado por `NicheHypothesisScheduler`.
  - **Entrada:** lê `market_niche` com `hypotheses_to_generate > 0`.
  - **Saída:** cria registros em `hypothesis` e atualiza `market_niche` (`hypotheses_to_generate = 0`).
- **Criativos** (`creative`): gera criativos para experimentos com `creativesToGenerate > 0` usando o ChatGPT. Implementado por `ExperimentCreativeService` e agendado por `ExperimentCreativeScheduler`.
  - **Entrada:** lê `experiment` com `creatives_to_generate > 0`.
  - **Saída:** cria registros em `creative` e atualiza `experiment` (`creatives_to_generate = 0`).

## Orientação para novos serviços
- Siga o mesmo padrão do serviço de **nicho**:
  - criar um pacote com o nome do domínio (ex: `niche`, `creative`);
  - implementar uma classe `*Service` com a lógica de geração;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do ChatGPT dentro do mesmo pacote.
