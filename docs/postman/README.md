# Postman Collections

Esta pasta concentra collections prontas para importar no Postman e exercitar os fluxos principais das integrações do Marketing Hub.

## Como usar
1. Abra o Postman e escolha **Import**.
2. Faça o upload do arquivo `facebook-ads-worker.postman_collection.json`.
3. Ajuste a variável `baseUrl` para apontar para a instância do backend que deseja testar (por padrão `http://191.252.92.222/api`).
4. Defina a variável `accountId` com o identificador da conta que deseja atualizar, renovar ou excluir.

A collection inclui requisições para listar, criar, atualizar e remover contas, consultar o `worker-config` ativo e registrar tentativas de renovação de token do Facebook Ads Worker.
