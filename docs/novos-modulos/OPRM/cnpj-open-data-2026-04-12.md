# Base CNPJ aberta (snapshot 2026-04-12) para OPRM

## Objetivo

Documentar os arquivos ZIP publicados em `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/`, suas URLs diretas a importância de cada conjunto para o OPRM no Marketing Hub, com foco inicial em mensuração quantitativa de tamanho de mercado (sem recorte geográfico).

## URL base

- `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/`

## Arquivos e importância para o projeto (foco quantitativo, sem geografia)

### 1) Cnaes.zip
- **URL**: `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Cnaes.zip`
- **O que contém**: tabela de domínio CNAE (código e descrição da atividade econômica).
- **Importância para o OPRM**:
  - classificar nichos por atividade real de mercado;
  - suportar filtros por segmento para análises de dor e oportunidade;
  - base para agregações por setor no eixo Dor → Resultado → Mecanismo → Prova → Oferta.

### 2) Empresas0.zip ... Empresas9.zip
- **URLs**:
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas0.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas1.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas2.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas3.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas4.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas5.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas6.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas7.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas8.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Empresas9.zip`
- **O que contém**: dados da pessoa jurídica no nível da empresa (CNPJ básico), como razão social, natureza jurídica, porte e capital social.
- **Importância para o OPRM**:
  - permitir clusterização de mercado por porte e natureza jurídica;
  - identificar perfil econômico predominante por nicho;
  - apoiar critérios de priorização comercial por potencial de compra e maturidade do público.

### 3) Estabelecimentos0.zip ... Estabelecimentos9.zip
- **URLs**:
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos0.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos1.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos2.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos3.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos4.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos5.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos6.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos7.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos8.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Estabelecimentos9.zip`
- **O que contém**: dados do estabelecimento (matriz/filial), incluindo situação cadastral, CNAE principal/secundário, endereço, município, UF e contato.
- **Importância para o OPRM**:
  - principal base para **contagem de estabelecimentos ativos** por CNAE;
  - suportar cálculo de tamanho de mercado por volume (total de CNPJs operacionais);
  - permitir métricas quantitativas como participação relativa por segmento.

### 4) Motivos.zip
- **URL**: `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Motivos.zip`
- **O que contém**: códigos e descrições dos motivos de situação cadastral.
- **Importância para o OPRM**:
  - explicar eventos de baixa/suspensão/inaptidão;
  - gerar sinais de dor operacional e risco em determinados segmentos.

### 5) Municipios.zip
- **URL**: `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Municipios.zip`
- **O que contém**: códigos e nomes de municípios.
- **Importância para o OPRM (neste momento)**:
  - **baixa prioridade** para o objetivo atual;
  - manter apenas para compatibilidade futura, sem uso analítico obrigatório na fase quantitativa inicial.

### 6) Naturezas.zip
- **URL**: `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Naturezas.zip`
- **O que contém**: tabela de natureza jurídica.
- **Importância para o OPRM**:
  - segmentar mercados por tipo de entidade jurídica;
  - ajustar linguagem de oferta e expectativa de decisão por perfil empresarial.

### 7) Paises.zip
- **URL**: `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Paises.zip`
- **O que contém**: códigos e nomes de países usados nos registros da base CNPJ.
- **Importância para o OPRM**:
  - suporte a casos com vínculo internacional;
  - melhora consistência de dados em cenários com sócios/endereços no exterior.

### 8) Qualificacoes.zip
- **URL**: `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Qualificacoes.zip`
- **O que contém**: tabela de qualificação de sócios e representantes.
- **Importância para o OPRM**:
  - enriquecer leitura de governança e perfil decisor;
  - apoiar hipóteses comerciais sobre quem influencia compra dentro das empresas.

### 9) Simples.zip
- **URL**: `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Simples.zip`
- **O que contém**: situação de opção/exclusão no Simples Nacional e MEI.
- **Importância para o OPRM**:
  - sinalizar maturidade tributária e porte operacional;
  - ajudar na priorização de produtos por capacidade de investimento do público.

### 10) Socios0.zip ... Socios9.zip
- **URLs**:
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios0.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios1.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios2.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios3.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios4.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios5.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios6.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios7.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios8.zip`
  - `https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-04-12/Socios9.zip`
- **O que contém**: quadro societário (identificação e qualificação dos sócios, datas e tipos de participação).
- **Importância para o OPRM**:
  - análise de perfil de decisão e estrutura societária;
  - suporte para estratégias comerciais B2B orientadas a decisores reais.

## Recomendação operacional para o projeto (fase 1: tamanho de mercado)

1. Ingerir primeiro as tabelas de domínio essenciais para classificação quantitativa: CNAE, Naturezas, Motivos e Qualificações (Municípios/Países ficam opcionais nesta fase).
2. Ingerir Empresas e Estabelecimentos para formar o núcleo de métricas de tamanho de mercado.
3. Enriquecer com Simples e Sócios para cortes de maturidade e perfil societário.
4. Publicar agregações via backend para consumo do OPRM, respeitando o modelo único (backend como ponto de acesso aos dados).

## Métricas quantitativas recomendadas (sem geografia)

- **TAM por segmento (CNAE)**: total de estabelecimentos ativos por CNAE principal.
- **Mercado endereçável por porte**: distribuição por porte dentro de cada CNAE.
- **Densidade empresarial por natureza jurídica**: volume por tipo de entidade.
- **Taxa de atividade**: ativos / total por segmento.
- **Penetração Simples/MEI**: proporção de optantes por segmento.
- **Complexidade societária**: média de sócios por empresa e faixas de estrutura societária.
