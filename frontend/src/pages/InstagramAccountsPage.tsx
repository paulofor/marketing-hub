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
      <table>
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
                <button
                  onClick={() => {
                    setForm({ id, name, currency });
                    setEditing(id);
                  }}
                >
                  Edit
                </button>
                <button onClick={() => deleteMutation.mutate(id)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div>
        <input
          placeholder="id"
          value={form.id}
          onChange={(e) => setForm({ ...form, id: e.target.value })}
        />
        <input
          placeholder="name"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
        />
        <input
          placeholder="currency"
          value={form.currency}
          onChange={(e) => setForm({ ...form, currency: e.target.value })}
        />
        <button onClick={submit}>{editing ? "Update" : "Create"}</button>
      </div>
    </div>
  );
}
