# AGENTS.md — Backend

- Todo o modelo de dados deve estar definido aqui no projeto **backend**.


## Estrutura obrigatória de pacotes por módulo backend

Use o módulo `com.marketinghub.geralanding.wireframe` como referência de organização por método, mas aplique a padronização abaixo para novos módulos e para refatorações planejadas:

- **Raiz do módulo:** cada módulo funcional deve ficar em uma raiz própria `com.marketinghub.<modulo>` ou `com.marketinghub.<dominio>.<modulo>`, sem misturar responsabilidades de outros módulos.
- **Controller único:** cada módulo deve ter exatamente um pacote `.controller` e um único controller responsável por responder todos os endpoints públicos daquele módulo. Não criar múltiplos controllers para o mesmo módulo; agrupe os métodos HTTP no controller único mantendo clareza de rotas e documentação.
- **Service único:** cada módulo deve ter exatamente um pacote `.service` e um único service responsável por orquestrar as operações do módulo. Extrações internas só devem existir quando forem auxiliares sem expor contrato do módulo; não crie services paralelos para cada endpoint.
- **DTOs dentro de service:** os DTOs de entrada e saída devem ficar dentro de `.service`, organizados obrigatoriamente em subpacotes por método/operação, seguindo o padrão observado em `geralanding.wireframe.service.<metodo>` (ex.: `.service.recebePrompt`, `.service.recebeResposta`, `.service.listStageExecutions`). Evite DTOs soltos em pacote genérico quando eles pertencem a uma operação específica.
- **Nome do subpacote por método:** o subpacote do DTO deve representar o método/operação de negócio atendido pelo controller e pelo service, usando nome simples, estável e aderente ao contrato da API.
- **DTOs como `record`:** todo DTO de contrato de entrada ou saída deve ser declarado como `record`, preservando imutabilidade e deixando claro que ele é apenas estrutura de dados do contrato. Não crie DTOs como classes mutáveis com getters/setters para novos contratos ou refatorações planejadas.
- **Swagger/OpenAPI em `docs/swagger`:** todo módulo com endpoints deve gerar e manter documentação Swagger/OpenAPI exclusivamente em `docs/swagger`, com arquivo nomeado pelo módulo ou integração (ex.: `docs/swagger/<modulo>-swagger.yaml`). Ao criar ou alterar endpoints, atualize a documentação do módulo no mesmo PR e não crie contratos Swagger/OpenAPI em outras pastas.
- **Contratos antes de implementação:** antes de criar nova rota, verifique se o contrato já existe no controller único e na documentação Swagger do módulo; se não existir, defina o contrato no backend, documente em `docs/swagger` e adicione/ajuste testes.

## Estrutura obrigatória de repositories

> **DESTAQUE — regra obrigatória de acesso ao banco:** todas as classes que conectam diretamente com banco de dados devem ficar exclusivamente dentro do pacote `com.marketinghub.repository`. Não crie repositories, DAOs, gateways JDBC/JPA ou qualquer classe com acesso direto ao banco dentro dos pacotes funcionais dos módulos.

- **Base única:** use `com.marketinghub.repository` como raiz única para toda persistência do backend.
- **Subdivisão por tecnologia:** repositories Spring Data/JPA devem ficar em `com.marketinghub.repository.jpa`. Se outra tecnologia de persistência for necessária, crie uma subdivisão explícita dentro de `com.marketinghub.repository` (ex.: `jdbc`, `jooq`), mantendo a mesma regra de centralização.
- **Subdivisão por módulo:** dentro da tecnologia, organize por módulo/assunto seguindo a estrutura existente, por exemplo `com.marketinghub.repository.jpa.oprm`, `com.marketinghub.repository.jpa.mois`, `com.marketinghub.repository.jpa.experiment`, `com.marketinghub.repository.jpa.leadportal`.
- **Pacotes funcionais:** pacotes como `service`, `web`, `mapper`, `dto` e pacotes de módulos (`oprm`, `mois`, `mds`, etc.) devem consumir interfaces/classes do pacote `repository`, mas não devem conter classes que executem SQL, estendam `JpaRepository` ou acessem `EntityManager`, `JdbcTemplate`, `DataSource` ou conexões diretamente.
- **Ao criar ou mover persistência:** preserve a coerência com o módulo de domínio, atualize imports e testes, e não duplique repositories em pacotes locais do módulo.
- O projeto **ai-worker** e os demais modulos devem usar este modelo sem manter uma cópia própria.
- Em classes do Spring com mais de um construtor (por exemplo handlers que recebem `RestTemplateBuilder`), marque explicitamente o construtor usado para injeção com `@Autowired` e certifique-se de importar a anotação, evitando que o container selecione um construtor incorreto.
- Liquibase: nunca altere arquivos de changelog já aplicados para evitar erros de checksum. Em vez disso, crie um novo changelog incremental e, se necessário, atualize os `include` do master.
- Liquibase (MySQL 5): ao escrever `preConditions`, utilize apenas SQL válida no MySQL 5 (testando as consultas antes de incluí-las) e configure `dbms="mysql"` quando a condição depender do banco para evitar erros de sintaxe.
- Liquibase (MySQL 5 — erro 1093): em `UPDATE` ou `DELETE`, é proibido consultar a mesma tabela-alvo em subconsulta no `WHERE`/`SET` para validar existência, conflito ou idempotência. Esse padrão causa `You can't specify target table ... for update in FROM clause` no MySQL 5.7. Prefira anti-join explícito (`LEFT JOIN <mesma_tabela> alias ... WHERE alias.id IS NULL`), tabela derivada materializada em nível seguro ou comandos separados. Antes de commitar changelog, revise cada `UPDATE/DELETE` procurando subconsultas que referenciem a tabela-alvo.
- Liquibase (SQL formatado): mantenha os atributos (`expectedResult`, `dbms`, etc.) na mesma linha do `--precondition-sql-check` junto com a consulta SQL (`SELECT ...`) exatamente como suportado pelo Liquibase para evitar falhas de parsing.
- Liquibase (YAML): em `preConditions`, nunca misture mapeamento e lista no mesmo nível de indentação (ex.: `onFail:` seguido de `- dbms:`). Use sempre lista explícita (`- onFail: ...`, `- onError: ...`, `- dbms:`) para evitar `ParserException` de YAML.
- JPA/Hibernate x Liquibase: ao criar ou alterar entidade/campo persistido, alinhe explicitamente o nome físico da coluna com o changelog canônico usando `@Column(name = "...")` sempre que houver siglas/acrônimos, camel-case ambíguo ou booleanos (`OpenAI`, `URL`, `HTML`, `ID`, `requiresOpenAiModel`, etc.). Não permita que `spring.jpa.hibernate.ddl-auto=update` crie coluna paralela/legada diferente da coluna Liquibase; isso pode gerar `NOT NULL` sem default e parar o backend no bootstrap. Antes de fazer `INSERT` Liquibase em tabelas existentes, confira se não há coluna duplicada no schema real e, se houver, crie changelog incremental de reparo antes do seed.
- Os endpoints de contas do Facebook não podem descartar valores que não aparecem na UI (por exemplo, tokens de acesso ou IDs padrão). Ao atualizar um registro existente, preserve qualquer campo que não tenha sido enviado explicitamente na requisição.
- JDBC + MySQL: ao ler colunas `UUID`, nunca use `ResultSet#getObject` com `UUID.class`. Faça a conversão explícita de acordo com o tipo da coluna (`CHAR(36)`/`VARCHAR`: `UUID.fromString(rs.getString(...))`; `BINARY(16)`: converta o `byte[]` para `UUID` com utilitário dedicado ou `UUID.nameUUIDFromBytes`).
- Escopo de controllers por módulo: cada módulo só pode acessar os controllers do próprio módulo/pacote (ex.: MOIS -> controllers de MOIS; OPRM -> controllers de OPRM). É proibido consumir controllers de outro módulo diretamente.
- Depois de uma alteração execute os testes unitários antes da criação do PR.
- Tratamento de erros: em métodos com possibilidade de erros críticos e uso de `try/catch`, inclua `catch (RuntimeException ex)` e registre em log os parâmetros de entrada, a linha do erro, a classe do erro e a mensagem recebida; sempre que houver `jobId` disponível, ele também deve ser incluído no log.

