# AGENTS.md — Facebook Ads Worker

- 🚨 **Muito importante:** qualquer alteração neste módulo deve ser refletida em todos os arquivos `.md` deste diretório. Mantenha a documentação atualizada.
- Este projeto utiliza o modelo de dados definido no **backend**.
- Não duplique ou mantenha modelo de dados aqui; importe-o do backend.
- Em produção utilizamos **MySql 5**.
- Tipos de dados permitidos (MySql 5): `INT`, `BIGINT`, `DECIMAL`, `DOUBLE`, `FLOAT`, `CHAR`, `VARCHAR`, `TEXT`, `LONGTEXT`, `BINARY(16)` para `UUID`, `DATE`, `DATETIME`, `TIMESTAMP`, `BOOLEAN`.
- Utilize o `facebook-ads-worker` para todas as chamadas à API do Facebook.
- Consulte sempre a documentação oficial da Graph API ao trabalhar neste módulo: https://developers.facebook.com/docs/graph-api e https://developers.facebook.com/docs/graph-api/reference.
- A versão da Graph API é configurável via propriedade `facebook.graph-api.version` (default `v23.0`) e deve estar alinhada com a recomendação oficial.
- Não mantenha segredos no repositório; use variáveis de ambiente ou GitHub Secrets.
- Endpoints do backend devem ser acessados com o prefixo configurado em `backend.api-prefix` (default `/api`).
- Sempre que chamar o backend registre logs com **URL completa**, parâmetros, payload enviado (quando existir) e a resposta recebida
  para facilitar troubleshooting.
- Prefixe os valores de URL nos logs de integração com endpoints usando `==>` para requisições e `<==` para respostas (incluindo
  erros), garantindo um padrão visual consistente em todo o módulo.
- Em caso de erro de permissão do Facebook, o worker bloqueia o experimento em memória até que o serviço seja reiniciado.
- Tokens podem ser invalidados pela Meta antes da data de expiração exibida no backend (por exemplo, troca de senha ou revogação
  manual do aplicativo); monitore os logs `(#190) OAuthException` e regenere o token quando necessário.
- Ao publicar instant forms aprove os rascunhos com `facebookFormId` nulo e reporte o identificador definitivo recebido da Meta
  através de `PATCH /api/instant-forms/{id}/publication`.

## Serviços existentes
- **Campanhas de Facebook Ads** (`campaign`): cria campanhas para Facebook e Instagram utilizando o `facebook-ads-worker` com criativos gerados pelo **AI Worker** e aprovados pelo usuário no frontend.

## Orientação para novos serviços
- Siga o mesmo padrão do serviço de **campanhas de Facebook Ads**:
  - criar um pacote com o nome do domínio (ex: `campaign`);
  - implementar uma classe `*Service` com a lógica de integração com a API do Facebook;
  - criar um `*Scheduler` com `@Scheduled` para executar o serviço periodicamente;
  - encapsular qualquer cliente do Facebook dentro do mesmo pacote.
