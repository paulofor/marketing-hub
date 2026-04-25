# MOIS — Sprint 0 (Descoberta técnica, contratos e compliance)

## Objetivo da Sprint 0

Definir, antes de qualquer implementação operacional, os contratos e regras mínimas para coleta automática com janela temporal (7/30 dias), score de sinais de sucesso e rastreabilidade completa.

---

## 1) Catálogo inicial de fontes suportadas

> Status Sprint 0: catálogo inicial definido para fase de implementação incremental.

| Fonte | Tipo de acesso | Cobertura inicial | Sinal primário de sucesso | Risco/política |
|---|---|---|---|---|
| Meta Ad Library | Busca pública web (sem autenticação no MVP) | Anúncios ativos por anunciante/termo/país | Recorrência de criativo + persistência da oferta no período | Respeitar termos da plataforma e limite de frequência de coleta |
| TikTok Creative Center | Busca pública web (com filtros públicos) | Top ads por país/indústria/objetivo | Ranking e presença recorrente na janela | Monitorar mudanças de estrutura e limites do portal |
| Google Ads Transparency Center | Busca pública web | Transparência de anúncios por anunciante e região | Persistência de campanha + consistência de mensagem | Respeitar uso permitido de dados públicos |
| YouTube | API oficial (fase posterior) / metadados públicos no MVP | Conteúdos de vendas (VSL/webinar) por tema | Frequência de publicação + sinais de engajamento relativo | Observar políticas de API e cotas |
| ClickBank Marketplace | Navegação pública | Produtos por categoria e gravidade | Gravidade + sinais de permanência da oferta | Respeitar termos de uso da plataforma |
| Digistore24 Marketplace | Navegação pública | Produtos por categoria/idioma | Ranking/visibilidade e recorrência temporal | Respeitar termos da plataforma |
| JVZoo | Navegação pública | Lançamentos e ofertas digitais internacionais | Atividade recorrente de ofertas por nicho | Respeitar termos da plataforma |

### Estratégia de expansão (fora da Sprint 0)
- adicionar provedores com API oficial primeiro;
- manter conectores por fonte (adapter pattern) para reduzir acoplamento.

---

## 2) Contrato oficial de coleta automática (proposta Sprint 0)

## 2.1 Request — iniciar job de coleta

```json
{
  "workspaceId": "workspace-default",
  "niche": "emagrecimento",
  "marketTheme": "perda de gordura para mulheres 30+",
  "sources": ["META_AD_LIBRARY", "CLICKBANK", "YOUTUBE"],
  "timeWindow": "LAST_7_DAYS",
  "limitPerSource": 50,
  "locale": "pt-BR",
  "country": "BR",
  "minSuccessScore": 60
}
```

### Regras mínimas
- `workspaceId`, `niche`, `sources`, `timeWindow` são obrigatórios;
- `timeWindow` permitido: `LAST_7_DAYS` ou `LAST_30_DAYS`;
- `limitPerSource`: 1..200 (default sugerido: 50);
- `minSuccessScore`: 0..100 (default sugerido: 50).

## 2.2 Response — criação do job

```json
{
  "jobId": "mois-collect-20260425-001",
  "workspaceId": "workspace-default",
  "status": "QUEUED",
  "createdAt": "2026-04-25T12:00:00Z",
  "timeWindow": "LAST_7_DAYS",
  "sources": ["META_AD_LIBRARY", "CLICKBANK", "YOUTUBE"]
}
```

## 2.3 Response — listagem de referências coletadas por job

```json
{
  "jobId": "mois-collect-20260425-001",
  "items": [
    {
      "referenceId": "ref-001",
      "source": "CLICKBANK",
      "collectedAt": "2026-04-25T12:15:22Z",
      "title": "Oferta exemplo",
      "url": "https://example.com/oferta",
      "niche": "emagrecimento",
      "successSignal": {
        "score": 74,
        "confidenceLevel": "MEDIUM",
        "evidenceCount": 3,
        "primaryReason": "Recorrência de oferta no período + consistência de promessa"
      }
    }
  ]
}
```

---

## 3) Contrato de “sinal de sucesso” (mínimo obrigatório)

Cada referência coletada deve conter:

- `score` (0–100): índice composto de sinal de sucesso;
- `confidenceLevel` (`LOW`/`MEDIUM`/`HIGH`);
- `evidenceCount` (inteiro >= 0);
- `primaryReason` (texto objetivo explicando por que recebeu o score);
- `source` + `collectedAt` + `url` para auditabilidade;
- `timeWindow` e `jobId` para lineage de execução.

### Fórmula inicial sugerida (documental)

`successScore = (0.35 * recorrencia) + (0.30 * consistenciaMensagem) + (0.20 * sinalEngajamentoRelativo) + (0.15 * provaDisponivel)`

> Observação: pesos poderão ser calibrados na Sprint 2 com dados reais.

---

## 4) Matriz de risco legal/compliance por fonte (Sprint 0)

| Fonte | Risco principal | Severidade | Mitigação obrigatória |
|---|---|---|---|
| Meta Ad Library | Coleta excessiva / uso fora de termo | Média | Throttling, cache e revisão periódica dos termos |
| TikTok Creative Center | Mudança de layout / restrição de acesso | Média | Adapter isolado + monitor de falhas por parser |
| Google Ads Transparency | Interpretação incorreta de uso permitido | Média | Revisão jurídica e uso apenas de dados públicos |
| YouTube | Limite de cota API (fase API) | Alta | Planejamento de cotas + fallback de metadados públicos |
| ClickBank | Restrição de scraping indevido | Média | Intervalo entre requisições e conformidade com termos |
| Digistore24 | Variação de estrutura e uso indevido | Média | Parser resiliente + governança de coleta |
| JVZoo | Limites de acesso e políticas de uso | Média | Rate limit + revisão de conformidade trimestral |

---

## 5) Fallback explícito (obrigatório)

Quando uma fonte não entregar métrica direta, aplicar fallback em 2 níveis:

1. **Fallback de sinal**  
   usar proxies documentados (recorrência temporal + consistência textual + presença multi-fonte).

2. **Fallback operacional**  
   se a coleta falhar para uma fonte:
   - marcar status parcial (`COMPLETED_WITH_WARNINGS`);
   - registrar motivo técnico;
   - seguir com as demais fontes sem bloquear o job.

---

## 6) Critério de aceite de Sprint 0

Sprint 0 é considerada concluída quando:
- contratos de request/response e sinal de sucesso estiverem documentados;
- catálogo inicial de fontes estiver definido;
- matriz de risco/compliance estiver registrada com mitigação;
- fallback estiver explicitado para ausência de métrica e falha operacional.

