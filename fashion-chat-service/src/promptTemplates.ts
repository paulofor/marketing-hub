import fs from 'node:fs/promises';
import path from 'node:path';

export interface FashionPromptTemplates {
  system: string;
  visualStyle: string;
  responseContract: string;
}

export class FashionPromptTemplateLoader {
  constructor(private readonly promptDir = process.env.FASHION_CHAT_PROMPT_DIR?.trim() || path.join(process.cwd(), 'prompts', 'fashion-chat')) {}

  async load(): Promise<FashionPromptTemplates> {
    const [system, visualStyle, responseContract] = await Promise.all([
      this.readTemplate('system.md'),
      this.readTemplate('visual-style.md'),
      this.readTemplate('response-contract.md'),
    ]);
    return { system, visualStyle, responseContract };
  }

  private async readTemplate(fileName: string): Promise<string> {
    const fullPath = path.join(this.promptDir, fileName);
    try {
      return (await fs.readFile(fullPath, 'utf-8')).trim();
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      throw new Error(`FASHION_CHAT_PROMPT_NOT_FOUND:${fullPath}:${message}`);
    }
  }
}
