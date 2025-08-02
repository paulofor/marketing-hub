import { useForm } from "react-hook-form";

interface LeadForm {
  leadgenId: string;
  instagramUserId: string;
  adId: string;
  campaignId: string;
  experimentId: string;
}

/**
 * Simple page to manually post a lead to the backend webhook.
 */
export default function LeadTester() {
  const { register, handleSubmit, reset } = useForm<LeadForm>();

  const onSubmit = async (data: LeadForm) => {
    await fetch("/webhook/leadgen", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        ...data,
        capturedAt: new Date().toISOString(),
      }),
    });
    reset();
  };

  return (
    <div className="container">
      <h1>Lead Tester</h1>
      <form>
        <input placeholder="leadgenId" {...register("leadgenId")} />
        <input placeholder="instagramUserId" {...register("instagramUserId")} />
        <input placeholder="adId" {...register("adId")} />
        <input placeholder="campaignId" {...register("campaignId")} />
        <input placeholder="experimentId" {...register("experimentId")} />
        <button
          type="button"
          onClick={handleSubmit(onSubmit, (errors) => {
            console.log("Validation errors", errors);
          })}
        >
          Send
        </button>
      </form>
    </div>
  );
}
