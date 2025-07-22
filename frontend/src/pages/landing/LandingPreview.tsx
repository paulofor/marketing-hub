import PageTitle from "../../components/PageTitle";
import { useParams } from "react-router-dom";

export default function LandingPreview() {
  const { id } = useParams();
  return (
    <div>
      <PageTitle>Landing {id}</PageTitle>
      <iframe title="preview" src={`/landings/${id}.html`} style={{width:'100%', height:'80vh'}} />
    </div>
  );
}
