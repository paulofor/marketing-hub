import { FormEvent, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  ExternalLink,
  Pencil,
  Plus,
  Trash2,
} from "lucide-react";
import { useProduct } from "../../api/product/useProduct";
import {
  ProductScientificArticle,
  SaveProductScientificArticle,
  useCreateProductScientificArticle,
  useDeleteProductScientificArticle,
  useProductScientificArticles,
  useUpdateProductScientificArticle,
} from "../../api/product/useProductScientificArticles";
import PageTitle from "../../components/PageTitle";

const emptyForm: SaveProductScientificArticle = {
  link: "",
  originalTitle: "",
  portugueseTitle: "",
  summary: "",
  mechanismApplication: "",
};

function toForm(
  article: ProductScientificArticle,
): SaveProductScientificArticle {
  return {
    link: article.link,
    originalTitle: article.originalTitle,
    portugueseTitle: article.portugueseTitle,
    summary: article.summary,
    mechanismApplication: article.mechanismApplication,
  };
}

export default function ProductScientificArticlesPage() {
  const { productId } = useParams();
  const productQuery = useProduct(productId);
  const articlesQuery = useProductScientificArticles(productId);
  const createArticle = useCreateProductScientificArticle(productId);
  const updateArticle = useUpdateProductScientificArticle(productId);
  const deleteArticle = useDeleteProductScientificArticle(productId);
  const [editingArticleId, setEditingArticleId] = useState<number | null>(null);
  const [form, setForm] = useState<SaveProductScientificArticle>(emptyForm);

  const productName = useMemo(() => {
    const product = productQuery.data;
    return product?.name || product?.slug || `Produto ${productId}`;
  }, [productId, productQuery.data]);

  const articles = articlesQuery.data ?? [];
  const isSaving = createArticle.isPending || updateArticle.isPending;

  function updateField<K extends keyof SaveProductScientificArticle>(
    field: K,
    value: SaveProductScientificArticle[K],
  ) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function resetForm() {
    setEditingArticleId(null);
    setForm(emptyForm);
  }

  function startEditing(article: ProductScientificArticle) {
    setEditingArticleId(article.id);
    setForm(toForm(article));
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (editingArticleId) {
      updateArticle.mutate(
        { articleId: editingArticleId, payload: form },
        { onSuccess: resetForm },
      );
      return;
    }
    createArticle.mutate(form, { onSuccess: resetForm });
  }

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Artigos científicos do produto</PageTitle>
          <p className="text-muted mb-0">
            {productName} · evidências usadas para sustentar o mecanismo,
            orientar IA e proteger a promessa comercial.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
      </div>

      <div className="product-scientific-articles">
        <section className="product-scientific-articles__form">
          <div className="d-flex align-items-center justify-content-between gap-2 mb-3">
            <div>
              <h2 className="h6 mb-1">
                {editingArticleId ? "Editar artigo" : "Cadastrar artigo"}
              </h2>
              <p className="text-muted small mb-0">
                Registre evidências que explicam a plausibilidade do mecanismo.
              </p>
            </div>
            <BookOpen size={20} aria-hidden="true" />
          </div>
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="scientific-article-link">
                Link
              </label>
              <input
                id="scientific-article-link"
                className="form-control"
                value={form.link}
                maxLength={1024}
                onChange={(event) => updateField("link", event.target.value)}
                required
              />
            </div>
            <div className="mb-3">
              <label
                className="form-label"
                htmlFor="scientific-article-original-title"
              >
                Título original
              </label>
              <input
                id="scientific-article-original-title"
                className="form-control"
                value={form.originalTitle}
                maxLength={512}
                onChange={(event) =>
                  updateField("originalTitle", event.target.value)
                }
                required
              />
            </div>
            <div className="mb-3">
              <label
                className="form-label"
                htmlFor="scientific-article-portuguese-title"
              >
                Título em português
              </label>
              <input
                id="scientific-article-portuguese-title"
                className="form-control"
                value={form.portugueseTitle}
                maxLength={512}
                onChange={(event) =>
                  updateField("portugueseTitle", event.target.value)
                }
                required
              />
            </div>
            <div className="mb-3">
              <label
                className="form-label"
                htmlFor="scientific-article-summary"
              >
                Resumo
              </label>
              <textarea
                id="scientific-article-summary"
                className="form-control"
                rows={5}
                value={form.summary}
                onChange={(event) => updateField("summary", event.target.value)}
                required
              />
            </div>
            <div className="mb-3">
              <label
                className="form-label"
                htmlFor="scientific-article-mechanism"
              >
                Aplicação no mecanismo
              </label>
              <textarea
                id="scientific-article-mechanism"
                className="form-control"
                rows={6}
                value={form.mechanismApplication}
                onChange={(event) =>
                  updateField("mechanismApplication", event.target.value)
                }
                required
              />
            </div>
            <div className="d-flex flex-wrap gap-2">
              <button
                className="btn btn-primary"
                type="submit"
                disabled={isSaving}
              >
                <Plus size={16} aria-hidden="true" />
                {editingArticleId ? "Salvar artigo" : "Cadastrar artigo"}
              </button>
              {editingArticleId && (
                <button
                  className="btn btn-outline-secondary"
                  type="button"
                  onClick={resetForm}
                >
                  Cancelar edição
                </button>
              )}
            </div>
          </form>
        </section>

        <section className="product-scientific-articles__list">
          {articlesQuery.isLoading && (
            <p className="text-muted mb-0">Carregando artigos...</p>
          )}
          {articlesQuery.isError && (
            <div className="alert alert-danger">
              Não foi possível carregar os artigos científicos do produto.
            </div>
          )}
          {!articlesQuery.isLoading && articles.length === 0 && (
            <div className="product-scientific-articles__empty">
              Nenhum artigo científico cadastrado para este produto.
            </div>
          )}
          {articles.map((article) => (
            <article
              className="product-scientific-articles__item"
              key={article.id}
            >
              <div className="product-scientific-articles__item-header">
                <div>
                  <h2 className="h5 mb-1">{article.portugueseTitle}</h2>
                  <p className="text-muted mb-0">{article.originalTitle}</p>
                </div>
                <a
                  className="btn btn-outline-secondary btn-sm"
                  href={article.link}
                  target="_blank"
                  rel="noreferrer"
                >
                  <ExternalLink size={15} aria-hidden="true" />
                  Abrir artigo
                </a>
              </div>
              <h3 className="h6 mt-3">Resumo</h3>
              <p>{article.summary}</p>
              <h3 className="h6 mt-3">Aplicação no mecanismo</h3>
              <p>{article.mechanismApplication}</p>
              <div className="d-flex flex-wrap gap-2 mt-3">
                <button
                  className="btn btn-outline-primary btn-sm"
                  type="button"
                  onClick={() => startEditing(article)}
                >
                  <Pencil size={15} aria-hidden="true" />
                  Editar
                </button>
                <button
                  className="btn btn-outline-danger btn-sm"
                  type="button"
                  disabled={deleteArticle.isPending}
                  onClick={() => deleteArticle.mutate(article.id)}
                >
                  <Trash2 size={15} aria-hidden="true" />
                  Remover
                </button>
              </div>
            </article>
          ))}
        </section>
      </div>
    </div>
  );
}
