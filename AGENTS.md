# AGNETS.md — Contrato operacional

## Missão do sistema

A missão do Marketing Hub é criar produtos digitais que realmente transformem a vida das pessoas, resolvendo necessidades reais com melhoria prática, percebida e aplicável. Lembrando que o consumidor é um ser humano e ele é regido pelo **principio fundamental de afastar a dor e o esforço, buscar a facilidade e o prazer**.

O objetivo comercial do sistema é identificar necessidades relevantes de mercado, entender onde existe oportunidade concreta de transformação, pesquisar como essa melhoria pode ser alcançada de forma plausível e convertê-la em produtos digitais produzidos com apoio de IA e com viabilidade comercial.

Toda decisão de arquitetura, dados, prompts, automações, integrações e artefatos deve reforçar esta missão:
- descobrir necessidades reais, não inventar demandas artificiais;
- buscar mecanismos e melhorias com potencial de gerar resultado concreto;
- transformar conhecimento em produto digital claro, útil, escalável e vendável;
- usar IA como meio de produção, estruturação e aceleração, sem perder aderência à realidade do usuário e do mercado;
- priorizar soluções que combinem transformação real para o cliente com sustentabilidade econômica para o negócio.



## 1. Fontes de verdade

- **Regras**: docs/canonical/system-governance-canon.v2.md sempre leia e atualize se necessário.
- **Artefatos**: docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md sempre leia e atualize se necessário.
- **Modelo de dados**: `docs/modelo-dados-experimento.md`. Alterou entidades ou relacionamentos? Atualize o documento imediatamente.
- **Liquibase / MySQL 5.7**: `docs/database/liquibase-mysql57.md`. Use sempre `databaseChangeLog` em YAML, `preConditions` com `dbms:mysql`, `splitStatements: true`, `stripComments: true` e valide mentalmente o SQL.

## 2. Convenções de engenharia

- **Servidor MCP** : Chame o endpoint MCP https://mcpserverdigi.shop/mcp via JSON-RPC. Quando precisar analisar casos específicos acesse o banco de dados usando esse servidor.
- **Tecnologias padrão**: Java 21 + Spring Boot 3, React 18 + Vite + TypeScript, Zustand para state, TanStack Query para dados. Formatação: Spotless (backend) e Prettier (frontend).
- **Banco**: MySQL 5.7. Somente o backend acessa o banco; demais módulos conversam via APIs do backend. Prefira filtros no SQL ao invés de pós-processar em memória.
- **Modelo único**: entidades residem no backend. Os demais módulos acessam o banco de dados pelo backend.
- **Fluxo entre containers**: nada de chamadas diretas entre serviços (frontend, workers, lead-portal etc). Todo tráfego passa pelo backend principal; apenas o backend fala com o banco.
- **Novos endpoints**: verifique se o contrato já existe; caso contrário, defina-o no backend, atualize a documentação e adicione testes.
- **Manual do usuário**: todos os links devem usar `target="_blank"`.
- **Frontend**: sempre que alterar o frontend crie os métodos do backend para suportar. Tanto back quanto o front estão sendo executados no mesmo host.
- **Qualidade**: sempre que alterar um módulo Java realizar os testes unitários antes de publicar o PR.
- **Logs**: os logs dos modulos Java Spring Boot podem ser acessados pelo MCP Server.  Chame o endpoint MCP https://mcpserverdigi.shop/mcp via JSON-RPC.
- **Testes Unitários**: os testes unitários precisam sempre estar em concordancia com as regras dos documentos canonicos.
- **Regra Geral (cânone x testes)**: sempre que houver alteração de regra em documento canônico, revisar e atualizar os testes unitários relacionados para manter aderência entre documentação, regras de domínio e validações automatizadas.
- **Telas do Usuario**: as telas de usuario, ou frontend precisam sempre estar dando as informações mais importantes e precisas para  o usuario e ofereçendo so comandos necessários para o direcionamento dos fluxo de processos mantidos pelo sistema. Evite informações contraditórias, em excesso e desorganizadas. Mantenha sempre a conformidade com os documentos canonicos.

## 3. Orientações Práticas:

- **Erro 422 (Procedimento Obrigatório / SOP)**: Trate toda ocorrência de `422 Unprocessable Entity` como divergência entre payload gerado pelo modelo e contrato/validação do backend até prova em contrário.
  - **Fluxo obrigatório (sempre nesta ordem):**
    1. Acessar logs do backend via MCP Server (`https://mcpserverdigi.shop/mcp`, JSON-RPC).
    2. Localizar a requisição que falhou e extrair o payload enviado pelo modelo (campos e valores relevantes).
    3. Comparar o payload com a especificação oficial do artefato em `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`.
    4. Comparar o payload com as validações ativas no backend (DTOs, validators, regras de domínio e contratos de API).
    5. Identificar e reportar **exatamente** qual trecho (campo/estrutura/valor) gerado pelo modelo foi rejeitado.
    6. Informar a causa raiz e a correção proposta (ajuste de prompt, mapeamento, contrato ou validação).
  - **Formato mínimo obrigatório da resposta de diagnóstico:**
    - o que o modelo entregou de forma literal
    - o que a especificação esperava de forma literal
    - diferença entre a entrega do modelo e o que era esperado
    - ação corretiva recomendada
  - Não encerrar análise de 422 sem apontar explicitamente o trecho rejeitado e a validação correspondente.
- **json** : temos que evitar ao máximo json dentro de json. Ou seja json em campo texto de outro json. 


## 4. Framework central do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

Ao alterar qualquer módulo, preserve esse eixo como referência principal de descoberta, modelagem, validação e empacotamento de valor.



## 5. Módulos e responsabilidades

- **MarketingHub Backend / Frontend**: camada administrativa e UI principal do sistema.
- **Facebook Ads Worker**: integração com a API da Meta para campanhas e públicos.
- **Worker AI**: integrações com modelos OpenAI para geração e otimização de ativos.
- **Lead Portal (backend/frontend)**: experiência dedicada aos leads após anúncios.
- **Lead Portal Payments Service**: pagamentos via Mercado Pago.
- **Email Service**: envio transacional integrado ao Amazon SES.
- **Image Watermark Service**: gera marcas-d'água para prévias.
- **Image Zipper Service**: monta e distribui pacotes de produtos/amostras.
- **MCP Server**: servidor de mcp, fica na pasta /mcp-server
- **OPRM** : responsavel por obter a rotina de uma determinada ocupação, importante para entender de forma clara e precisa as dificuldade e dores de um determinado mercado, através da busca concreta em acesso a sites especializados direcionados para o nicho.
- **MDS** : modulo que vai buscar na internet artigos e informações cientificas de ponta e de credibilidade para dar apoio na construção de mecanismos eficazes que vão resolver de fato os problemas do mercado em relação a uma dor. Com esse mecanismo o Marketing Hub usa como base para criar produtos digitais transformadores de fato.

Documente qualquer alteração cross-módulo no cânone correspondente e sincronize contratos antes de integrar.


## 6. Dominios

- **oportunidadebrasil.shop** : apontando para 191.252.120.96
- **pagamentopalf.site** : apontando para  191.252.102.54


## 7. Segurança e secrets

- Nunca commite `.env` ou credenciais. Use GitHub Actions secrets.
- Revise variáveis sensíveis nos pipelines antes de publicar artefatos.
