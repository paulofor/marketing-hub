import { useState } from "react";
import { useForm } from "react-hook-form";

interface AdSetForm {
  geo: string;
  minAge: number;
  maxAge: number;
  gender: string;
  locale: string;
  interests: string;
  customAudiences: string;
  lookalikeSource: string;
  lookalikeCountry: string;
  lookalikePercent: string;
  placements: string;
}

export default function PublicosTab() {
  const { register, handleSubmit, reset } = useForm<AdSetForm>();
  const [adSets, setAdSets] = useState<AdSetForm[]>([]);
  const onSubmit = (data: AdSetForm) => {
    setAdSets((a) => [...a, data]);
    reset();
  };
  return (
    <div className="mt-3">
      <form>
        <input
          className="form-control mb-2"
          placeholder="Geo (país/estado/cidade)"
          {...register("geo")}
        />
        <div className="d-flex mb-2">
          <input
            type="number"
            className="form-control me-2"
            placeholder="Idade mínima"
            {...register("minAge")}
          />
          <input
            type="number"
            className="form-control"
            placeholder="Idade máxima"
            {...register("maxAge")}
          />
        </div>
        <input
          className="form-control mb-2"
          placeholder="Gênero"
          {...register("gender")}
        />
        <input
          className="form-control mb-2"
          placeholder="Idioma/Locale"
          {...register("locale")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Interesses/comportamentos"
          {...register("interests")}
        />
        <textarea
          className="form-control mb-2"
          placeholder="Públicos personalizados e exclusões"
          {...register("customAudiences")}
        />
        <div className="d-flex mb-2">
          <input
            className="form-control me-2"
            placeholder="Fonte Lookalike"
            {...register("lookalikeSource")}
          />
          <input
            className="form-control me-2"
            placeholder="País"
            {...register("lookalikeCountry")}
          />
          <input
            className="form-control"
            placeholder="%"
            {...register("lookalikePercent")}
          />
        </div>
        <input
          className="form-control mb-2"
          placeholder="Placements"
          {...register("placements")}
        />
        <button
          type="button"
          className="btn btn-primary"
          onClick={handleSubmit(onSubmit, (errors) => {
            console.log("Validation errors", errors);
          })}
        >
          Salvar Público
        </button>
      </form>
      {adSets.length > 0 && (
        <div className="table-responsive mt-3">
          <table className="table">
            <thead>
              <tr>
                <th>Geo</th>
                <th>Idade</th>
                <th>Gênero</th>
                <th>Locale</th>
                <th>Interesses</th>
                <th>Custom</th>
                <th>Lookalike</th>
                <th>Placements</th>
              </tr>
            </thead>
            <tbody>
              {adSets.map((a, idx) => (
                <tr key={idx}>
                  <td>{a.geo}</td>
                  <td>
                    {a.minAge}-{a.maxAge}
                  </td>
                  <td>{a.gender}</td>
                  <td>{a.locale}</td>
                  <td>{a.interests}</td>
                  <td>{a.customAudiences}</td>
                  <td>
                    {a.lookalikeSource} {a.lookalikeCountry} {a.lookalikePercent}%
                  </td>
                  <td>{a.placements}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
