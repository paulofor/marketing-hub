# Acesso ao VPS pagamentopalf.site

Registro operacional para acesso ao VPS usado na publicação das páginas públicas do `pagamentopalf.site`.

## Forma correta de acesso

Use SSH com chave pública e force a identidade correta:

```bash
ssh -i ~/.ssh/codex_exp54_pagamentopalf -o IdentitiesOnly=yes root@pagamentopalf.site
```

Para copiar arquivos para o VPS:

```bash
scp -i ~/.ssh/codex_exp54_pagamentopalf -o IdentitiesOnly=yes <arquivo-local> root@pagamentopalf.site:<destino>
```

## Observações

- O host público usado é `pagamentopalf.site`.
- O usuário operacional validado é `root`.
- O acesso correto é por chave pública, não por senha.
- Ao trocar de ambiente Codex, confirme que a chave privada local corresponde à chave pública cadastrada em `/root/.ssh/authorized_keys` no VPS.

## Contexto do registro

Este procedimento foi validado durante a publicação da página rastreável do experimento 54:

- Página: `https://pagamentopalf.site/sales-page-exp54.html`
- Arquivo versionado: `lead-portal-payments-service/docker/proxy/html/sales-page-exp54.html`
- Melhor forma de acesso ao VPS: SSH por chave pública com `IdentitiesOnly=yes`.
