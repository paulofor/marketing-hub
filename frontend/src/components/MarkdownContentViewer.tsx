import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

type MarkdownContentViewerProps = {
  content?: string | null;
};

export default function MarkdownContentViewer({ content }: MarkdownContentViewerProps) {
  if (!content) {
    return <p className="text-muted mb-0">Sem conteúdo markdown.</p>;
  }

  return (
    <div className="border rounded-3 p-3 bg-light-subtle">
      <div className="markdown-content-viewer">
        <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
      </div>
    </div>
  );
}
