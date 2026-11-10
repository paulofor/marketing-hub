# Fonte revisada — Beeble SwitchX 3.0: vídeo-para-vídeo com performance preservada

**Data do anúncio:** 10/11/2026. **Estado:** ATIVO no Beeble Cloud; API SwitchX em beta público, mas confirmação explícita de que o endpoint já executa especificamente a versão 3.0 não localizada. **Pesos SwitchX 3.0:** não publicados nesta verificação. O uso de MiniMax H3 como base aberta não implica abertura dos pesos derivados.

## Fontes oficiais consultadas em 10/11/2026
- Lançamento, exemplos e limites: https://beeble.ai/research/switchx-3-0
- Preços e limites Cloud: https://beeble.ai/pricing-cloud
- Documentação geral da API em beta público: https://developer.beeble.ai/
- Tabela de preços da API: https://developer.beeble.ai/pricing

## Achados verificados
- O método **Source-Locked Rendering** preserva, de acordo com a empresa, a performance dentro da máscara alfa do vídeo de origem quadro a quadro; o ambiente e a iluminação são modificados.
- A versão 3.0 adiciona até **4 imagens de referência**, cenários dinâmicos (trânsito, pessoas, partículas, fogo etc.), rastreamento de câmera sem marcadores e instruções sobre o que acontece e quando.
- Suporta saída até **2160p/4K e cor de 10 bits** no plano Cloud Professional; preserva enquadramento e número de quadros. A página de preços indica **máximo de 360 frames para 3.0**, ou cerca de 12 segundos a 30 fps. Não generalizar a fps diferentes.
- Cloud Creator: US$19/mês ou US$16/mês com cobrança anual, 540 créditos/mês, saída 720p; Professional: US$75/mês ou US$60/mês anual, 2.400 créditos/mês, 4K/10-bit. Tarifas Cloud: 6 créditos por segundo em 720p, 20 em 1080p, 80 em 2160p. A licença comercial Cloud é anunciada para planos pagos.
- API Build **separada da assinatura Cloud**: US$0,10 por 30 quadros em 720p ou US$0,30 por 30 quadros em 1080p, recarga mínima US$50; 5 jobs simultâneos; 10 requisições/minuto; exige atribuição visível "Powered by SwitchX" ou "Powered by Beeble" em aplicativos públicos, salvo dispensa Enterprise escrita. O portal não confirma 4K nem o ID de versão 3.0 via API.
- A marca descreve o modelo como próprio, adaptado sobre o MiniMax H3 aberto, com dados/arquitetura ajustados, não como os pesos originais distribuídos pela Beeble.

## Interpretação para produção audiovisual
É uma ferramenta especializada para editar filmagem real, diferente de geradores texto-para-vídeo. Relevante para mudar locação, condições de luz e eventos do cenário mantendo gestos e expressão do apresentador/ator. Não inferir superioridade sobre Kling 4.0, Seedance 2.5 ou o editor Adobe sem benchmark controlado.

## Limites, licença e riscos
- Máscaras alfa ruins, oclusões, cabelos, reflexos e movimento rápido podem reduzir qualidade; exemplos promocionais não são benchmark independente.
- Não afirmar que a API Build suporta SwitchX **3.0** ou 4K até obter confirmação no modelo/endpoint.
- Pagamento Cloud e API são canais distintos; preço de API por quadros não equivale a custo total da peça, que inclui preparação, retries e pós-produção.
- Consentimento/contratos para imagens de pessoas reais e ativos protegidos, especialmente em publicidade; verificar termos comerciais de uso específicos.
