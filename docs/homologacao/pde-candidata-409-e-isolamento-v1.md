# Homologação da candidata MUSA v8 — gate 409 e isolamento

Data: 2026-09-18  
Produto: Vega (4), experimento 92, experiência `musa-pde-entry-v12-primeiro-ajuste-aplicavel`.

## Causa confirmada

O smoke de candidata buscava corretamente o contrato interno autenticado, mas exigia também que
as rotas públicas centrais retornassem sucesso. Enquanto a versão não está promovida, essas rotas
devem responder `409`; por isso o workflow 35354613361 reprovou uma proteção correta.

## Decisão adotada

Foram comparadas três alternativas:

1. Liberar a candidata pelas rotas públicas centrais: reduziria o erro, mas quebraria o gate.
2. Ignorar todas as rotas públicas na candidata: reduziria a cobertura da paridade que Psique usa.
3. Usar o contrato interno autorizado, exigir `409` no backend público e comparar a resposta da PDE
   pública com o contrato interno: preserva segurança e comprova o artefato. Esta foi a escolhida.

## Matriz local

- candidata protegida com `409` estruturado nas duas rotas públicas centrais;
- bloqueio de vazamento indevido do contrato candidato;
- bloqueio de divergência entre contrato interno e a PDE pública;
- versões v5–v7, backend e workers preservados durante promoção e rollback da v8;
- namespace Compose injetado por execução, sem depender de identificador de sandbox histórico;
- nenhum pagamento, campanha, e-mail, tráfego comercial ou chamada paga de IA.

## Limite

A v8 atual já apresenta a privacidade recolhida no navegador público, mas a atividade #449 continua
histórica e bloqueada até a publicação autorizada desta correção e a comprovação oficial do novo
fingerprint. A retomada de Psique permanece fora desta homologação para não gerar uma nova revisão paga.
