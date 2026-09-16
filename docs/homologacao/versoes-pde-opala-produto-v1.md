# Homologação — Versões PDE no produto Opala

Data: 16/09/2026  
Escopo: card do produto Opala, visão consolidada das versões e preservação do fluxo técnico existente.

## Matriz definida antes dos testes

| Dimensão | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Abrir Produtos, acessar **Versões PDE** de um Opala e consultar uma candidata | Identidade, hipótese, mudança, vídeos, oferta, checkout, experimento, trajetória e pendências visíveis |
| Validação | Produto fora do tipo Opala | Botão ausente e endpoint agregado rejeita a consulta |
| Falha | Versão sem contrato, vídeo, checkout ou teste | Backend lista cada ausência; tela não oferece preparação para publicação |
| Integrações | Slots, experimentos e vídeos | Agregação usa as referências persistidas e não inventa aprovação no frontend |
| Observabilidade | Homologação e URL | Último resultado e data de validação permanecem visíveis |
| Métricas | Versão vinculada a experimento | Tabela técnica preserva sinais segmentados por `experienceVersion` |
| Dados de teste | Testes unitários e navegação local | Fixtures locais; nenhuma campanha, cobrança ou métrica produtiva alterada |
| Navegadores/dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 | Card, trajetória, links e pendências legíveis e acionáveis |

## Evidências de execução

- backend: 94 testes relacionados aprovados, cobrindo visão consolidada, slots, prontidão do ciclo,
  integração da jornada, oferta/checkout e monitor pós-publicação;
- após o ajuste final de compatibilidade com cadastros Opala legados, os 30 testes diretamente
  afetados de serviço e controller foram repetidos com sucesso;
- frontend: 16 testes aprovados para card do produto e tela de versões;
- TypeScript: `tsc --noEmit` aprovado;
- build de produção do frontend aprovado;
- navegação local com dependências simuladas: fluxo **Produtos → Versões PDE** aprovado em Chromium
  desktop (1440 px), iPhone 15 Pro (393 px) e Pixel 7 (412 px);
- nas três resoluções, candidata e publicada ficaram visíveis, a pendência da candidata foi exibida,
  links externos abriram em nova aba e não houve rolagem horizontal;
- screenshots locais de inspeção: `/tmp/opala-pde-versions-desktop.png`,
  `/tmp/opala-pde-versions-iphone.png` e `/tmp/opala-pde-versions-pixel.png`;
- nenhum dado produtivo, campanha, cobrança, PR ou deploy foi alterado.
