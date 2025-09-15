# AGENTS.md — Facebook Ads Worker

- 🚨 **Muito importante:** qualquer alteração neste módulo deve ser refletida em todos os arquivos `.md` deste diretório. Mantenha a documentação atualizada.
- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.
- Utilize o `facebook-ads-worker` para todas as chamadas à API do Facebook.
- Não mantenha segredos no repositório; use variáveis de ambiente ou GitHub Secrets.
- Endpoints do backend devem ser acessados com o prefixo configurado em `backend.api-prefix` (default `/api`).

## Serviços existentes
- **Campanhas de Facebook Ads** (`campaign`): cria campanhas para Facebook e Instagram utilizando o `facebook-ads-worker` com criativos gerados pelo **AI Worker** e aprovados pelo usuário no frontend.

## Orientação para novos serviços
- Siga o mesmo padrão do serviço de **campanhas de Facebook Ads**:
  - criar um pacote com o nome do domínio (ex: `campaign`);
  - implementar uma classe `*Service` com a lógica de integração com a API do Facebook;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do Facebook dentro do mesmo pacote.
