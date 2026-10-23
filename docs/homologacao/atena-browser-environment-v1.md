# Navegador da pesquisa de Atena — 23/10/2026

A pesquisa #21 de Vega concluiu a Big Idea usando fontes alternativas, mas informou
que `consultar_paginas_publicas` não encontrava Chromium. O navegador e a versão
Playwright 1.54.2 estavam instalados na imagem; faltava repassar seu caminho ao MCP.
O histórico de `LOOP-CREATIVE-REVIEW-WITHOUT-DESTINATION-EVIDENCE` registra a mesma
classe de falha em outro revisor. Não houve nova inferência da pesquisa #21.

Alternativas avaliadas: reinstalar Chromium (duplicaria uma dependência presente),
substituir a ferramenta por leitura HTTP (perderia a navegação renderizada), ou
repassar as duas variáveis não sensíveis do navegador (adotada). A configuração
mantém a correlação e o isolamento existentes, sem expor tokens nem permitir shell.

O teste Java exporta a lista do comando Codex real. O teste Node inicia outra execução
com somente esse ambiente e percorre MCP + Chromium, incluindo leitura válida,
falha de rede, bloqueio de escrita/redirecionamento e encerramento concorrente.
Antes da correção, reproduziu o executável ausente no cache padrão. Depois:

- 69 testes Java, incluindo Big Idea, identidade e recuperação, aprovados.
- Oito testes Node aprovados na sandbox e na base Playwright 1.54.2 usada pela imagem,
  sem rede externa, com fontes sintéticas e filesystem do projeto preservado.
- O teste de navegador isolado usa a configuração gerada pelo Java, portanto exige
  `mvn test` antes de `npm test`, como já ocorre no CI do executor.
- Spotless e montagem das provas comerciais existentes conferidos. Nenhuma atestação
  anterior, autorização financeira ou parecer comercial foi substituído.

Hipótese de melhoria: reduzir fontes declaradas inacessíveis por falha de ambiente e
evitar consumo em retentativas sem mudança de causa. A adoção exige leitura real pela
ferramenta nas próximas pesquisas; páginas externas ainda podem negar acesso ou usar
redirecionamentos bloqueados. Acesso técnico e pesquisa pública não comprovam demanda.
