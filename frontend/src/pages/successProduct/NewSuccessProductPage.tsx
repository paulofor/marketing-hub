import { useState } from "react";
import { useCreateSuccessProduct } from "../../api/successProduct/useCreateSuccessProduct";
import PageTitle from "../../components/PageTitle";

export default function NewSuccessProductPage() {
  const create = useCreateSuccessProduct();
  const [description, setDescription] = useState("");

  const submit = async () => {
    try {
      await create.mutateAsync({ description });
      setDescription("");
      alert("Produto de Sucesso salvo!");
    } catch (err) {
      alert("Erro ao salvar Produto de Sucesso");
    }
  };

  return (
    <div>
      <PageTitle>Novo Produto de Sucesso</PageTitle>
      <textarea
        className="form-control mb-2"
        placeholder="Descrição"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        rows={5}
      />
      <button className="btn btn-primary" onClick={submit}>
        Salvar
      </button>
    </div>
  );
}
