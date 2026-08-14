Leia o documento `/docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md`.

## Hotmart — regra operacional

- As rotinas automáticas deste módulo permanecem desativadas.
- A navegação autenticada é executada pelo coletor MOIS isolado, com conta dedicada e acesso somente de leitura; o Agente Radar recebe apenas evidências estruturadas.
- Nunca persistir ou versionar usuário, senha, JWT, cookies ou dados de sessão.
- Nunca registrar credenciais em logs, exemplos, testes ou documentação.
- O agente não pode escrever, afiliar, comprar, alterar produto, conta ou configuração na Hotmart.
- Se precisar criar endpoints no backend, crie-os no contexto coletor do MOIS.

## Regra obrigatória de logs em integrações OpenAI

- Registrar request cru e `jobId` no envio à OpenAI, sem segredos.
- Registrar response cru e `jobId` no retorno da OpenAI, sem segredos.
- Registrar payload e `jobId` no envio ao backend, sem segredos.
