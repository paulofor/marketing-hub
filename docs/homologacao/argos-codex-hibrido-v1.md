# Homologação — Argos Codex híbrido v1

| Área | Caminho feliz | Validação/falha | Evidência esperada |
|---|---|---|---|
| Direção | Codex gera perguntas e consultas para um ciclo real | resposta fora do schema bloqueia o ciclo | plano, resposta bruta e modelo persistidos |
| Marketplaces | plano solicita Hotmart e ClickBank com até 25 produtos | marketplace desconhecido ou volume maior é rejeitado | pedido sem credenciais e correlação pelo ciclo |
| Busca pública | consultas dirigidas entram antes das consultas genéricas | indisponibilidade não fabrica evidência | URLs e payloads brutos auditáveis |
| Qualidade | compara pelo menos 10 ofertas | menos de 10 mantém `RESEARCH_MORE` | preço, promessa, mecanismo, sinais e lacunas |
| Segurança | agente opera sem segredos dos coletores | compra, afiliação e publicação são proibidas | zero efeito externo |
| Observabilidade | falha contém ciclo, etapa e causa | resposta inválida não conclui pesquisa | logs e erro persistido |
| Segregação | fixtures e replay não entram como evidência real | dados de teste são identificados | ciclos de teste separados |

Primeira rodada local deve cobrir contrato, worker, backend e changelog. Se revelar defeito,
após a última correção são obrigatórias duas rodadas completas consecutivas sem falhas.
