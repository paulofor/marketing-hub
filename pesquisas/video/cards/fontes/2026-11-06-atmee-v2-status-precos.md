# Atmee V2 — análise de fonte revisada em 06/11/2026

**Fato:** Atmanity apresentou Atmee V2 em 04/11/2026, segundo registro de lançamento. O site oficial descreve um modelo para avatares interativos de quadro completo com comportamento e expressões dirigidos pelo Causeur.

**Status verificado:** Atmee V1 ATIVO, inclusive no plano Free. Atmee V2 com ACESSO LIMITADO: planos Growth, Pro, Scale e Enterprise, ou código de convite para planos inferiores; a API documenta que algumas contas podem receber erro 403 até a liberação. V2 HD somente Enterprise segundo tabela oficial atual. Pesos do V2 NÃO disponibilizados; SaaS/API proprietários.

**Operação:** criação de avatar pela API `POST /v1/avatars` por JSON ou ZIP, foto e amostra de voz de 10–60 s; posteriormente `POST /v1/sessions`. O plano Growth custa US$49/mês, inclui 365 créditos; V2 full-stack custa 2 créditos/min e V2 avatar-only 1 crédito/min. V2 512×512; a empresa anuncia corpo inteiro, ângulos variados, comportamentos em tempo real. Latência e realismo não foram testados independentemente.

**Condições:** consentimento explícito para fotos/vozes de pessoas; verificar contratos, limitações de volume, PT-BR e custos antes do uso comercial. Não confundir o renderizador de avatar interativo com geradores de longa-metragem. Não há benchmark independente robusto em condições reais.

**Fontes originais e de datação:**
- https://www.atmanity.io/
- https://www.atmee.ai/
- https://www.atmee.ai/pricing
- https://www.atmee.ai/docs/api/create-avatar
- https://runtimewire.com/article/chen-ping-yu-atmee-v2-real-time-avatar-model (04/11/2026, secundária para data)

**Oportunidade Marketing Hub:** criar atendimento comercial audiovisual interativo com persona licenciada e conectar a um pipeline LLM próprio, comparando preço/latência/consistência com soluções similares. 
