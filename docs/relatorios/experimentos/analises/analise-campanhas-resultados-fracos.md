# Analise das campanhas com resultados fracos

## Diagnostico

As campanhas estao fracas principalmente por quebra de funil depois do clique, nao apenas por anuncio ruim.

Evidencia dos relatorios em `/docs/relatorios/experimentos`:

| Exp. | Nicho | Impressoes | Cliques | CTR | CPC | Views form | Envios |
|---|---:|---:|---:|---:|---:|---:|---:|
| 37 | Personal | 2.524 | 114 | 4,52% | R$0,22 | 129 | 0 |
| 38 | Personal | 2.297 | 14 | 0,61% | R$2,53 | 32 | 0 |
| 39 | Manicure domicilio | 4.297 | 179 | 4,17% | R$0,19 | 109 | 0 |
| 40 | Alongamento unhas | 2.757 | 113 | 4,10% | R$0,15 | 104 | 0 |
| 41 | Alongamento unhas | 781 | 18 | 2,30% | R$0,69 | 24 | 0 |
| 47 | Loja vestuario | 9.987 | 75 | 0,75% | R$0,34 | 42 | 0 |
| 50 | Promocao de vendas | 5.395 | 38 | 0,70% | R$0,66 | 35 | 0 |

O ponto mais grave: **575 visualizacoes de formulario somadas e 0 envios**. Isso indica problema de conversao, landing, formulario ou oferta imediata. Antes de escalar trafego, precisa corrigir esse gargalo.

## Por que esta fraco

1. **A landing/formulario nao converte**

   Os anuncios 37, 39 e 40 geraram CTR bom e clique barato. Mesmo assim, ninguem enviou formulario. Isso sugere friccao, promessa desalinhada, formulario ruim, lentidao, erro de submissao ou recompensa pouco desejavel na primeira dobra.

2. **Alguns experimentos foram publicados sem base minima**

   Nos experimentos 37 e 38, o checklist estrategico aparece como nao aprovado. No 38, o angulo de campanha esta vazio, e o resultado foi ruim: CTR 0,61% e CPC R$2,53.

3. **Metrica de decisao esta ausente**

   Varios relatorios tem `primaryMetric: null`, `primaryVariable: null`, `kpiTargetCpl: 0`. Isso enfraquece o aprendizado: a campanha roda, mas nao existe criterio claro de sucesso, parada ou comparacao.

4. **Os criativos bons atraem curiosidade, mas nao compromisso**

   Os experimentos 39 e 40 tem bons CTRs porque a dor e concreta: agenda vulneravel, cliente some, manutencao sem processo. Mas a conversao zera porque o proximo passo provavelmente exige esforco demais ou nao entrega valor instantaneo claro.

5. **B2B mais amplo teve baixa tracao**

   Os experimentos 47 e 50 ficaram abaixo de 1% de CTR. O publico esta mais frio, os interesses/funcoes aparecem vazios e a promessa exige maturidade operacional. Para esse publico, "planilha/checklist" precisa ser visualmente muito concreta e ligada a uma urgencia real.

6. **Problema tecnico de velocidade**

   Os relatorios 41 e 47 citam "lentidao critica" na landing. O 50 diz que o tempo tecnico ja pode reduzir engajamento inicial. Em trafego pago mobile, isso mata conversao.

## O que melhorar agora

1. **Pausar novos testes ate validar o formulario**

   Fazer um teste operacional ponta a ponta: clicar no anuncio/URL, abrir no mobile, preencher, enviar, receber confirmacao e registrar lead. Meta minima: formulario funcionando e carregando rapido.

2. **Simplificar a primeira conversao**

   Trocar "preencha briefing/diagnostico" por uma recompensa de baixissimo esforco:

   - Manicure: "Baixar 5 mensagens prontas para confirmar horario sem parecer grossa".
   - Alongamento: "Baixar 6 cards de manutencao para mandar no WhatsApp".
   - Loja de roupas: "Baixar planilha semaforo pronta".
   - Promocao de vendas: "Abrir checklist minimo de evidencias por loja".

3. **Reduzir campos**

   Para primeira conversao: nome + WhatsApp ou e-mail. Qualquer diagnostico mais longo deve vir depois do lead.

4. **Usar como base os angulos 39 e 40**

   Eles provaram atracao barata. O problema nao e o topo. Reaproveitar:

   - "Agenda cheia no WhatsApp, mas vulneravel?"
   - "Cliente some da manutencao e volta so quando quebra?"

   Mas mudar a landing para entregar algo imediato.

5. **Corrigir governanca antes de publicar**

   Nao publicar experimento se:

   - checklist da hipotese nao estiver aprovado;
   - angulo estiver vazio;
   - publico/interesses/funcoes estiverem vazios sem justificativa;
   - `primaryMetric`, meta e stop loss estiverem nulos;
   - landing/formulario nao tiver teste real concluido.

6. **Mudar a leitura do CPL**

   CPL nao pode aparecer como R$0,00 quando houve 0 leads. Isso mascara o problema. O correto e "sem leads / CPL indefinido".

## Prioridade recomendada

A maior oportunidade esta em **corrigir a conversao da landing/formulario** e relancar os melhores angulos: experimentos 39 e 40. Eles ja mostraram demanda no clique.

Se, depois de corrigir a pagina, continuarem com 0 envio, ai o problema e oferta/recompensa. Hoje, a evidencia aponta primeiro para gargalo de funil.
