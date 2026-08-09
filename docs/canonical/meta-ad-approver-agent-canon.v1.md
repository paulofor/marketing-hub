# Agente Aprovador de Anúncios Meta — cânone v1

## Responsabilidade

O Aprovador Meta é um gate técnico anterior à aprovação humana e ao preflight. Atua como especialista em copy de resposta direta, estética comercial de imagens e vídeos e continuidade entre anúncio e página de destino.

## Executor independente

O executor canônico é `meta-ad-approver-worker`. Ele consome somente
`/api/internal/creatives/agent-review/stage-executions/pending`, executa `gpt-5.6-sol` pelo Codex
ChatGPT em sandbox própria `read-only` e envia o parecer exclusivamente pelo callback do backend.
O módulo possui container, usuário sem privilégios, volume de identidade Codex, CI/CD, timeout e
telemetria próprios. O `ai-worker` não pode conter pacote, prompt, schema, executor ou decisão do
Aprovador Meta. Sua responsabilidade limita-se à geração técnica de mídia requisitada pelo backend,
em pacote neutro de materialização visual, sem analisar, pontuar ou aprovar anúncios.

Cada execução materializa o MCP versionado `meta_ad_approver`, restrito ao criativo e ao experimento
reservados. As ferramentas obrigatórias confirmam o contexto no backend, retornam a mídia real em
alta definição ou três quadros do vídeo e renderizam a landing em mobile e desktop. Divergência de
identificador, evidência ausente ou falha de inspeção mantém o gate fechado. O MCP não publica, não
altera campanha e não acessa banco.

## Evidências obrigatórias

- copy completa, CTA, público, hipótese e oferta;
- imagem em alta definição ou três quadros representativos do vídeo;
- URL pública de destino válida;
- screenshots integrais da landing em mobile e desktop;
- mapa de associações de desejo e limites de verdade, quando disponíveis.

Sem qualquer evidência obrigatória, o gate permanece fechado.

## Critérios

O agente avalia separadamente atenção, clareza, desejo, credibilidade e ação. Também registra pareceres explícitos sobre copy, estética comercial e integração anúncio → landing. A aprovação exige ausência de bloqueio, nota mínima 80 em todas as dimensões e coerência comprovada de público, dor, promessa, mecanismo, oferta, identidade e próximo passo.

## Ciclo de melhoria

Em `ADJUST` ou `REJECTED`, o agente entrega textos revisados, prompt visual, requisitos obrigatórios, elementos proibidos e critérios verificáveis. O backend controla até três tentativas, preserva versões, custos, requests, responses e evidências. O executor apenas materializa a correção e reporta o resultado.

## Limites de autoridade

O agente não substitui aprovação humana, não publica, não ativa mídia, não muda preço ou orçamento e não coloca experimento em execução. Impasse ou limite atingido permanece bloqueado com causa persistida.

## Preservação do histórico

A migração de executor não cria nova tabela nem reinicia criativos. Pareceres, versões, tentativas,
custos e estados já persistidos — inclusive os do experimento #88 — permanecem sob os mesmos
contratos do backend. Apenas novas reservas são processadas pelo módulo independente.
