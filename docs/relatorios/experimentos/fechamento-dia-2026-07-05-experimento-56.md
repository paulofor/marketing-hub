# Fechamento do dia - Experimento 56 - 2026-07-05

## Resumo executivo

O experimento 56 saiu de bloqueio tecnico de criativo e pixel para campanha publicada no fluxo do Facebook Ads Worker.

O objetivo comercial do experimento permanece validar venda low-ticket do produto `Mapa de Recorrencia 7D` para manicure/nail designer, com pagina de venda propria e checkout como proximo passo do funil.

Decisao principal do dia: tratar o pixel como ativo de cluster de mercado. Para este experimento, o pixel `KitEmprNailDesign` foi reaproveitado por aderencia ao cluster de beleza/manicure, em vez de tentar manter pixel por experimento.

## Evidencias registradas

- Experimento: `56`.
- Nicho: `21`.
- Produto: `Mapa de Recorrencia 7D`.
- Tipo: `LOW_TICKET_PRODUCT`.
- Objetivo de campanha: `SALES`.
- Status do experimento no backend: `RUNNING`.
- URL publicada: `https://oportunidadebrasil.shop/flows/exp-56-gerasalespage-v1`.
- Pixel associado ao nicho: `1272936690700110`.
- Readiness atual: sem issues; criativo, pagina e publico estao prontos.
- Fila `/api/facebook-campaigns/experiments-ready`: vazia apos publicacao, indicando que o experimento nao ficou preso na fila.
- Campanha Meta registrada: `120249969046490326`.
- Status da campanha no backend: `ACTIVE`.
- Ad set registrado: `120249969046640326`.
- Anuncio registrado: `120249969047510326`.
- Funil no momento do fechamento: sem dados suficientes de acesso ao checkout ou compra.

## O que foi resolvido

- O contrato de criativos do AI Worker foi corrigido e reprocessado.
- O criativo do experimento 56 ficou aprovavel.
- O bloqueio de pixel foi removido com reaproveitamento de pixel aderente ao nicho.
- O experimento foi liberado pelo contrato canonico do backend.
- O Facebook Ads Worker publicou a campanha e registrou os IDs da Meta no backend.

## Pendencias operacionais

- O backend mostra campanha `ACTIVE`, mas o ad set e o anuncio aparecem como `PAUSED` no retrato operacional consultado no fechamento.
- O ad set aparece com a issue: `Conjunto nao esta vinculado a segmentacao aprovada do experimento.`
- Ainda nao existem dados de funil para concluir sobre criativo, pagina, checkout ou compra.
- A configuracao operacional do `facebook-ads-worker` foi observada anteriormente com `FACEBOOKPIXEL_ENABLED=false`, enquanto o backend bloqueia low-ticket sem pixel. Essa divergencia precisa ser alinhada para evitar novo destravamento manual.
- Foi observado risco de seguranca em registros de passo de publicacao com URL contendo `access_token`; isso deve virar correcao separada para mascarar segredo antes da persistencia.

## Alternativas avaliadas

1. Pixel por experimento.
   - Beneficio: separacao maxima de dados por teste.
   - Risco: fragmenta aprendizado, aumenta manutencao e pode bater em limites/permissoes da Meta.
   - Esforco: alto.
   - Decisao: descartado para a rotina padrao.

2. Pixel unico para todos os nichos.
   - Beneficio: menor friccao operacional e maior concentracao inicial de eventos.
   - Risco: mistura sinais de mercados diferentes e pode prejudicar otimizacao.
   - Esforco: baixo.
   - Decisao: aceitavel apenas como fallback emergencial, nao como regra definitiva.

3. Pixel por cluster comercial.
   - Beneficio: equilibra aprendizado, controle operacional e separacao de sinais por mercado.
   - Risco: exige regra de classificacao de nicho e fallback quando a Meta bloquear criacao.
   - Esforco: medio.
   - Decisao: escolhido como melhor direcao sistemica.

## Aprendizados de marketing

- O gargalo do dia nao foi a oferta, mas a infraestrutura de publicacao: contrato de criativo e pixel.
- Para low-ticket, destravar publicacao sem pagina rastreavel ou sem pixel reduz muito o aprendizado. A campanha precisa medir ao menos visualizacao da pagina, clique para checkout e compra.
- O pixel por cluster e melhor para o Marketing Hub do que pixel por experimento, porque o negocio precisa aprender rapido sem misturar mercados totalmente diferentes.
- Para manicure/nail design, manter o pixel `KitEmprNailDesign` e coerente enquanto o cluster for beleza profissional.

## Proximas acoes recomendadas

- Verificar no proximo ciclo se o ad set e o anuncio sairam de `PAUSED` para entrega efetiva ou se ha bloqueio de revisao/configuracao na Meta.
- Monitorar primeiros sinais: impressoes, CTR, visitas na pagina, clique para checkout e compra.
- Se houver entrega sem clique, revisar criativo e angulo de dor.
- Se houver visita sem checkout, revisar primeira dobra, prova visual da planilha e CTA.
- Se houver checkout click sem compra, revisar preco, confianca, promessa e friccao do Mercado Pago.
- Criar correcao sistemica separada para gestao de pixel por cluster.
- Criar correcao sistemica separada para impedir persistencia de `access_token` em URL de auditoria.

## Decisao para o proximo dia

Nao escalar orcamento ainda.

O experimento deve primeiro provar entrega e intencao minima:

- campanha com entrega real;
- anuncio ativo e revisado;
- visitas chegando na pagina;
- eventos de checkout click registrados;
- custo inicial dentro de faixa aceitavel;
- ausencia de bloqueio tecnico no funil.

Escalar somente depois de sinal minimo de funil. Se o anuncio continuar pausado ou sem entrega, a prioridade deixa de ser marketing e volta a ser correcao operacional da publicacao.
