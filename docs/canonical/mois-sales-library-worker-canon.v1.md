# Cânone — MOIS Sales Library Worker (v1)

## Objetivo
Processar de forma assíncrona os jobs `PENDING` da biblioteca de páginas de vendas do MOIS, executar extração básica da página e gravar análise estruturada para uso comercial.

## Fluxo operacional
1. Worker chama `POST /api/mois/sales-library/jobs:claim`.
2. Backend retorna um job `PENDING` e o promove para `FETCHING`.
3. Worker busca a URL da página e executa análise estruturada inicial.
4. Em sucesso, worker chama `POST /api/mois/sales-library/jobs/{jobId}:complete` com score e JSONs (`sections_json`, `copy_json`, `visual_json`, `image_json`).
5. Em falha, worker chama `POST /api/mois/sales-library/jobs/{jobId}:fail` com categoria/mensagem.

## Contrato mínimo de saída da análise
- `score_total`: pontuação comercial agregada.
- `sections_json`: presença de headline/CTA/provas.
- `copy_json`: estrutura de copy extraída.
- `visual_json`: sinais visuais detectados.
- `image_json`: metadados de imagens relevantes.
- `analysis_notes`: observações do processamento.
- `parser_version`, `prompt_version`, `model_name`: rastreabilidade técnica.

## Regras
- O worker não acessa banco diretamente; usa apenas API do backend principal.
- Toda transição de estado deve ocorrer por endpoint do backend MOIS.
- Falhas de parsing/fetch devem marcar job como `FAILED` com causa explícita.
