import { useState } from 'react';
import { useCreateMedia } from '../../api/media/useCreateMedia';

export default function NewMediaPage() {
  const create = useCreateMedia();
  const [form, setForm] = useState({ provider: 'SYNTHESIA', avatar: '', voice: '', script: '' });

  const submit = () => {
    create.mutate(form);
  };

  return (
    <div>
      <select className="form-select mb-2" value={form.provider} onChange={(e) => setForm({ ...form, provider: e.target.value })}>
        <option value="SYNTHESIA">Synthesia</option>
        <option value="HEYGEN">HeyGen</option>
      </select>
      <textarea className="form-control mb-2" placeholder="Script" value={form.script} onChange={(e) => setForm({ ...form, script: e.target.value })} />
      <button className="btn btn-primary" onClick={submit}>Generate</button>
    </div>
  );
}
