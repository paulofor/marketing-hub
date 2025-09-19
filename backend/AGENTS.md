# AGENTS.md — Backend

- Todo o modelo de dados deve estar definido aqui no projeto **backend**.
 - O projeto **ai-worker** deve usar este modelo sem manter uma cópia própria.
- Em classes do Spring com mais de um construtor (por exemplo handlers que recebem `RestTemplateBuilder`), marque explicitamente o construtor usado para injeção com `@Autowired` e certifique-se de importar a anotação, evitando que o container selecione um construtor incorreto.
