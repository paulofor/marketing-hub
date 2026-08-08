# Matriz de homologação — hipótese vinculada ao produto

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Criar hipótese após selecionar nicho e produto | Hipótese persiste `product_id` e aparece selecionada |
| Validação | Criar sem produto | Backend responde 400 |
| Validação | Produto pertence a outro nicho | Backend responde 400 |
| Segregação | Trocar produto na tela | Hipótese e território anteriores são limpos |
| Segregação | Gerar oferta com hipótese de outro produto | Backend responde 400 |
| Segregação | Criar experimento com hipótese de outro produto | Backend responde 400 |
| Legado | Hipótese sem `product_id` | Continua auditável, mas não aparece nem cria novo experimento |
| Observabilidade | Erro de contrato | Resposta funcional identifica a divergência |
| Segurança comercial | Concluir criação válida | Experimento nasce `PLANNED`, sem publicar ou ativar mídia |
| Navegadores | Chromium desktop e emulação iPhone/Pixel | Seleção e criação permanecem utilizáveis |

Métricas de homologação: zero mistura entre produtos, zero campanha ativada e zero gasto. Dados locais devem usar entidades efêmeras da suíte de testes.
