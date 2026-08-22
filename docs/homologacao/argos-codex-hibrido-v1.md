# Homologação — Argos Codex híbrido v1

## Correção preventiva de execução — 2026-08-22

- O prompt é enviado ao processo Codex pela entrada padrão e o stream é encerrado explicitamente.
- Timeout, falha de processo e ausência de resposta estruturada permanecem causas distintas e auditáveis.
- A credencial Brave pode ser fornecida pelos contratos `BRAVE_SEARCH_API_KEY` ou `BRAVE_API_KEY`; o health informa somente a presença, nunca o valor.
- Ofertas e anúncios só entram no dossiê quando correspondem a dois termos específicos da investigação; público, canal e palavras genéricas não completam o gate.
- Consultas dirigidas extensas preservam vagas para intenção comercial e mecanismo plausível dentro do limite operacional.
- Webapps e outros formatos fora de Hotmart/ClickBank podem usar páginas comerciais públicas como alternativas comparáveis, uma por domínio, sem tratar presença, preço ou plano como venda comprovada.
- O mecanismo científico deve ser semanticamente aderente à decisão do cliente; arquivos públicos, homônimos e revisões genéricas não completam o gate.
- O contrato automatizado impede regressão para APIs que ignorem silenciosamente a entrada do agente.

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
