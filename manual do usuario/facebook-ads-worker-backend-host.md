# Como configurar o host do Backend para o Facebook Ads Worker

O **Facebook Ads Worker** utiliza o backend para buscar campanhas e criativos que precisam ser enviados ao Facebook.
Por padrão, o Worker espera encontrar o backend em `http://localhost:8080`. Quando o backend está em outro servidor ou porta, o Worker
não consegue se conectar e erros como `404 Not Found` podem ocorrer.

Siga uma das opções abaixo para informar o endereço correto do backend:

## Opção 1: editar o arquivo de configuração

1. Abra o arquivo `facebook-ads-worker/src/main/resources/application.properties`.
2. Altere a propriedade `backend.base-url` para o endereço do seu backend. Exemplo:
   ```properties
   backend.base-url=https://api.exemplo.com:8080
   ```
3. Salve o arquivo e reinicie o Worker.

## Opção 2: usar variável de ambiente

1. Defina a variável de ambiente `BACKEND_BASE_URL` antes de iniciar o Worker:
   - **Linux/macOS**:
     ```bash
     export BACKEND_BASE_URL=https://api.exemplo.com:8080
     ```
   - **Windows PowerShell**:
     ```powershell
     $env:BACKEND_BASE_URL="https://api.exemplo.com:8080"
     ```
2. Inicie o Worker normalmente (`mvn spring-boot:run` ou `java -jar`).

> Certifique-se de incluir o protocolo (`http://` ou `https://`) e, se necessário, a porta. O Worker adicionará automaticamente o
> prefixo `/api` ao realizar as chamadas.

Para saber mais sobre como o Spring Boot carrega propriedades e variáveis de ambiente, consulte a
<a href="https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config" target="_blank">documentação oficial</a>.
