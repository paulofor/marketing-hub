import { useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useAsset } from '../../api/media/useAsset';
import PageTitle from '../../components/PageTitle';
import { resolveAssetUrl } from '../../utils/resolveAssetUrl';

export default function MediaDetailPage() {
  const { id } = useParams();
  const assetId = Number(id);
  const { data } = useAsset(assetId);

  const assetUrl = useMemo(() => {
    if (!data) {
      return '';
    }
    if (data.publicUrl) {
      return data.publicUrl;
    }
    if (data.url) {
      return resolveAssetUrl(data.url);
    }
    return '';
  }, [data]);

  if (!data) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>{`Arquivo ${data.id}`}</PageTitle>
      <p>Status: {data.status}</p>
      <p>Nome do arquivo: {data.url ?? '—'}</p>
      {assetUrl && (
        <div className="mt-3">
          {data.type === 'AUDIO' ? (
            <audio controls src={assetUrl} />
          ) : data.type === 'IMAGE' ? (
            <img src={assetUrl} alt={`Visualização do arquivo ${data.id}`} className="img-fluid" />
          ) : (
            <video controls src={assetUrl} />
          )}
        </div>
      )}
      {!assetUrl && (
        <p className="text-muted">URL pública indisponível para este ativo.</p>
      )}
    </div>
  );
}
