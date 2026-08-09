# Homologação da memória premium dos agentes v1

## Critério de sucesso

Os cinco agentes operacionais recuperam apenas memórias do próprio agente e escopo, registram
candidatos auditáveis e continuam operando quando não existe memória. Nenhum candidato é promovido
automaticamente a verdade.

## Matriz ponta a ponta

| Área | Caminho feliz | Validação/falha esperada |
|---|---|---|
| Recuperação | MCP consulta lista limitada e ordenada | agente/escopo alheio e estado contradito não aparecem |
| Aprendizagem | MCP grava candidato com evidência e execução | payload inválido, excessivo ou duplicado é bloqueado/deduplicado |
| Feedback | resultado oficial confirma ou contradiz | agente não promove a própria memória |
| Desempenho | no máximo 12 itens e 24 mil caracteres por consulta | histórico completo nunca é injetado no prompt |
| Observabilidade | origem, execução, confiança e uso persistidos | indisponibilidade retorna erro explícito sem fabricar contexto |
| Evidência pesada | S3 privado por referência/checksum | S3 fora do ar não quebra leitura textual |
| Multimodal | Aprovador referencia inspeção de imagem/vídeo e landing | bytes/HTML bruto não entram na memória textual |
| Compatibilidade | API/MCP independe de browser | Chrome desktop/mobile permanece exigido apenas no Aprovador |

Uma rodada integral sem defeitos conclui a homologação. Se houver correção, a contagem reinicia
e exige duas rodadas integrais consecutivas sem falhas.
