import { useParams } from 'react-router-dom';
import { useAsset } from '../../api/media/useAsset';

export default function MediaDetailPage() {
  const { id } = useParams();
  const assetId = Number(id);
  const { data } = useAsset(assetId);

  if (!data) return <p>Loading...</p>;

  return (
    <div>
      <h2>Asset {data.id}</h2>
      <p>Status: {data.status}</p>
      {data.url && (
        <div>
          {data.type === 'AUDIO' ? (
            <audio controls src={data.url} />
          ) : (
            <video controls src={data.url} />
          )}
        </div>
      )}
    </div>
  );
}
