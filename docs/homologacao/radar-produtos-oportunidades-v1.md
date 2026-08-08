# Matriz de homologação — Radar de Produtos e Oportunidades v1

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Hotmart entrega snapshot e o MOIS conclui análise | Oportunidade aparece com fonte, scores, evidência e próxima ação |
| Integração | ClickBank habilitado com token válido | Coleta diária persiste dados sem acesso direto ao banco |
| Falha | Token ausente, inválido ou fonte indisponível | Lacuna auditável; nenhuma oportunidade inventada |
| Meta Ads | Observação supervisionada válida | Evidência entra sem scraping e sem afirmar venda |
| Deduplicação | Mesmo produto reaparece em nova coleta | Histórico é preservado sem inflar o sinal por retry |
| Validação | Score alto sem prova própria | Tela informa que ranking não comprova vendas |
| Autoridade | Oportunidade priorizada | Nenhum produto, experimento, preço, publicação ou gasto é criado automaticamente |
| Segregação | Consulta de outro workspace | Nenhuma evidência cruza workspaces |
| Volume | 50 oportunidades | Resposta limitada e tabela responsiva sem carga irrestrita |
| Observabilidade | Coleta ou análise falha | Job, fonte, horário, status e causa permanecem rastreáveis |
| Navegadores | Chromium desktop | Menu, estados vazio/erro/sucesso, links e tabela funcionam |
| Dispositivos | iPhone 15 Pro e Pixel 7 | Cards e tabela são navegáveis sem perda de conteúdo essencial |

Uma rodada local completa sem defeitos conclui a homologação. Se surgir defeito e houver correção, executar cinco rodadas completas consecutivas após a última correção.

