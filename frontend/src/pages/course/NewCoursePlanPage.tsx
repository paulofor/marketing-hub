import { useState } from "react";
import { useCreateCoursePlan } from "../../api/course/useCreateCoursePlan";
import PageTitle from "../../components/PageTitle";

export default function NewCoursePlanPage() {
  const create = useCreateCoursePlan();
  const [form, setForm] = useState({
    targetAudience: "",
    transformation: "",
    macroTopics: "",
  });

  const submit = () => {
    create.mutate(form);
  };

  return (
    <div>
      <PageTitle>New Course Plan</PageTitle>
      <input
        className="form-control mb-2"
        placeholder="Target Audience"
        value={form.targetAudience}
        onChange={(e) => setForm({ ...form, targetAudience: e.target.value })}
      />
      <input
        className="form-control mb-2"
        placeholder="Transformation"
        value={form.transformation}
        onChange={(e) => setForm({ ...form, transformation: e.target.value })}
      />
      <textarea
        className="form-control mb-2"
        placeholder="Macro Topics"
        value={form.macroTopics}
        onChange={(e) => setForm({ ...form, macroTopics: e.target.value })}
      />
      <button className="btn btn-primary" onClick={submit}>
        Generate
      </button>
    </div>
  );
}
