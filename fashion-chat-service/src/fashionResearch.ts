export interface FashionResearchResult {
  query: string;
  sources: FashionSource[];
  summary: string;
}

export interface FashionSource {
  title: string;
  url: string;
  snippet: string;
}

export class FashionResearchService {
  constructor(private readonly fetchImpl: typeof fetch = globalThis.fetch) {}

  async research(question: string): Promise<FashionResearchResult> {
    const query = this.buildQuery(question);
    const sources = await this.searchDuckDuckGo(query);
    return {
      query,
      sources,
      summary: this.buildSummary(sources),
    };
  }

  private buildQuery(question: string): string {
    const cleanQuestion = question.replace(/\s+/g, ' ').trim().slice(0, 180);
    return `moda estilo consultoria tendencias roupa ${cleanQuestion}`;
  }

  private async searchDuckDuckGo(query: string): Promise<FashionSource[]> {
    const url = `https://duckduckgo.com/html/?q=${encodeURIComponent(query)}`;
    try {
      const response = await this.fetchImpl(url, {
        headers: {
          'user-agent': 'MarketingHubFashionChat/0.1',
          accept: 'text/html',
        },
        signal: AbortSignal.timeout(12000),
      });
      if (!response.ok) {
        return this.fallbackSources(query, `HTTP_${response.status}`);
      }
      const html = await response.text();
      const sources = this.parseDuckDuckGoHtml(html);
      return sources.length > 0 ? sources.slice(0, 4) : this.fallbackSources(query, 'NO_RESULTS');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'SEARCH_FAILED';
      return this.fallbackSources(query, message);
    }
  }

  private parseDuckDuckGoHtml(html: string): FashionSource[] {
    const results: FashionSource[] = [];
    const resultRegex = /<a[^>]+class="result__a"[^>]+href="([^"]+)"[^>]*>([\s\S]*?)<\/a>[\s\S]*?<a[^>]+class="result__snippet"[^>]*>([\s\S]*?)<\/a>/gi;
    let match: RegExpExecArray | null;
    while ((match = resultRegex.exec(html)) !== null && results.length < 6) {
      const url = this.decodeDuckDuckGoUrl(this.stripHtml(match[1]));
      const title = this.stripHtml(match[2]);
      const snippet = this.stripHtml(match[3]);
      if (url && title) {
        results.push({ title, url, snippet });
      }
    }
    return results;
  }

  private decodeDuckDuckGoUrl(value: string): string {
    try {
      const decoded = value.replace(/&amp;/g, '&');
      const parsed = new URL(decoded, 'https://duckduckgo.com');
      const uddg = parsed.searchParams.get('uddg');
      return uddg ? decodeURIComponent(uddg) : decoded;
    } catch {
      return value;
    }
  }

  private stripHtml(value: string): string {
    return value
      .replace(/<[^>]+>/g, ' ')
      .replace(/&quot;/g, '"')
      .replace(/&#x27;/g, "'")
      .replace(/&amp;/g, '&')
      .replace(/\s+/g, ' ')
      .trim();
  }

  private buildSummary(sources: FashionSource[]): string {
    if (sources.length === 0) {
      return 'Pesquisa externa sem resultados utilizaveis. Responder com principios seguros de consultoria de moda.';
    }
    return sources.map((source, index) => `${index + 1}. ${source.title}: ${source.snippet}`).join('\n');
  }

  private fallbackSources(query: string, reason: string): FashionSource[] {
    return [
      {
        title: 'Pesquisa web indisponivel',
        url: 'about:blank',
        snippet: `Nao foi possivel concluir a pesquisa para "${query}" (${reason}). Use orientacao geral de moda, sem inventar tendencias especificas.`,
      },
    ];
  }
}
