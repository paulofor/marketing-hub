# Editor de prompt para geração de imagens do Lead Portal

Esta tela permite personalizar o texto usado pelo worker de IA para criar os pacotes de imagens enviados aos leads. Além do texto, você define qual modelo de imagem deve ser chamado e quantas variações serão solicitadas em cada execução (modo batch).

## Onde acessar

1. Abra o AI Hub.
2. Vá em **Campanhas → Prompt de imagens**.
3. A página mostra, à esquerda, todos os fluxos simples publicados e, à direita, o formulário de edição.

## Fluxos elegíveis

- Apenas fluxos simples publicados aparecem na lista.
- O indicador “Prompt customizado” mostra rapidamente se aquele fluxo já está usando um template próprio ou ainda está no padrão do sistema.
- Use o campo de busca para localizar rapidamente pelo nome ou pelo slug.

## Campos disponíveis

| Campo | Descrição |
| --- | --- |
| **Modelo de imagem** | Código do modelo informado ao worker. Seleciona da mesma tabela de modelos usada nos experimentos (ex.: `gpt-image-1`). Se deixar em branco, usamos o padrão configurado globalmente. |
| **Tamanho do lote** | Número de imagens geradas por submissão. Mantemos o processamento em batch (1 a 20 imagens) para reduzir custo por unidade. |
| **Template do prompt** | Texto enviado ao worker. Aceita placeholders para inserir dados do formulário automaticamente. |

### Placeholders suportados

Todos os tokens usam o formato `{{nome_do_campo}}`.

| Placeholder | Fonte |
| --- | --- |
| `{{profissional}}` | Nome informado pelo lead.
| `{{atividade}}` | Tipo de atividade derivado do slug (ex.: personal trainer).
| `{{studio}}` | Nome do estúdio/empresa.
| `{{local}}` | Cidade/bairro preenchido ou detectado.
| `{{contato}}` | Canal e valor de contato resumidos (WhatsApp, e-mail etc.).
| `{{servicos}}` | Lista plana dos serviços principais.
| `{{servicos_lista}}` | Lista dos serviços separados por quebra de linha.
| `{{batch_size}}` | Número de imagens solicitado naquele fluxo.
| `{{dados_json}}` | JSON completo com todas as respostas saneadas.
| `{{respostas.campo}}` | Qualquer campo individual do formulário. Ex.: `{{respostas.whatsapp}}`, `{{respostas.objetivo}}`.

## Restaurar textos padrões

- **Restaurar template padrão**: substitui apenas o texto pelo prompt oficial sugerido.
- **Restaurar modelo e lote padrão**: volta para o modelo e quantidade definidos globalmente.

## Boas práticas

1. **Contextualize o público**: use placeholders para citar o nome, serviços e região.
2. **Inclua instruções visuais**: especifique clima, cores, iluminação e elementos de marca.
3. **Reforce o batch**: explique que são variações quadradas prontas para feed/stories.
4. **Campos opcionais**: quando algum dado não existir, o sistema substitui por um fallback (ex.: “Contato não informado”).

## Fluxo de trabalho recomendado

1. Selecione o fluxo simples desejado.
2. Ajuste modelo, lote e template.
3. Clique em **Salvar alterações**.
4. Valide o resultado acompanhando a fila em **IA e Conteúdo → Gerações IA** ou no painel de pacotes de imagem.

As alterações são versionadas no banco de dados e o fluxo é republicado automaticamente quando estiver aprovado, mantendo o deploy totalmente automatizado.
