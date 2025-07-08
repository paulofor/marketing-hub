import { useParams } from "react-router-dom";
import { useAsset } from "../../api/media/useAsset";
import PageTitle from "../../components/PageTitle";

export default function MediaDetailPage() {
  const { id } = useParams();
  const assetId = Number(id);
  const { data } = useAsset(assetId);

  if (!data) return <p>Loading...</p>;

  return (
    <div>
      <PageTitle>Asset {data.id}</PageTitle>
      <p>Status: {data.status}</p>
      {data.url && (
        <div>
          {data.type === "AUDIO" ? (
            <audio controls src={data.url} />
          ) : (
            <video controls src={data.url} />
          )}
        </div>
      )}
    </div>
  );
}
