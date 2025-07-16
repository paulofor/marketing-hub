import { useState } from "react";
import { useParams } from "react-router-dom";
import {
  useCreateInstagramPost,
  useDeleteInstagramPost,
  useUpdateInstagramPost,
} from "../../api/post/instagramPostMutations";
import { useInstagramPosts } from "../../api/post/useInstagramPosts";
import PageTitle from "../../components/PageTitle";

export default function InstagramPostsPage() {
  const { id = "" } = useParams<{ id: string }>();
  const { data } = useInstagramPosts(id);
  const posts = Array.isArray(data) ? data : [];
  const create = useCreateInstagramPost(id);
  const update = useUpdateInstagramPost(id);
  const remove = useDeleteInstagramPost(id);
  const [form, setForm] = useState({ id: 0, caption: "", mediaUrl: "" });
  const [editing, setEditing] = useState<number | null>(null);

  const submit = () => {
    if (editing) {
      update.mutate(form);
    } else {
      create.mutate(form);
    }
    setForm({ id: 0, caption: "", mediaUrl: "" });
    setEditing(null);
  };

  return (
    <div>
      <PageTitle>Publicações do Instagram</PageTitle>
      <div className="table-responsive">
        <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Legenda</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {posts.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.caption}</td>
              <td>
                <button
                  className="btn btn-sm btn-outline-primary me-2"
                  onClick={() => {
                    setForm(p);
                    setEditing(p.id);
                  }}
                >
                  Editar
                </button>
                <button
                  className="btn btn-sm btn-outline-danger"
                  onClick={() => remove.mutate(p.id)}
                >
                  Excluir
                </button>
              </td>
            </tr>
          ))}
        </tbody>
        </table>
      </div>
      <div className="row g-2">
        <div className="col-md-5">
          <input
            className="form-control"
            placeholder="legenda"
            value={form.caption}
            onChange={(e) => setForm({ ...form, caption: e.target.value })}
          />
        </div>
        <div className="col-md-5">
          <input
            className="form-control"
            placeholder="url da mídia"
            value={form.mediaUrl}
            onChange={(e) => setForm({ ...form, mediaUrl: e.target.value })}
          />
        </div>
        <div className="col-md-2">
          <button className="btn btn-primary w-100" onClick={submit}>
            {editing ? "Atualizar" : "Criar"}
          </button>
        </div>
      </div>
    </div>
  );
}
