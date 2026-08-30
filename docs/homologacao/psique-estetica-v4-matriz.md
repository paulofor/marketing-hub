# Matriz de homologação — Psique estética v4

## Objetivo

Comprovar localmente que Psique avalia composição visual de forma contextual, auditável e
preventiva antes de aprovar uma superfície comercial. A homologação não publica ativos, não chama
modelo externo e não produz eventos de funil.

## Critério comercial

- **Gargalo corrigido:** um parecer visual genérico podia aprovar uma página legível, mas monótona,
  sem demonstrar equilíbrio texto–imagem, ritmo, cor, variedade ou conexão humana contextual.
- **Métrica esperada:** 100% dos pareceres v4 com pixels persistem as sete dimensões estéticas,
  arquétipo, conexão humana, evidência e déficit crítico.
- **Continuar:** contrato completo e aprovação somente com todas as notas aplicáveis a partir de 3.
- **Ajustar:** falso bloqueio por ausência legítima de pessoas, paleta contida ou arquétipo.
- **Parar:** aprovação sem evidência, regressão de compatibilidade ou mistura com vendas reais.

## Casos ponta a ponta

| Área | Caminho/caso | Resultado obrigatório |
| --- | --- | --- |
| Caminho feliz | Landing visual equilibrada, com pessoas demonstrando uso | v4 aceita o parecer e permite `APPROVED` quando todas as notas são pelo menos 3 |
| Caminho feliz contextual | Vitrine ou aplicação sem pessoas, cuja promessa não exige identificação humana | ausência não vira cota nem bloqueio artificial |
| Validação | Modalidade `VISUAL` sem `visualComposition` | execução é rejeitada antes de persistir sucesso |
| Validação | Função humana declarada sem pessoa observada | contrato é rejeitado como incoerente |
| Gate | Uma dimensão abaixo de 3 com decisão `APPROVED` | aprovação é rejeitada |
| Gate | Déficit visual crítico com decisão `APPROVED` | aprovação é rejeitada |
| Sem pixels | Avaliação sem modalidade visual | notas ficam em zero e arquétipo em `NOT_APPLICABLE` |
| Schema | Seis schemas atuais | objetos fechados, campos obrigatórios, constantes tipadas e subconjunto estrito do provedor |
| Worker | Avaliação, observação e quatro atividades BPM | todos carregam núcleo v4, prompt/schema atuais e validação funcional |
| Backend | Nova avaliação sem versão explícita | backend persiste `BEHAVIORAL_V4`; versões anteriores continuam aceitas para replay |
| Frontend | Biblioteca de personas | seletor oferece v4 como padrão e identifica v1–v3 como compatibilidade/histórico |
| Auditoria | Harness, health e banco | artefatos v4 aparecem no detalhe; executor e agente convergem na versão 5 |
| MySQL 5.7 | Migração aplicada e reaplicada na fixture | snapshot v4 criado uma vez, paths corretos e nenhuma versão duplicada |
| Observabilidade | Resultado real futuro | request, resposta bruta, modelo, tokens, custo, tarefa e evidências continuam correlacionados |

## Navegadores, dispositivos e evidência de referência

A captura produtiva continua cobrindo a página completa em Chromium. A matriz considera desktop
de 1440 px, iPhone 15 Pro e Pixel 7, os mesmos contextos relevantes para hierarquia, densidade,
respiro e continuidade entre dobras. O contrato não define quantidade universal de imagens, cores
ou pessoas; cada parecer deve explicar a função no arquétipo e no dispositivo observado.

## Segregação e custo

Fixtures, respostas simuladas e banco MySQL 5.7 efêmero usam somente dados locais. Destinatários,
campanhas, preço, publicação e orçamento ficam fora do escopo. Nenhuma aprovação simulada conta
como venda, conversão ou preferência humana confirmada.
