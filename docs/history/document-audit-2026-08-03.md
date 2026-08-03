# Auditoria documental — 2026-08-03

> STATUS: HISTÓRICO
> ÚLTIMA VALIDAÇÃO: 2026-08-03

## Objetivo

Reduzir ambiguidades para pessoas e modelos sem apagar evidências úteis.

## Decisão

Foram avaliadas três alternativas:

1. Excluir planos e relatórios antigos: baixa ambiguidade, mas alto risco de perder evidências.
2. Apenas renomear arquivos: baixo esforço, mas não cria uma regra clara de precedência.
3. Criar governança, manter uma fonte por duplicata e arquivar históricos: melhor equilíbrio entre clareza, rastreabilidade e custo.

A alternativa 3 foi adotada.

## Alterações aplicadas

- Criado `docs/README.md` como índice de precedência e estados documentais.
- Mantida uma única cópia do estudo de landing page em `docs/pesquisa-profunda/estudo-estrutura-landing.md`.
- Mantida uma única cópia da metodologia de worker OpenAI em `docs/metodologia/como-pedir-worker-openai-do-zero.md`.
- Roadmap de agentes v2 marcado como proposta vigente; versão anterior marcada como obsoleta.
- Snapshot de banco diferenciado explicitamente do modelo documental de referência.
- Exportações numeradas do experimento 10 classificadas como históricas e isoladas da documentação corrente.
- Índices vazios inventariados; a regra nova impede novos marcadores vazios.

## Pendência consciente

Os `ini.md` vazios fora do núcleo documental não foram removidos em massa porque alguns podem ser marcadores esperados por ferramentas ou fluxos ainda não documentados. Devem ser substituídos gradualmente por `README.md` úteis quando cada diretório for revisado.

