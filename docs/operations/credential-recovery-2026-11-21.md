# Recuperação de credenciais do Marketing Hub

Em 21/11/2026, a recuperação estava **parcial**. A integração do MCP com
o GitHub voltou a responder após a substituição do token de leitura e o
[deploy #253](https://github.com/paulofor/marketing-hub/actions/runs/44861032629).
Os quatro workflows antigos de exposição foram desativados. O acesso
administrativo aos seis servidores foi verificado por SSH.

As seis credenciais SSH de publicação ainda apareciam com datas anteriores
ao incidente de 10/11. O cadastro da identidade administrativa é uma etapa
de recuperação. A troca nos fluxos de publicação e a retirada das identidades
antigas nos destinos ainda precisam de comprovação.

O workflow **Audit deployment SSH public fingerprints** permite ao titular
comparar somente impressões públicas SHA-256. Em PR e push, executa testes
sintéticos. O relatório manual indica identidade ou ausência no job. Conexão,
substituição e revogação precisam ser verificadas separadamente.

Para cada credencial afetada, registre o consumidor, a data, a referência
segura da troca, a evidência de invalidação no provedor e a validação do
serviço. Mantenha os valores privados nos controles de credenciais.

Acompanhe os resultados neste registro e na
[execução #3994](https://iahub.xyz/codex/requests/3994). O
[registro original](../homologacao/financial-cumulative-ceilings-v1.md#bloqueio-externo-observado-na-publicação)
preserva o contexto e a condição de publicação.
