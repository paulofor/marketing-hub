# Canon de secrets e deploys

Versao: v1
Data: 2026-07-18

## Regra central

Todo servico do Marketing Hub que dependa de credencial externa, e-mail, IA, pagamento, anuncio, checkout, storage ou integracao comercial deve ter seus secrets inventariados antes de deploy.

O inventario operacional vigente fica em `docs/operations/secrets-inventory.md`.

## Obrigatorio

- Registrar nome da variavel, servico, host/deploy, condicao de obrigatoriedade, validacao pos-deploy e impacto comercial.
- Nunca registrar valor real do secret em documento, compose versionado, log, print ou resposta.
- Bloquear deploy quando variavel sensivel obrigatoria estiver ausente ou vazia.
- Validar o fluxo de negocio dependente do secret depois do deploy, nao apenas o health tecnico.
- Atualizar o inventario no mesmo trabalho quando um novo servico ou nova dependencia sensivel for criado.

## Proibido

- Usar valor real de credencial como default em novo arquivo versionado.
- Considerar container saudavel como validacao suficiente quando o servico depende de provedor externo.
- Corrigir falha de secret com paliativo que exponha token, magic link, chave ou credencial ao usuario final.

## Causa-raiz que originou a regra

O PDE Platform / Clube MUSA subiu tecnicamente ativo, mas sem envio real de magic link porque as credenciais AWS do Amazon SES estavam vazias no runtime. Isso interrompia a entrada do lead no produto e gerava perda direta de conversao.
