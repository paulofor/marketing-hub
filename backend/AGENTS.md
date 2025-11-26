# AGENTS.md — Backend

- Todo o modelo de dados deve estar definido aqui no projeto **backend**.
- O projeto **ai-worker** deve usar este modelo sem manter uma cópia própria.
- Em classes do Spring com mais de um construtor (por exemplo handlers que recebem `RestTemplateBuilder`), marque explicitamente o construtor usado para injeção com `@Autowired` e certifique-se de importar a anotação, evitando que o container selecione um construtor incorreto.
- Liquibase: nunca altere arquivos de changelog já aplicados para evitar erros de checksum. Em vez disso, crie um novo changelog incremental e, se necessário, atualize os `include` do master.
- Liquibase (MySQL 5): ao escrever `preConditions`, utilize apenas SQL válida no MySQL 5 (testando as consultas antes de incluí-las) e configure `dbms="mysql"` quando a condição depender do banco para evitar erros de sintaxe.
- Liquibase (SQL formatado): mantenha os atributos (`expectedResult`, `dbms`, etc.) na mesma linha do `--precondition-sql-check` junto com a consulta SQL (`SELECT ...`) exatamente como suportado pelo Liquibase para evitar falhas de parsing.
- Os endpoints de contas do Facebook não podem descartar valores que não aparecem na UI (por exemplo, tokens de acesso ou IDs padrão). Ao atualizar um registro existente, preserve qualquer campo que não tenha sido enviado explicitamente na requisição.
- JDBC + MySQL: ao ler colunas `UUID`, nunca use `ResultSet#getObject` com `UUID.class`. Faça a conversão explícita de acordo com o tipo da coluna (`CHAR(36)`/`VARCHAR`: `UUID.fromString(rs.getString(...))`; `BINARY(16)`: converta o `byte[]` para `UUID` com utilitário dedicado ou `UUID.nameUUIDFromBytes`).
