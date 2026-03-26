import { FormEvent, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { useProduct } from "../../api/product/useProduct";
import { useSalesVideoProfiles } from "../../api/salesVideo/useSalesVideoProfiles";
import { useCreateSalesVideoProfile } from "../../api/salesVideo/useCreateSalesVideoProfile";
import { SalesVideoKind } from "../../api/salesVideo/types";
import { TenantContextBanner } from "../../components/TenantContextBanner";

const VIDEO_KIND_OPTIONS: SalesVideoKind[] = ["HERO", "OBJECTION", "PROOF"];

export default function ProductSalesVideoPage() {
  const { productId } = useParams();
  const { data: product, isLoading: productLoading } = useProduct(productId);
  const { data: profiles, isLoading: profilesLoading } = useSalesVideoProfiles(productId);
  const createProfile = useCreateSalesVideoProfile(productId);
  const [formState, setFormState] = useState({
    videoKind: "HERO" as SalesVideoKind,
    title: "",
    personaName: "",
    personaStyle: "",
    voiceStyle: "",
    language: "pt-BR",
    targetDurationSeconds: "",
    landingPageId: "",
  });

  const profileList = useMemo(() => profiles ?? [], [profiles]);

  if (!productId) {
    return (
      <div>
        <PageTitle>Vídeos do Produto</PageTitle>
        <p>Informe um produto válido para visualizar os perfis de vídeo.</p>
      </div>
    );
  }

  if (productLoading || profilesLoading) {
    return (
      <div>
        <PageTitle>Vídeos do Produto #{productId}</PageTitle>
        <p>Carregando...</p>
      </div>
    );
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState.title.trim()) {
      toast.error("Informe um título para o perfil");
      return;
    }
    const duration = formState.targetDurationSeconds.trim()
      ? Number(formState.targetDurationSeconds)
      : undefined;
    if (duration !== undefined && Number.isNaN(duration)) {
      toast.error("Duração alvo inválida");
      return;
    }
    const landingPageId = formState.landingPageId.trim()
      ? Number(formState.landingPageId)
      : undefined;
    if (landingPageId !== undefined && Number.isNaN(landingPageId)) {
      toast.error("ID da landing precisa ser numérico");
      return;
    }

    try {
      await createProfile.mutateAsync({
        videoKind: formState.videoKind,
        title: formState.title.trim(),
        personaName: formState.personaName.trim() || undefined,
        personaStyle: formState.personaStyle.trim() || undefined,
        voiceStyle: formState.voiceStyle.trim() || undefined,
        language: formState.language.trim() || undefined,
        targetDurationSeconds: duration,
        landingPageId,
      });
      toast.success("Perfil de vídeo criado");
      setFormState((prev) => ({
        ...prev,
        title: "",
        personaName: "",
        personaStyle: "",
        voiceStyle: "",
        targetDurationSeconds: "",
        landingPageId: prev.landingPageId,
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao criar perfil";
      toast.error(message);
    }
  };

  return (
    <div>
      <PageTitle>Vídeos do Produto #{productId}</PageTitle>
      <TenantContextBanner className="mb-3" />
      <div className="mb-3 d-flex gap-2 align-items-center">
        <Link to="/products" className="btn btn-link p-0">
          &larr; Voltar para produtos
        </Link>
        {product && (
          <span className="text-muted">
            Nicho: <strong>{product.niche}</strong> — Avatar: <strong>{product.avatar}</strong>
          </span>
        )}
      </div>

      <section className="mb-4">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <h2 className="h5 mb-0">Perfis cadastrados</h2>
          <small className="text-muted">
            Acompanhe status, script e últimos jobs diretamente nesta lista.
          </small>
        </div>
        <div className="table-responsive">
          <table className="table table-striped">
            <thead>
              <tr>
                <th>ID</th>
                <th>Título</th>
                <th>Tipo</th>
                <th>Status</th>
                <th>Script</th>
                <th>Último job</th>
                <th>Landing</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {profileList.length === 0 && (
                <tr>
                  <td colSpan={8} className="text-center text-muted">
                    Nenhum perfil cadastrado.
                  </td>
                </tr>
              )}
              {profileList.map((profile) => (
                <tr key={profile.id}>
                  <td>{profile.id}</td>
                  <td>{profile.title}</td>
                  <td>{profile.videoKind}</td>
                  <td>{profile.status}</td>
                  <td>{profile.latestScript?.status ?? "—"}</td>
                  <td>
                    {profile.lastJob
                      ? `${profile.lastJob.jobType} · ${profile.lastJob.status}`
                      : "—"}
                  </td>
                  <td>{profile.landingPageId ?? "—"}</td>
                  <td>
                    <Link
                      to={`/sales-videos/profiles/${profile.id}`}
                      className="btn btn-sm btn-outline-primary"
                    >
                      Ver perfil
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section>
        <h2 className="h5 mb-3">Novo perfil de vídeo</h2>
        <form className="card p-3" onSubmit={handleSubmit}>
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label">Tipo do vídeo</label>
              <select
                className="form-select"
                value={formState.videoKind}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, videoKind: event.target.value as SalesVideoKind }))
                }
              >
                {VIDEO_KIND_OPTIONS.map((kind) => (
                  <option key={kind} value={kind}>
                    {kind}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-8">
              <label className="form-label">Título interno</label>
              <input
                className="form-control"
                value={formState.title}
                onChange={(event) => setFormState((prev) => ({ ...prev, title: event.target.value }))}
                placeholder="Ex.: Vídeo hero para a oferta principal"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Persona</label>
              <input
                className="form-control"
                value={formState.personaName}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, personaName: event.target.value }))
                }
                placeholder="Nome da persona"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Estilo da persona</label>
              <input
                className="form-control"
                value={formState.personaStyle}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, personaStyle: event.target.value }))
                }
                placeholder="Ex.: consultiva, descontraída"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Estilo de voz</label>
              <input
                className="form-control"
                value={formState.voiceStyle}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, voiceStyle: event.target.value }))
                }
                placeholder="Ex.: calorosa, urgente"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Idioma</label>
              <input
                className="form-control"
                value={formState.language}
                onChange={(event) => setFormState((prev) => ({ ...prev, language: event.target.value }))}
                placeholder="pt-BR"
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Duração alvo (segundos)</label>
              <input
                className="form-control"
                type="number"
                min="0"
                value={formState.targetDurationSeconds}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, targetDurationSeconds: event.target.value }))
                }
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Landing page (ID opcional)</label>
              <input
                className="form-control"
                type="number"
                value={formState.landingPageId}
                onChange={(event) =>
                  setFormState((prev) => ({ ...prev, landingPageId: event.target.value }))
                }
                placeholder="123"
              />
            </div>
          </div>
          <div className="mt-3">
            <button className="btn btn-primary" type="submit" disabled={createProfile.isPending}>
              {createProfile.isPending ? "Salvando..." : "Criar perfil"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
