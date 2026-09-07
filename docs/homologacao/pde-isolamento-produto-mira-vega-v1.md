# Matriz de homologação — isolamento de runtime PDE por produto v1

Data: 2026-09-07

## Objetivo e evidência inicial

Garantir que uma alteração, publicação, reinício ou rollback de Mira (produto `10`) não altere a
superfície do Vega/Método MUSA (produto `4`) e que a mesma proteção seja reutilizável pelos próximos
PDEs.

A inspeção produtiva anterior à mudança encontrou somente o container
`pde-platform-frontend-v7`, com a imagem `pde-platform-frontend-v7`, servindo simultaneamente o MUSA
v7 e a rota `/mira-private`. O smoke de Mira também usava `https://v7.clubemusa.com.br`. Portanto,
Mira não possuía imagem nem container próprios; a separação era apenas lógica dentro do bundle do
Vega.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Registrar somente a regra em documento | Mudança pequena | Mantém o acoplamento produtivo e depende de memória humana | Rejeitada |
| Duplicar backend, banco e workers por produto | Isolamento máximo | Duplica estado, agendamentos e custo; contraria o motor PDE multi-produto | Rejeitada |
| Compartilhar apenas infraestrutura neutra e isolar cada superfície de produto | Deploy e rollback independentes sem duplicar o motor | Exige imagem, container, porta, proxy e smoke próprios | Escolhida |

## Matriz obrigatória

| Área | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Identidade | Vega e Mira usam repositórios de imagem, serviços e containers distintos | Duplicidade entre produtos falha antes do build | Contrato versionado e teste de unicidade |
| Empacotamento | O bundle do Vega não oferece a rota de Mira; a imagem Mira possui entrada própria | Ausência do entrypoint, Dockerfile ou identidade do produto falha | Build das duas imagens e inspeção do conteúdo |
| Compose | Mira sobe em `pde-platform-frontend-mira`, porta `5180`; Vega mantém seus containers versionados | Variável de imagem ou porta ausente falha no `compose config` | Configuração Compose renderizada |
| Proxy | `/mira-private`, seus assets, diagnóstico e API chegam somente ao container Mira | Subrota legada continua 404; upstream ausente não redireciona para Vega | Proxy Nginx real com TLS local e teste de contrato |
| Deploy | `mira` atualiza somente Mira; `v5`, `v6` e `v7` atualizam somente Vega | Target desconhecido falha; push comum não escolhe produto; bootstrap é explícito e temporário | Simulação do workflow e contrato de deploy |
| Integração | Mira usa o backend neutro pela API oficial | Sem token, rota interna permanece protegida; sem backend, health falha | HTTP real na topologia local |
| Observabilidade | Diagnóstico de Mira declara produto, versão, imagem, tag e container próprios | Diagnóstico com identidade Vega ou campo vazio falha | JSON de diagnóstico e health |
| Métricas | QA e `AGENT_VALIDATION` permanecem fora do funil comercial | Nenhuma venda, receita, checkout real ou participante é inferido | Eventos locais segregados e efeitos externos nulos |
| Dados de teste | MySQL, sessões e tokens sintéticos pertencem ao projeto Compose exclusivo | Nenhuma credencial real ou evento produtivo é usado | Volume efêmero removido ao final |
| Navegadores | Jornada Mira funciona em Chromium desktop, iPhone 15 Pro e Pixel 7 | Overflow, erro de console, falha de retomada ou segurança reprova | Playwright nos três perfis |
| Regressão Vega | MUSA v7 continua renderizando e não expõe `/mira-private` no próprio bundle | Import/rota Mira no entrypoint Vega reprova | Teste estrutural e smoke MUSA |
| Ciclo de vida | Reinício/rollback de Mira não referencia container ou imagem Vega | Comando que remove ou recria outro produto reprova | Teste de escopo do workflow |

## Critérios de decisão

- **Continuar:** todos os contratos, builds, containers e jornadas passam; Mira e Vega possuem
  identidades operacionais distintas.
- **Ajustar:** qualquer colisão de imagem, serviço, container, porta, bundle, proxy ou target de
  deploy; corrigir a causa e reiniciar a homologação completa.
- **Parar:** a separação exigir duplicação insegura do banco/rotinas ou mudança externa irreversível
  sem autorização.

Uma rodada inicial completa sem defeitos conclui a homologação. Se uma rodada revelar defeito, após
a última correção são exigidas duas rodadas completas e consecutivas sem falhas.

## Resultado local

Depois da última correção, duas rodadas completas e consecutivas terminaram sem falhas:

- Actionlint dos workflows, contrato da fila compartilhada e contratos de deploy, proxy, smoke e
  arquitetura aprovados;
- builds independentes de Vega e Mira aprovados; a inspeção dos artefatos confirmou ausência de
  conteúdo cruzado entre os bundles;
- 167 testes do backend PDE, 45 testes do serviço de proxy/pagamentos e 4 testes do worker aprovados
  por rodada, sem falhas ou erros;
- 24 jornadas Playwright por rodada aprovadas em Chromium desktop, iPhone 15 Pro e Pixel 7, usando
  MySQL 5.7, backend real e frontends separados na topologia local;
- proxy Nginx com TLS validou a URL histórica de Mira no container próprio e rejeitou Mira no
  container Vega;
- a recriação forçada de `pde-platform-frontend-mira` mudou somente o ID de Mira e preservou o ID
  de `pde-platform-frontend-v7` nas duas rodadas;
- os containers, redes e volumes efêmeros foram removidos ao final.

## Estado produtivo após a verificação

Produção ainda não recebeu esta mudança. Em 2026-09-07, o host `163.245.200.7` possuía somente
`pde-platform-frontend-v7`, na imagem
`ghcr.io/paulofor/pde-platform-frontend-v7:20c1037ce8e98160a4527d13b311ce8d60a1b37d`, porta `5178`,
para as superfícies verificadas. A rota `/mira-private` respondia `200` pelo mesmo runtime, enquanto
`/mira-private/version-diagnostics.json` e `/mira-private/healthz` respondiam `404`; não existia
container `pde-platform-frontend-mira` em execução.

O rollout só conclui o isolamento quando a alteração passar pelo PR e pelos workflows versionados:
primeiro Mira em `bootstrap-legacy-route`, depois o proxy proprietário e por fim Mira em `isolated`.
Nenhuma publicação, campanha, evento comercial ou venda foi criada nesta homologação.
