import { useState } from "react";
import { useInstagramAccounts } from "../api/useInstagramAccounts";
import {
  useCreateInstagramAccount,
  useUpdateInstagramAccount,
  useDeleteInstagramAccount,
} from "../api/instagramAccountMutations";

export default function InstagramAccountsPage() {
  const { data, isLoading, error } = useInstagramAccounts();
  const createMutation = useCreateInstagramAccount();
  const updateMutation = useUpdateInstagramAccount();
  const deleteMutation = useDeleteInstagramAccount();

  const [form, setForm] = useState({ id: "", name: "", currency: "" });
  const [editing, setEditing] = useState<boolean | string>(false);

  if (isLoading) return <p>Loading...</p>;
  if (error) return <p>Error loading accounts</p>;

  const submit = () => {
    if (editing) {
      updateMutation.mutate(form);
    } else {
      createMutation.mutate(form);
    }
    setForm({ id: "", name: "", currency: "" });
    setEditing(false);
  };

  return (
    <div>
      <table className="table table-striped">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Currency</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {data?.map(({ id, name, currency }) => (
            <tr key={id}>
              <td>{id}</td>
              <td>{name}</td>
              <td>{currency}</td>
              <td>
                <a
                  className="btn btn-sm btn-outline-secondary me-2"
                  href={`/accounts/instagram/${id}/posts`}
                >
                  Posts
                </a>
                <button
                  className="btn btn-sm btn-outline-primary me-2"
                  onClick={() => {
                    setForm({ id, name, currency });
                    setEditing(id);
                  }}
                >
                  Edit
                </button>
                <button
                  className="btn btn-sm btn-outline-danger"
                  onClick={() => deleteMutation.mutate(id)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row g-2">
        <div className="col-md-2">
          <input
            className="form-control"
            placeholder="id"
            value={form.id}
            onChange={(e) => setForm({ ...form, id: e.target.value })}
          />
        </div>
        <div className="col-md-4">
          <input
            className="form-control"
            placeholder="name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </div>
        <div className="col-md-2">
          <input
            className="form-control"
            placeholder="currency"
            value={form.currency}
            onChange={(e) => setForm({ ...form, currency: e.target.value })}
          />
        </div>
        <div className="col-md-2">
          <button className="btn btn-primary w-100" onClick={submit}>
            {editing ? "Update" : "Create"}
          </button>
        </div>
      </div>
    </div>
  );
}
