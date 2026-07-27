package com.marketinghub.product.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Responsabilidade: renderizar a definição pública de produto em uma página HTML legível. */
class ProductMarketingDefinitionHtmlRenderer {
  private static final Pattern STRONG_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");

  /** Converte o Markdown comercial gerado pelo backend em HTML seguro para visualização pública. */
  String render(String markdown) {
    String body = renderMarkdownBody(markdown);
    String template =
        """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Definição de Produto para Mercado</title>
                  <style>
                    :root {
                      color-scheme: light;
                      --bg: #f6f4f1;
                      --paper: #ffffff;
                      --ink: #252525;
                      --muted: #66615c;
                      --line: #ded8d1;
                      --accent: #8b2f52;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      background: var(--bg);
                      color: var(--ink);
                      font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                      line-height: 1.65;
                    }
                    main {
                      width: min(940px, calc(100% - 32px));
                      margin: 40px auto;
                      padding: clamp(24px, 5vw, 56px);
                      background: var(--paper);
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      box-shadow: 0 18px 60px rgba(37, 37, 37, 0.08);
                    }
                    h1 {
                      margin: 0 0 20px;
                      font-size: clamp(2rem, 4vw, 3.2rem);
                      line-height: 1.08;
                      letter-spacing: 0;
                    }
                    h2 {
                      margin: 36px 0 14px;
                      padding-top: 24px;
                      border-top: 1px solid var(--line);
                      font-size: clamp(1.25rem, 2vw, 1.65rem);
                      letter-spacing: 0;
                    }
                    p { margin: 0 0 16px; }
                    blockquote {
                      margin: 0 0 28px;
                      padding: 14px 18px;
                      border-left: 4px solid var(--accent);
                      background: #fbf7f9;
                      color: var(--muted);
                    }
                    ul {
                      margin: 0 0 18px;
                      padding-left: 22px;
                    }
                    li { margin: 8px 0; }
                    strong { color: var(--accent); font-weight: 700; }
                    @media (max-width: 640px) {
                      main {
                        width: 100%;
                        min-height: 100vh;
                        margin: 0;
                        border: 0;
                        border-radius: 0;
                      }
                    }
                  </style>
                </head>
                <body>
                  <main>
                {{body}}
                  </main>
                </body>
                </html>
                """;
    return template.replace("{{body}}", body);
  }

  /** Renderiza os blocos Markdown suportados pelo documento comercial público. */
  private String renderMarkdownBody(String markdown) {
    StringBuilder html = new StringBuilder();
    boolean listOpen = false;
    for (String rawLine : markdown.split("\\R", -1)) {
      String line = rawLine.strip();
      if (line.isEmpty()) {
        if (listOpen) {
          html.append("</ul>\n");
          listOpen = false;
        }
        continue;
      }
      if (line.startsWith("- ")) {
        if (!listOpen) {
          html.append("<ul>\n");
          listOpen = true;
        }
        html.append("<li>").append(renderInline(line.substring(2))).append("</li>\n");
        continue;
      }
      if (listOpen) {
        html.append("</ul>\n");
        listOpen = false;
      }
      appendBlock(html, line);
    }
    if (listOpen) {
      html.append("</ul>\n");
    }
    return html.toString();
  }

  /** Adiciona o bloco HTML equivalente à linha Markdown informada. */
  private void appendBlock(StringBuilder html, String line) {
    if (line.startsWith("# ")) {
      html.append("<h1>").append(renderInline(line.substring(2))).append("</h1>\n");
    } else if (line.startsWith("## ")) {
      html.append("<h2>").append(renderInline(line.substring(3))).append("</h2>\n");
    } else if (line.startsWith("> ")) {
      html.append("<blockquote>").append(renderInline(line.substring(2))).append("</blockquote>\n");
    } else {
      html.append("<p>").append(renderInline(line)).append("</p>\n");
    }
  }

  /** Renderiza marcações inline controladas depois de escapar o conteúdo textual. */
  private String renderInline(String value) {
    String escaped = escapeHtml(value);
    Matcher matcher = STRONG_PATTERN.matcher(escaped);
    return matcher.replaceAll("<strong>$1</strong>");
  }

  /** Escapa caracteres especiais para impedir interpretação de HTML vindo dos dados do produto. */
  private String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
