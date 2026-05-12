# Diagnóstico: item/textura preto e rosa (magenta) no Minecraft Bedrock

## Sintoma observado

O padrão preto e rosa/magenta indica **textura ausente** (missing texture) no cliente Bedrock.

No print enviado, o item aparece como `item:digicom:goo`, o que sugere addon/resource pack customizado com referência inválida para textura.

## O que foi verificado no MCP do projeto

No endpoint MCP do projeto (`https://mcpserverdigi.shop/mcp`), a lista de tools disponível hoje inclui:

- `db_health`, `db_list_tables`, `db_read_table`, `db_query`
- `java_module_logs`
- `meta_docs_get`, `meta_graph_get`, `meta_graph_debug_token`
- ferramentas de GitHub Actions

Não há tool específica para Minecraft/resource pack. Então a causa precisa ser tratada no pacote Bedrock (manifest, paths, cache, compatibilidade de versão), não em configuração do MCP deste projeto.

## Sobre “executar comandos Linux pelo MCP Server”

Você está correto: **em alguns MCP servers existe tool de shell/exec para rodar comandos Linux remotamente**.

No MCP deste projeto, durante esta investigação, essa capability **não apareceu no `tools/list`**. Se essa tool for habilitada depois (ex.: `shell_exec`, `run_command`, `terminal_exec`), ela pode ser usada para auditoria do host com foco em causa-raiz.

### Comandos Linux que valem ouro quando shell remoto estiver disponível

1. **Localizar packs e nomes suspeitos (`digicom`, `goo`)**
   - `find / -type f \( -name '*.mcpack' -o -name 'manifest.json' -o -name '*.png' \) 2>/dev/null | rg -i 'digicom|goo|resource_packs|behavior_packs'`

2. **Validar estrutura de pastas (evitar `pack/pack/...`)**
   - `find <PASTA_DO_PACK> -maxdepth 3 -type d | sort`

3. **Conferir manifesto e UUID/version**
   - `cat <PASTA_DO_PACK>/manifest.json`

4. **Checar se PNG referenciado realmente existe**
   - `rg -n 'digicom|goo|texture' <PASTA_DO_PACK>`

5. **Permissões e dono dos arquivos**
   - `find <PASTA_DO_PACK> -type f -print0 | xargs -0 ls -l`

6. **Hashes para detectar deploy antigo/cacheado**
   - `find <PASTA_DO_PACK> -type f -name '*.png' -print0 | xargs -0 sha256sum | sort`

> Mesmo com shell remoto, o diagnóstico mais comum continua sendo erro no pack (manifest/path/cache), e não `server.properties`.

## Tentativa real de execução de comandos no host via MCP

Foi feita tentativa prática de executar comando Linux no host pelo MCP (`tools/call`), com os nomes de tool mais comuns:

- `shell_exec` → retorno JSON-RPC: `Unknown tool: shell_exec`.
- `run_command` / `execute_command` → tentativas com timeout de upstream no endpoint.

Interpretação técnica:

1. No momento da análise, **não há evidência de tool de shell exposta de forma estável** neste MCP.
2. Sem tool de execução remota disponível, não é possível rodar `find`, `cat`, `sha256sum` diretamente no host via MCP nesta sessão.
3. A investigação viável ficou limitada às tools funcionais de dados/logs já descritas.

## Pesquisa no MCP Server (servidor do projeto)

Foi feita investigação direta no MCP remoto para verificar se o problema poderia estar em dados/serviços do servidor:

- `tools/list`: confirmou que não existe tool específica para Minecraft packs/textures.
- `db_list_tables`: banco com 176 tabelas do ecossistema Marketing Hub.
- `db_query` em `information_schema.tables` para padrões `minecraft`, `texture`, `pack`, `digicom`: retornou apenas tabelas internas de pacotes/entregáveis de imagem (`deliverable_package`, `image_deliverable_package` etc.), sem domínio de assets de Minecraft.
- `java_module_logs` (backend e ai-worker): logs com stacktrace de persistência Hibernate, sem referência a `minecraft`, `digicom:goo` ou erro de carregamento de textura.

**Conclusão da busca no servidor:** não há evidência de que a causa esteja no servidor MCP/Marketing Hub. O defeito permanece caracterizado como problema de addon/resource pack Bedrock no cliente/mundo.

## Evidências técnicas (Microsoft + Minecraft docs)

De acordo com a documentação oficial do ecossistema Bedrock:

1. Todo resource/behavior pack precisa de `manifest.json` válido, com UUIDs e versões consistentes.
2. O Minecraft usa UUID + version para decidir se substitui pack já importado (se versão igual/menor, pode ignorar atualização).
3. A recomendação atual de plataforma é alinhar `min_engine_version` com versões recentes e manter `format_version` de manifest em `2` para packs.
4. Estrutura e tipos de arquivo da pasta do pack precisam estar corretos para o Bedrock carregar os assets (`.png`, `.json`, caminhos esperados).

## Causa-raiz mais provável para o seu caso

Como **nenhuma textura funciona** e até item de orientação ficou preto/rosa, a chance maior é uma destas:

1. `manifest.json` inválido/incompatível (UUID repetido, módulo errado, versão não atualizada).
2. Caminho/nome de textura divergente do identifier do item (`item:digicom:goo`).
3. Pack antigo em cache (mesmo UUID/versão), impedindo recarga correta.
4. Addon convertido de Java para Bedrock com estrutura de pastas/nomes não compatível.

## Resposta direta à sua dúvida

> "Será que falta configuração no `server.properties` para aceitar texturas?"

**Na maioria dos casos, não.** O preto/rosa geralmente é problema do **pack no cliente/world pack**, não permissão do servidor para “aceitar textura”.

## Checklist de correção (ordem recomendada)

1. **Validar `manifest.json` do resource pack**
   - `format_version: 2`
   - `header.uuid` único
   - `modules[].uuid` diferente do `header.uuid`
   - `version` incrementada (ex.: de `[1,0,0]` para `[1,0,1]`)
   - `min_engine_version` compatível com sua versão Bedrock

2. **Conferir tipo de módulo**
   - no resource pack, módulo deve ser `"type": "resources"`.

3. **Revisar o item com erro (`digicom:goo`)**
   - conferir o arquivo JSON do item e a referência literal da textura.
   - conferir se o PNG existe no caminho exato (incluindo maiúsculas/minúsculas).

4. **Forçar refresh de cache de pack**
   - mudar UUIDs (header e module) + subir versão.
   - remover/importar novamente o `.mcpack`/addon.

5. **Testar sem conflito**
   - ativar **só** esse pack no mundo (desativar outros temporariamente).

6. **Reinstalar apenas o pack**
   - baixar/exportar novamente do original, evitando zip com pasta duplicada (`pack/pack/...`).

## Resultado esperado

Após corrigir manifest + path da textura e forçar nova identidade (UUID/version), o item deixa de renderizar em preto/rosa e passa a usar o PNG definido.
