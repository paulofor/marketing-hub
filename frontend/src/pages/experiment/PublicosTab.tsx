import { useForm } from "react-hook-form";

interface AudienceForm {
  geo: string;
  minAge: number;
  maxAge: number;
  gender: string;
  locale: string;
  interests: string;
  customAudiences: string;
  exclusions: string;
  lookalikeSource: string;
  lookalikeCountry: string;
  lookalikePercent: number;
  placements: string;
}

export default function PublicosTab() {
  const { register, handleSubmit } = useForm<AudienceForm>();
  const onSubmit = (data: AudienceForm) => {
    console.log("Audience", data);
  };

  return (
    <form className="p-3">
      <div className="mb-2">
        <label className="form-label">Geo</label>
        <input className="form-control" {...register("geo")} />
      </div>
      <div className="mb-2 d-flex">
        <div className="me-2">
          <label className="form-label">Idade mínima</label>
          <input type="number" className="form-control" {...register("minAge")} />
        </div>
        <div>
          <label className="form-label">Idade máxima</label>
          <input type="number" className="form-control" {...register("maxAge")} />
        </div>
      </div>
      <div className="mb-2">
        <label className="form-label">Gênero</label>
        <select className="form-select" {...register("gender")}>
          <option value="">Todos</option>
          <option value="male">Masculino</option>
          <option value="female">Feminino</option>
        </select>
      </div>
      <div className="mb-2">
        <label className="form-label">Idioma/Locale</label>
        <input className="form-control" {...register("locale")} />
      </div>
      <div className="mb-2">
        <label className="form-label">Interesses / comportamentos</label>
        <textarea className="form-control" {...register("interests")} />
      </div>
      <div className="mb-2">
        <label className="form-label">Públicos personalizados</label>
        <textarea className="form-control" {...register("customAudiences")} />
      </div>
      <div className="mb-2">
        <label className="form-label">Exclusões</label>
        <textarea className="form-control" {...register("exclusions")} />
      </div>
      <div className="mb-2">
        <label className="form-label">Lookalike (fonte + país + %)</label>
        <input
          className="form-control mb-2"
          placeholder="Fonte"
          {...register("lookalikeSource")}
        />
        <input
          className="form-control mb-2"
          placeholder="País"
          {...register("lookalikeCountry")}
        />
        <input
          type="number"
          className="form-control"
          placeholder="%"
          {...register("lookalikePercent")}
        />
      </div>
      <div className="mb-2">
        <label className="form-label">Placements</label>
        <textarea className="form-control" {...register("placements")} />
      </div>
      <button
        className="btn btn-primary"
        onClick={handleSubmit(
          onSubmit,
          (errors) => {
            console.log("Validation errors", errors);
          },
        )}
      >
        Salvar
      </button>
    </form>
  );
}
