# Homologação local da continuação Vega #91 e Mira

Executa como uma unidade os contratos alterados nesta continuação: backend principal, frontend
administrativo, jornada privada de Mira, Têmis e imagens Docker temporárias. O runner reutiliza a
matriz isolada de Mira com MySQL 5.7, desktop, iPhone 15 Pro e Pixel 7 e acrescenta o `verify`, MCP e
imagem do `meta-ad-approver-worker`.

```bash
export MIRA_DOCKER_PROJECT='<projeto Compose exclusivo informado pela sandbox>'
bash scripts/run-docker-homologation.sh \
  bash infra/testing/vega91-mira-continuation/run-round.sh final-1
bash scripts/run-docker-homologation.sh \
  bash infra/testing/vega91-mira-continuation/run-round.sh final-2
```

Os dados de leitura são exclusivamente sintéticos e locais. O runner não chama Meta, não usa os
convites humanos produtivos, não solicita liberação, não altera experimento e não registra venda.
Evidências ficam em `tmp/vega91-mira-<rodada>` e `tmp/mira-reading-<rodada>`, ignorados pelo Git.
