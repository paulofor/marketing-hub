# PDE Harness SDK Java

Biblioteca Java 21 que transforma o protocolo local do Codex App Server em um contrato de execução
segregado, auditável e orientado a Produtos Digitais Experienciais.

## Fronteira

- inicia `codex app-server --listen stdio://` como processo local;
- usa JSONL/JSON-RPC, sessão ChatGPT gerenciada pelo Codex e versão fixada do protocolo;
- cria ou retoma thread, inicia turno, valida a saída final contra o JSON Schema e entrega eventos e
  resultado tipados com modelo, versões e hashes;
- remove chaves da OpenAI do ambiente filho e não possui fallback para API;
- valida workspace, schema estruturado, versão do Codex e integridade do bundle oficial;
- não acessa banco, não faz polling, não decide próxima etapa, não publica e não gasta.

O worker de cada PDE continua responsável por consumir o endpoint `pending`, carregar prompts e
schemas versionados, chamar esta biblioteca e reportar o resultado ao backend.

## Validação local

```bash
mvn spotless:check test
mvn -Pcodex-app-server-it verify
```

O segundo comando executa somente o handshake real `initialize`/`initialized`; não abre turno de
modelo e não gera conteúdo. O contrato fixado é o Codex CLI `0.149.0`.

## Atualização do protocolo

Antes de atualizar o Codex, gere o bundle com a nova versão, substitua o recurso versionado, atualize
o SHA-256 do manifesto e execute novamente todos os testes de contrato e o handshake real. Uma
incompatibilidade deve bloquear o SDK; nunca autoriza chamada direta à OpenAI API.
