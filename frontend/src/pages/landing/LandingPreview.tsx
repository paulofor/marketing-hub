import PageTitle from "../../components/PageTitle";
import { useParams } from "react-router-dom";
import { useLandingVideoSlots } from "../../api/salesVideo/useLandingVideoSlots";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";

export default function LandingPreview() {
  const { id } = useParams();
  const url = `/landings/${id ?? ""}.html`;
  const { data: slots, isLoading } = useLandingVideoSlots(id);
  const videoSlots = slots ?? [];

  return (
    <div>
      <PageTitle>{`Landing ${id ?? ""}`}</PageTitle>
      <iframe title="preview" src={url} style={{ width: "100%", height: "80vh" }} />
      <section className="mt-4">
        <h2 className="h5">Slots de vídeo configurados</h2>
        {!id && <p className="text-muted">Informe um ID válido para visualizar os slots.</p>}
        {id && isLoading && <p>Carregando slots...</p>}
        {id && !isLoading && videoSlots.length === 0 && (
          <p className="text-muted">Nenhum slot cadastrado para esta landing.</p>
        )}
        <div className="row g-3">
          {videoSlots.map((slot) => {
            const assetUrl = slot.assetUrl ? resolveAssetUrl(slot.assetUrl) : undefined;
            const posterUrl = slot.posterAssetUrl ? resolveAssetUrl(slot.posterAssetUrl) : undefined;
            return (
              <div key={slot.id} className="col-md-6">
                <div className="card p-3 h-100">
                  <h3 className="h6">Slot {slot.slotName}</h3>
                  <p className="text-muted mb-2">Asset #{slot.assetId}</p>
                  {assetUrl ? (
                    <video
                      src={assetUrl}
                      poster={posterUrl}
                      controls={slot.controlsEnabled}
                      muted={slot.muted}
                      loop={slot.loopVideo}
                      className="w-100"
                    />
                  ) : (
                    <p className="text-muted">Vídeo ainda não disponível.</p>
                  )}
                  {slot.vttAssetUrl && (
                    <small className="text-muted">Legendas: {resolveAssetUrl(slot.vttAssetUrl)}</small>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
