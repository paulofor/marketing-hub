import { FormEvent, ReactNode, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FlaskConical, Layers, Megaphone, SearchCheck } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import {
  CreateManualExperiment,
  useCreateManualExperiment,
} from "../../api/experiment/useCreateManualExperiment";

type FormState = Record<keyof CreateManualExperiment, string>;

const INITIAL_STATE: FormState = {
  nicheName: "",
  nicheAudience: "",
  nicheDescription: "",
  marketReference: "",
  pains: "",
  desires: "",
  likelyChannels: "Meta Ads",
  hypothesisStatement: "",
  persona: "",
  problem: "",
  promise: "",
  mechanism: "",
  proof: "",
  successSignal: "",
  offerName: "",
  leadMagnet: "",
  productName: "",
  primaryCta: "",
  testPrice: "47",
  promiseLimit: "",
  validationType: "Lista de prioridade",
  experimentChannel: "Meta Ads",
  dailyBudget: "50",
  kpiTargetCpl: "10",
  sampleSize: "100",
  creativeAngles: "",
  successCriteria:
    "CTR acima de 1,5%; conversao em lead acima de 20%; clique em compra acima de 5%",
  discardCriteria:
    "Baixo clique no criativo, baixa conversao da landing ou ausencia de sinal de intencao de compra",
};

export default function ManualExperimentWizardPage() {
  const navigate = useNavigate();
  const createManualExperiment = useCreateManualExperiment();
  const [form, setForm] = useState<FormState>(INITIAL_STATE);
  const requiredFieldsComplete = useMemo(
    () => Boolean(form.nicheName && form.persona && form.problem),
    [form],
  );

  function updateField(name: keyof CreateManualExperiment, value: string) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const experiment = await createManualExperiment.mutateAsync(
      toPayload(form),
    );
    navigate(`/experiments/${experiment.id}`, {
      state: { initialTab: "construction" },
    });
  }

  return (
    <div>
      <PageTitle icon={experimentIcon}>Experimento Manual</PageTitle>
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
        <div>
          <span className="badge text-bg-warning mb-2">Fluxo manual</span>
          <p className="text-muted mb-0">
            Registra somente o brief inicial. Mecanismo, oferta, prova,
            entregáveis e métricas são conduzidos na aba Construção.
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/experiments">
          Voltar
        </Link>
      </div>

      <form onSubmit={handleSubmit}>
        <WizardSection
          icon={<Layers size={20} />}
          title="1. Nicho e dor"
          subtitle="O mínimo necessário para iniciar investigação comercial sem inventar solução cedo demais"
        >
          <div className="row g-3">
            <TextField
              label="Nome do nicho"
              name="nicheName"
              value={form.nicheName}
              onChange={updateField}
              required
            />
            <TextField
              label="Público inicial"
              name="nicheAudience"
              value={form.nicheAudience}
              onChange={updateField}
            />
            <TextAreaField
              label="Referência de mercado"
              name="marketReference"
              value={form.marketReference}
              onChange={updateField}
              placeholder="Ex.: Marie Claire, editorias de beleza, saúde e carreira"
            />
            <TextAreaField
              label="Dor observada"
              name="pains"
              value={form.pains}
              onChange={updateField}
              required
            />
            <TextAreaField
              label="Desejo ou alívio buscado"
              name="desires"
              value={form.desires}
              onChange={updateField}
              placeholder="Ex.: reduzir esforço, ganhar controle, parecer mais profissional, vender com menos fricção"
            />
          </div>
        </WizardSection>

        <WizardSection
          icon={<FlaskConical size={20} />}
          title="2. Hipótese inicial"
          subtitle="A aposta de mercado antes da pesquisa MDS e antes da fabricação FEO"
        >
          <div className="row g-3">
            <TextField
              label="Persona"
              name="persona"
              value={form.persona}
              onChange={updateField}
              required
            />
            <TextField
              label="Dor principal em uma frase"
              name="problem"
              value={form.problem}
              onChange={updateField}
              required
            />
            <TextAreaField
              label="Hipótese de valor"
              name="hypothesisStatement"
              value={form.hypothesisStatement}
              onChange={updateField}
              placeholder="Ex.: acreditamos que este público vai responder a uma proposta que reduz uma dor concreta com menor esforço percebido."
            />
            <TextAreaField
              label="Sinal de sucesso esperado"
              name="successSignal"
              value={form.successSignal}
              onChange={updateField}
              placeholder="Ex.: envio de formulário, clique em compra, resposta qualificada, lead pedindo acesso ao material."
            />
          </div>
        </WizardSection>

        <WizardSection
          icon={<SearchCheck size={20} />}
          title="3. Insumos para MDS e Construção"
          subtitle="Contexto que ajuda a descobrir mecanismo, prova e oferta sem fechar a promessa antes da evidência"
        >
          <div className="row g-3">
            <TextAreaField
              label="Canais prováveis"
              name="likelyChannels"
              value={form.likelyChannels}
              onChange={updateField}
              placeholder="Ex.: Meta Ads, Instagram, Google, WhatsApp, e-mail."
            />
            <TextAreaField
              label="Restrições ou cuidados da promessa"
              name="promiseLimit"
              value={form.promiseLimit}
              onChange={updateField}
              placeholder="Ex.: não prometer cura, renda garantida ou resultado absoluto."
            />
          </div>
        </WizardSection>

        <WizardSection
          icon={<Megaphone size={20} />}
          title="4. Parâmetros iniciais de teste"
          subtitle="Configuração operacional preliminar; a campanha final será refinada na Construção"
        >
          <div className="row g-3">
            <TextField
              label="Canal inicial"
              name="experimentChannel"
              value={form.experimentChannel}
              onChange={updateField}
            />
            <TextField
              label="Orçamento diário"
              name="dailyBudget"
              type="number"
              value={form.dailyBudget}
              onChange={updateField}
            />
            <TextField
              label="CPL alvo"
              name="kpiTargetCpl"
              type="number"
              value={form.kpiTargetCpl}
              onChange={updateField}
            />
            <TextField
              label="Amostra"
              name="sampleSize"
              type="number"
              value={form.sampleSize}
              onChange={updateField}
            />
            <TextAreaField
              label="Ângulos ou observações iniciais"
              name="creativeAngles"
              value={form.creativeAngles}
              onChange={updateField}
            />
            <TextAreaField
              label="Critério inicial de aprovação"
              name="successCriteria"
              value={form.successCriteria}
              onChange={updateField}
            />
            <TextAreaField
              label="Critério de descarte"
              name="discardCriteria"
              value={form.discardCriteria}
              onChange={updateField}
            />
          </div>
        </WizardSection>

        <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 border-top pt-3">
          <span className="text-muted small">
            O experimento será salvo como <strong>MANUAL_FLOW</strong> e aberto
            na aba Construção para completar MDS, oferta, prova, FEO e funil.
          </span>
          <button
            className="btn btn-primary"
            type="submit"
            disabled={
              !requiredFieldsComplete || createManualExperiment.isPending
            }
          >
            {createManualExperiment.isPending
              ? "Criando..."
              : "Criar e abrir Construção"}
          </button>
        </div>
      </form>
    </div>
  );
}

function WizardSection({
  icon,
  title,
  subtitle,
  children,
}: {
  icon: ReactNode;
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <section className="py-3 border-top">
      <div className="d-flex align-items-center gap-2 mb-3">
        <span className="text-primary d-inline-flex">{icon}</span>
        <div>
          <h2 className="h5 mb-0">{title}</h2>
          <p className="small text-muted mb-0">{subtitle}</p>
        </div>
      </div>
      {children}
    </section>
  );
}

function TextField({
  label,
  name,
  value,
  onChange,
  type = "text",
  required,
}: {
  label: string;
  name: keyof CreateManualExperiment;
  value: string;
  onChange: (name: keyof CreateManualExperiment, value: string) => void;
  type?: string;
  required?: boolean;
}) {
  return (
    <div className="col-12 col-md-6">
      <label className="form-label">
        {label}
        {required ? <span className="text-danger"> *</span> : null}
      </label>
      <input
        className="form-control"
        type={type}
        value={value}
        onChange={(event) => onChange(name, event.target.value)}
        required={required}
      />
    </div>
  );
}

function TextAreaField({
  label,
  name,
  value,
  onChange,
  placeholder,
  required,
}: {
  label: string;
  name: keyof CreateManualExperiment;
  value: string;
  onChange: (name: keyof CreateManualExperiment, value: string) => void;
  placeholder?: string;
  required?: boolean;
}) {
  return (
    <div className="col-12">
      <label className="form-label">
        {label}
        {required ? <span className="text-danger"> *</span> : null}
      </label>
      <textarea
        className="form-control"
        rows={3}
        value={value}
        placeholder={placeholder}
        required={required}
        onChange={(event) => onChange(name, event.target.value)}
      />
    </div>
  );
}

function toPayload(form: FormState): CreateManualExperiment {
  return {
    ...form,
    testPrice: toOptionalNumber(form.testPrice),
    dailyBudget: toOptionalNumber(form.dailyBudget),
    kpiTargetCpl: toOptionalNumber(form.kpiTargetCpl),
    sampleSize: toOptionalNumber(form.sampleSize),
  };
}

function toOptionalNumber(value: string) {
  if (!value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}
