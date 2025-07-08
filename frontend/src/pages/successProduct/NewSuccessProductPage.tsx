import { useState } from "react";
import { useCreateSuccessProduct } from "../../api/successProduct/useCreateSuccessProduct";
import PageTitle from "../../components/PageTitle";

export default function NewSuccessProductPage() {
  const create = useCreateSuccessProduct();
  const [description, setDescription] = useState("");

  const submit = () => {
    create.mutate({ description });
  };

  return (
    <div>
      <PageTitle>New Success Product</PageTitle>
      <textarea
        className="form-control mb-2"
        placeholder="Description"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <button className="btn btn-primary" onClick={submit}>
        Save
      </button>
    </div>
  );
}
