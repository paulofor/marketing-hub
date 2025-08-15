import { useState } from "react";
import { useCreateSuccessProduct } from "../../api/successProduct/useCreateSuccessProduct";
import PageTitle from "../../components/PageTitle";
import { SuccessProductPlatform } from "../../api/successProduct/useSuccessProducts";

export default function NewSuccessProductPage() {
  const create = useCreateSuccessProduct();
  const [description, setDescription] = useState("");
  const [platform, setPlatform] = useState<SuccessProductPlatform>("COFRE");

  const submit = async () => {
    try {
      await create.mutateAsync({ description, platform });
      setDescription("");
      setPlatform("COFRE");
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
      <select
        className="form-select mb-2"
        value={platform}
        onChange={(e) => setPlatform(e.target.value as SuccessProductPlatform)}
      >
        <option value="COFRE">Cofre</option>
        <option value="HOTMART">Hotmart</option>
        <option value="CLICKBANK">Clickbank</option>
      </select>
      <button className="btn btn-primary" onClick={submit}>
        Salvar
      </button>
    </div>
  );
}
