# Renovação segura do MCP — 20/10/2026

## Caso e causa comprovada

- Alvo: `https://mcpserverdigi.shop/mcp`, host `191.252.210.83`, proxy
  `marketinghub-mcp-nginx`. Solicitação de infraestrutura independente dos produtos.
- O certificado em disco e o servido publicamente têm serial
  `05C3A36FC252210FCD73B040C85756FDEE2A`, emitido em 20/07/2026 e expirado em
  **18/10/2026 às 23:15:12 UTC**. Python e curl com confiança padrão recusaram a conexão.
- O cron executava Certbot diariamente, mas passava um `--deploy-hook` com
  `docker exec ... nginx -s reload` para dentro do container. A imagem oficial não
  contém esse comando. A reprodução local com Certbot 5.8.0 encerrou com código 1:
  `Unable to find deploy-hook command docker in the PATH.` Não houve chamada ACME.
- O journal de 20/10 às 03:17 mostra o agendamento e `No MTA installed, discarding output`.
  A saída daquela tentativa não foi preservada; não se atribui um erro exato a cada execução
  histórica. Há duas emissões no arquivo (abril/julho), sem sucessora de julho.
- O deploy anterior verificava existência dos arquivos, sem verificar expiração ou HTTPS
  público. O registro de abril com porta HTTP fechada é histórico; hoje a porta responde
  e o desafio tem rota própria. Não foi tratado como a causa atual.

## Alternativas comparadas

| Caminho | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Corrigir somente o cron no host | Menor alteração imediata | Mantém drift operacional e saída descartada | Não adotado isoladamente |
| Renovação no workflow existente do MCP | Agenda, logs, falha visível, fila e prova pública no mesmo controle | Depende de Actions/SSH; duas janelas diárias e validade mínima de 14 dias antecipam problemas | Adotado |
| Certbot permanente com acesso ao Docker | Renovação próxima ao proxy | Novo serviço e privilégio desnecessário para recarregar Nginx | Não adotado |

## Matriz local

Sem produção, dados de clientes, inferência, campanha ou custo comercial nos testes.
O fluxo HTTP é simulado; os certificados, CA e handshakes TLS são locais e reais.
Não há alteração de frontend: navegadores/dispositivos não mudam o contrato TLS.

| Aceite | Cobertura |
| --- | --- |
| Renovar o vencido e carregar o novo | Shell real, CA/Docker simulados, certificado novo verificável |
| Preservar validade e retomar recarga interrompida | Certificado válido não reemitido; recarga reaplicável |
| Falhas antes do aceite | Montagem errada, desafio inacessível, CA recusando, configuração e reload inválidos |
| Isolamento | Apenas proxy e linhagem MCP; outro produto/domínio recusado; cron alheio preservado |
| Confiança real | TLS local válido, expirado, CA não confiável e nome incorreto |
| Observabilidade | HTTP 200 com erro MCP recusado; `tools/list` e `db_health` funcionais exigidos |
| Recuperação e publicação | Scope MCP isolado; fila sem cancelamento, revisão integrada e retomada automática |
| Higiene | Bash, ShellCheck, Actionlint e limpeza de topologia Compose |

## Resultado local

Os 16 testes de `scripts/test-mcp-tls.py` passaram após corrigir uma lacuna encontrada
na primeira rodada: `openssl x509 -checkhost` pode retornar zero mesmo sem correspondência;
agora o shell exige também a resposta afirmativa. O verificador externo usa a validação
de hostname nativa do TLS. A correção cobre o MCP e um domínio diferente, além de
preservar o caminho válido e a recuperação de reload.

Também passaram os 91 testes Java do MCP, os contratos de publicação/recuperação,
o isolamento do escopo MCP, Bash, ShellCheck e Actionlint com suporte a `queue: max`.
A topologia Compose local foi removida.

O harness passa a testar disponibilidade segura da ferramenta antes de tratá-la como
operacional. Métrica de aceite: consultas diretas com verificação TLS integral. A hipótese
operacional é reduzir interrupções de diagnóstico; não há resultado de vendas atribuído
a essa mudança. Evidências da aplicação e da nova validade serão anexadas ao PR após o deploy.

Referência: [documentação oficial de renovação do Certbot](https://eff-certbot.readthedocs.io/en/stable/using.html#renewing-certificates).
