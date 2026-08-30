# Homologação — navegador público da Biblioteca Meta no Argos v1

Data: 2026-08-30

## Objetivo

Comprovar que o Product Discovery Worker consegue observar, em um Chromium efêmero, a busca
pública oficial da Biblioteca de Anúncios da Meta preparada pelo backend e entregar fatos
estruturados ao Argos. O fluxo não autentica usuário, não persiste cookies, não contorna CAPTCHA,
não publica campanha e não interpreta anúncio como venda.

## Matriz ponta a ponta

| Eixo | Cenário | Resultado obrigatório |
| --- | --- | --- |
| Caminho feliz | Busca oficial brasileira, filtro Instagram confirmado e cards ativos visíveis | Persistir IDs, anunciantes, textos, formatos, destinos, instante, URL e payload bruto; devolver OBSERVED |
| Validação | URL fora de facebook.com/ads/library, país divergente, plataforma diferente ou investigação de outro ciclo | Rejeitar antes de gravar qualquer observação |
| Resultado vazio | A própria interface confirma zero resultados com país, Instagram e status ativo aplicados | Registrar NO_MATCHING_ACTIVE_ADS; não fabricar anúncio nem tratar isso como venda |
| Falha externa | CAPTCHA, login obrigatório, bloqueio, timeout, página incompleta ou mudança de layout | Registrar AWAITING_SUPERVISED_OBSERVATION; preservar a sessão humana e nunca declarar ausência de mercado |
| Idempotência | Retry do mesmo lease e collectorRunId | Não duplicar observação nem aumentar longevidade artificialmente |
| Integração | Worker solicita e reporta pelo controller Product Discovery | Não chamar controller MOIS nem banco diretamente; backend continua autoridade de persistência e avanço |
| Observabilidade | Toda navegação e callback | Registrar ciclo, investigação, URL sem segredo, status HTTP, desfecho, duração e contagens; health expõe o último desfecho |
| Segurança | Sessão Chromium | Contexto efêmero, sem storage state, senha, token, cookie persistente, download, clique de publicação ou evasão de controles |
| Métricas | Relatório de Argos | Separar anúncios, ativos, anunciantes e modo de coleta; nenhuma dessas medidas conta como compra, receita ou venda |
| Segregação de teste | Fixtures locais e backend test double | Usar IDs, domínios e ciclos sintéticos; não gravar dados de homologação em produção |
| Compatibilidade | Runtime do executor | Validar Chromium empacotado na imagem Docker; a sessão humana de fallback continua coberta em desktop, iPhone 15 Pro e Pixel 7 |

## Critérios de decisão

- **Continuar:** navegador confirma filtros e devolve cards válidos ou um vazio explícito.
- **Ajustar:** existe conteúdo funcional, mas o parser perdeu um campo ou o contrato de
  persistência rejeitou uma evidência legítima.
- **Parar a automação e usar fallback:** a fonte exige login, CAPTCHA, consentimento interativo
  não resolvível, limitação de acesso ou deixou de expor fatos verificáveis.

Uma resposta HTTP inicial diferente de 2xx não decide sozinha o resultado: a Biblioteca pode
renderizar a interface pública e os cards mesmo assim. O gate funcional depende dos filtros e fatos
visíveis; erro HTTP sem conteúdo verificável continua sendo falha externa.
