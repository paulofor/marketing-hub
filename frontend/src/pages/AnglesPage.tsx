import { useState } from "react";
import { useAngles } from "../api/angle/useAngles";
import { useCreateAngle } from "../api/angle/useCreateAngle";
import PageTitle from "../components/PageTitle";

export default function AnglesPage() {
  const { data } = useAngles();
  const angles = Array.isArray(data) ? data : [];
  const create = useCreateAngle();
  const [name, setName] = useState("");
  const submit = async () => {
    await create.mutateAsync({ name });
    setName("");
  };
  return (
    <div>
      <PageTitle>Angles</PageTitle>
      <input
        className="form-control mb-2"
        placeholder="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <button className="btn btn-primary mb-3" onClick={submit}>
        Novo Angle
      </button>
      <ul>
        {angles.map((a) => (
          <li key={a.id}>{a.name}</li>
        ))}
      </ul>
    </div>
  );
}
