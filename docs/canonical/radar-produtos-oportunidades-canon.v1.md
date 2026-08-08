# Radar de Produtos e Oportunidades — cânone v1

## Objetivo

Transformar evidências públicas e auditáveis de mercado em uma fila diária de pesquisa e validação comercial. O Radar reduz dispersão, mas não substitui experimento próprio, venda confirmada ou decisão humana.

## Responsabilidades

- Hotmart e ClickBank coletam snapshots em seus módulos executores e enviam dados ao backend MOIS.
- A Biblioteca de Anúncios da Meta usa investigação supervisionada; nenhum agente pode raspar a interface, contornar autenticação, CAPTCHA, região ou limite de acesso.
- O backend persiste fontes, sinais, datas, scores, lacunas e recomendações.
- O Agente Radar interpreta somente evidências persistidas e oferece prioridade, hipótese de demanda, saturação, risco, lacunas e próxima ação.
- O Orquestrador pode solicitar análise, mas não transforma recomendação em produto, experimento, publicação ou gasto automaticamente.

## Cadência

- Hotmart: ciclo de listagem diário às 05:00 e enriquecimento às 06:00, `America/Sao_Paulo`.
- ClickBank: alternância horária dos três ciclos, somente quando habilitada e com credencial válida; essa cobertura satisfaz a consolidação diária.
- Meta Ads: observações supervisionadas entram quando cadastradas e são consolidadas no ranking diário.
- Falha ou ausência de credencial deve aparecer como lacuna; nunca deve ser convertida em sinal negativo do mercado.

## Critérios de priorização

Cada oportunidade deve preservar fonte e data. O ranking combina qualidade da página, aquecimento público, recência e saturação. Popularidade, temperatura, gravity, quantidade de anúncios ou longevidade são sinais; nenhum deles comprova vendas.

Saídas permitidas: `PRIORITIZE`, `OBSERVE`, `RESEARCH_MORE`, `SATURATED_REQUIRES_ANGLE` e `DO_NOT_PRIORITIZE`. Toda saída deve explicar evidências, lacunas e próxima ação.

## Limites de autoridade

O Radar não copia marca, texto, criativo ou produto; não cria produto ou experimento; não altera preço; não publica; não envia comunicação; não ativa mídia; não gasta. Uma oportunidade só se torna comercialmente validada por vendas reais, entrega satisfatória e métricas próprias do Marketing Hub.

## Observabilidade

Coletas devem registrar request, URL, payload bruto recebido, status, volume, falha e correlação do job sem expor credenciais. A tela deve mostrar fonte indisponível e ausência de evidência explicitamente.
