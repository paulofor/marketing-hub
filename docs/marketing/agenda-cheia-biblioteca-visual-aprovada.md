# Agenda Cheia — biblioteca visual aprovada

**STATUS: OPERACIONAL**  
**FONTE CANÔNICA:** pipeline de entrega do `lead-portal-payments-service`  
**ÚLTIMA VALIDAÇÃO:** 2026-08-03

## Decisão

O Agenda Cheia Nail Design usa uma biblioteca persistente de fotografias premium aprovadas antes da venda. O pedido não gera fotografias novas enquanto a compradora espera.

## Fluxo comercial

`briefing → seleção de 10 fotos aprovadas → personalização → gate → ZIP → e-mail`

São personalizados por compra: nome profissional, região, WhatsApp, serviços, cores, objetivo, textos, legendas, mensagens e composição. As fotografias podem ser reutilizadas em combinações diferentes porque a promessa comercial é personalização do kit, não exclusividade fotográfica.

## Gate operacional

- O diretório `photo-library/approved` deve conter no mínimo 10 imagens JPG ou PNG.
- Cada imagem deve ter pelo menos 1024 × 1024 pixels.
- Somente fotografias revisadas visualmente e liberadas para uso comercial podem entrar em `approved`.
- A ausência de acervo suficiente bloqueia a entrega; não existe fallback para geração improvisada durante a compra.
- Novas fotografias devem ser geradas em lotes pelo fluxo versionado, revisadas fora da jornada da compradora e promovidas ao diretório aprovado somente após nota visual mínima 9/10.

## Modelo de geração do acervo

- O gerador de lotes usa o snapshot fixo `gpt-image-2-2026-04-21`, com qualidade `high` e saída 1024 × 1024.
- A versão fixa preserva consistência entre lotes e evita mudança silenciosa do alias do provedor.
- A geração acontece antes das vendas. Durante a compra, o pipeline apenas seleciona fotografias aprovadas e personaliza o material da cliente.
- A troca do modelo exige novo lote comparativo e aprovação visual humana mínima 9/10 antes de substituir fotografias do acervo comercial.

## Razão comercial

O modelo reduz custo, prazo e variação de qualidade. A geração integral por venda produziu pacotes tecnicamente válidos, porém visualmente inconsistentes. A biblioteca aprovada preserva a promessa de entrega premium sem depender da estabilidade de dez gerações em tempo real.
