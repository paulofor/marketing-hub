import { useState } from "react";
import type { ComponentType, ReactNode } from "react";
import {
  BadgeDollarSign,
  Brush,
  FileText,
  FlaskConical,
  Loader2,
  Megaphone,
  PackageCheck,
  Save,
  Target,
} from "lucide-react";
import { CreateProduct } from "../../api/product/useCreateProduct";
import { Product } from "../../api/product/useProducts";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import { useNiches } from "../../api/niche/useNiches";

type ProductFormValues = {
  [K in keyof CreateProduct]-?: string;
};

const defaultForm: ProductFormValues = {
  slug: "",
  name: "",
  publicUrl: "",
  logoUrl: "",
  colorPalette: "",
  targetAudience: "",
  languageStyle: "",
  codeModules: "",
  productType: "PDE - Produto Digital Experiencial",
  productFormat: "",
  deliveryMode: "",
  revenueModel: "",
  valueUnit: "",
  valueEvidenceMetric: "",
  validationDefinitionVersion: "v1",
  validationDefinitionJson: "",
  commercialStatus: "VALIDACAO_COMERCIAL",
  currentPriceBrl: "",
  primaryHypothesisId: "",
  primaryHypothesis: "",
  associatedExperiments: "",
  commercialNotes: "",
  sevenDayJourney: "",
  supportMaterialPositioning: "",
  primaryCta: "",
  niche: "",
  marketNicheId: "",
  avatar: "",
  instagramAccountId: "",
  explicitPain: "",
  promise: "",
  uniqueMechanism: "",
  tripwire: "",
  riskReversal: "",
  socialProof: "",
  scientificEvidencePack: "",
  pdeExperienceJson: "",
  checkoutMonetization: "",
  funnel: "",
  creativeVolume: "",
  storytelling: "",
  aiCost: "",
};

function toFormValues(product?: Product): ProductFormValues {
  if (!product) return defaultForm;
  return {
    slug: product.slug ?? "",
    name: product.name ?? "",
    publicUrl: product.publicUrl ?? "",
    logoUrl: product.logoUrl ?? "",
    colorPalette: product.colorPalette ?? "",
    targetAudience: product.targetAudience ?? "",
    languageStyle: product.languageStyle ?? "",
    codeModules: product.codeModules ?? "",
    productType: product.productType ?? defaultForm.productType,
    productFormat: product.productFormat ?? "",
    deliveryMode: product.deliveryMode ?? "",
    revenueModel: product.revenueModel ?? "",
    valueUnit: product.valueUnit ?? "",
    valueEvidenceMetric: product.valueEvidenceMetric ?? "",
    validationDefinitionVersion:
      product.validationDefinitionVersion ??
      defaultForm.validationDefinitionVersion,
    validationDefinitionJson: product.validationDefinitionJson ?? "",
    commercialStatus: product.commercialStatus ?? defaultForm.commercialStatus,
    currentPriceBrl:
      product.currentPriceBrl != null ? String(product.currentPriceBrl) : "",
    primaryHypothesisId: product.primaryHypothesisId ?? "",
    primaryHypothesis: product.primaryHypothesis ?? "",
    associatedExperiments: product.associatedExperiments ?? "",
    commercialNotes: product.commercialNotes ?? "",
    sevenDayJourney: product.sevenDayJourney ?? "",
    supportMaterialPositioning: product.supportMaterialPositioning ?? "",
    primaryCta: product.primaryCta ?? "",
    niche: product.niche ?? "",
    marketNicheId:
      product.marketNicheId != null ? String(product.marketNicheId) : "",
    avatar: product.avatar ?? "",
    instagramAccountId:
      product.instagramAccountId != null
        ? String(product.instagramAccountId)
        : "",
    explicitPain: product.explicitPain ?? "",
    promise: product.promise ?? "",
    uniqueMechanism: product.uniqueMechanism ?? "",
    tripwire: product.tripwire ?? "",
    riskReversal: product.riskReversal ?? "",
    socialProof: product.socialProof ?? "",
    scientificEvidencePack: product.scientificEvidencePack ?? "",
    pdeExperienceJson: product.pdeExperienceJson ?? "",
    checkoutMonetization: product.checkoutMonetization ?? "",
    funnel: product.funnel ?? "",
    creativeVolume: product.creativeVolume ?? "",
    storytelling: product.storytelling ?? "",
    aiCost: product.aiCost != null ? String(product.aiCost) : "",
  };
}

function toPayload(form: ProductFormValues): CreateProduct {
  return {
    ...form,
    marketNicheId: Number(form.marketNicheId) || undefined,
    instagramAccountId: Number(form.instagramAccountId) || undefined,
    currentPriceBrl: Number(form.currentPriceBrl) || undefined,
    aiCost: Number(form.aiCost) || 0,
  };
}

type ProductFieldProps = {
  field: keyof ProductFormValues;
  label: string;
  value: string;
  onChange: (field: keyof ProductFormValues, value: string) => void;
  required?: boolean;
  multiline?: boolean;
  rows?: number;
  inputMode?: "decimal" | "numeric" | "text" | "url";
  placeholder?: string;
};

function ProductField({
  field,
  label,
  value,
  onChange,
  required = false,
  multiline = false,
  rows = 3,
  inputMode = "text",
  placeholder,
}: ProductFieldProps) {
  return (
    <div className="product-editor-field">
      <label className="form-label" htmlFor={`product-${String(field)}`}>
        {label}
        {required ? " *" : ""}
      </label>
      {multiline ? (
        <textarea
          id={`product-${String(field)}`}
          className="form-control"
          rows={rows}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(field, e.target.value)}
        />
      ) : (
        <input
          id={`product-${String(field)}`}
          className="form-control"
          inputMode={inputMode}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(field, e.target.value)}
        />
      )}
    </div>
  );
}

type ProductEditorSectionProps = {
  icon: ComponentType<{ size?: number }>;
  title: string;
  children: ReactNode;
};

function ProductEditorSection({
  icon: Icon,
  title,
  children,
}: ProductEditorSectionProps) {
  return (
    <section className="product-editor-section">
      <div className="product-editor-section__title">
        <span aria-hidden="true">
          <Icon size={18} />
        </span>
        <h2>{title}</h2>
      </div>
      <div className="product-editor-section__body">{children}</div>
    </section>
  );
}

type ProductFormProps = {
  initialProduct?: Product;
  isSaving: boolean;
  submitLabel?: string;
  onSubmit: (payload: CreateProduct) => void;
};

export default function ProductForm({
  initialProduct,
  isSaving,
  submitLabel = "Salvar",
  onSubmit,
}: ProductFormProps) {
  const { data: accountsData } = useInstagramAccounts();
  const { data: nichesData } = useNiches();
  const accounts = Array.isArray(accountsData) ? accountsData : [];
  const niches = Array.isArray(nichesData) ? nichesData : [];
  const [form, setForm] = useState<ProductFormValues>(() =>
    toFormValues(initialProduct),
  );

  const setField = (field: keyof ProductFormValues, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const submit = () => {
    onSubmit(toPayload(form));
  };

  return (
    <div className="product-editor">
      <aside className="product-editor-summary">
        <span className="badge text-bg-light border">
          {form.commercialStatus || "Sem status"}
        </span>
        <h2>{form.name || "Produto sem nome"}</h2>
        <dl>
          <div>
            <dt>Tipo</dt>
            <dd>{form.productType || "Não informado"}</dd>
          </div>
          <div>
            <dt>Preço</dt>
            <dd>
              {form.currentPriceBrl
                ? `R$ ${form.currentPriceBrl}`
                : "Preço aberto"}
            </dd>
          </div>
          <div>
            <dt>Nicho</dt>
            <dd>{form.niche || "Não selecionado"}</dd>
          </div>
          <div>
            <dt>Promessa</dt>
            <dd>{form.promise || form.primaryHypothesis || "Não definida"}</dd>
          </div>
        </dl>
        <button
          className="btn btn-primary w-100"
          onClick={submit}
          disabled={isSaving}
        >
          {isSaving ? (
            <>
              <Loader2
                className="product-editor__button-icon spinning"
                size={16}
              />
              Salvando...
            </>
          ) : (
            <>
              <Save className="product-editor__button-icon" size={16} />
              {submitLabel}
            </>
          )}
        </button>
      </aside>

      <div className="product-editor-main">
        <ProductEditorSection icon={PackageCheck} title="Identidade comercial">
          <div className="product-editor-grid product-editor-grid--3">
            <div className="product-editor-grid__wide">
              <ProductField
                field="name"
                label="Nome comercial"
                required
                value={form.name}
                onChange={setField}
              />
            </div>
            <ProductField
              field="slug"
              label="Slug"
              required
              value={form.slug}
              onChange={setField}
            />
            <ProductField
              field="currentPriceBrl"
              label="Preço atual"
              inputMode="decimal"
              value={form.currentPriceBrl}
              onChange={setField}
            />
          </div>
          <div className="product-editor-grid product-editor-grid--2">
            <ProductField
              field="publicUrl"
              label="URL pública"
              inputMode="url"
              value={form.publicUrl}
              onChange={setField}
            />
            <ProductField
              field="logoUrl"
              label="URL do logo"
              inputMode="url"
              value={form.logoUrl}
              onChange={setField}
            />
            <ProductField
              field="productType"
              label="Tipo de produto"
              value={form.productType}
              onChange={setField}
            />
            <ProductField
              field="commercialStatus"
              label="Status comercial"
              value={form.commercialStatus}
              onChange={setField}
            />
            <ProductField
              field="aiCost"
              label="Custo de IA"
              inputMode="decimal"
              value={form.aiCost}
              onChange={setField}
            />
          </div>
        </ProductEditorSection>

        <ProductEditorSection
          icon={FlaskConical}
          title="Definição do teste de produto"
        >
          <div className="product-editor-grid product-editor-grid--3">
            <ProductField
              field="productFormat"
              label="Formato entregue"
              value={form.productFormat}
              onChange={setField}
              placeholder="Programa guiado, pacote de imagens, diagnóstico..."
            />
            <ProductField
              field="deliveryMode"
              label="Modo de entrega"
              value={form.deliveryMode}
              onChange={setField}
              placeholder="Automática, personalizada, híbrida..."
            />
            <ProductField
              field="revenueModel"
              label="Modelo de receita"
              value={form.revenueModel}
              onChange={setField}
              placeholder="Compra única, assinatura, recorrência..."
            />
            <ProductField
              field="valueUnit"
              label="Unidade de valor"
              value={form.valueUnit}
              onChange={setField}
              placeholder="7 dias concluídos, 10 imagens utilizáveis..."
            />
            <ProductField
              field="valueEvidenceMetric"
              label="Evidência de valor"
              value={form.valueEvidenceMetric}
              onChange={setField}
              placeholder="Uso, satisfação, recompra ou resultado percebido"
            />
            <ProductField
              field="validationDefinitionVersion"
              label="Versão da definição"
              value={form.validationDefinitionVersion}
              onChange={setField}
            />
          </div>
          <ProductField
            field="validationDefinitionJson"
            label="Contrato comparável de validação (JSON)"
            multiline
            rows={12}
            value={form.validationDefinitionJson}
            onChange={setField}
            placeholder='{"problem":{},"promise":{},"mechanism":{},"format":{},"delivery":{},"economics":{},"successEvidence":{},"decisionRules":{}}'
          />
        </ProductEditorSection>

        <ProductEditorSection icon={Target} title="Mercado e persona">
          <div className="product-editor-grid product-editor-grid--2">
            <div className="product-editor-field">
              <label className="form-label" htmlFor="product-marketNicheId">
                Nicho *
              </label>
              <select
                id="product-marketNicheId"
                className="form-select"
                value={form.marketNicheId}
                onChange={(e) =>
                  setForm({
                    ...form,
                    marketNicheId: e.target.value,
                    niche:
                      niches.find(
                        (niche) => String(niche.id) === e.target.value,
                      )?.name ?? "",
                  })
                }
              >
                <option value="">Selecione o Nicho</option>
                {niches.map((niche) => (
                  <option key={niche.id} value={niche.id}>
                    {niche.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="product-editor-field">
              <label
                className="form-label"
                htmlFor="product-instagramAccountId"
              >
                Conta do Instagram
              </label>
              <select
                id="product-instagramAccountId"
                className="form-select"
                value={form.instagramAccountId}
                onChange={(e) => setField("instagramAccountId", e.target.value)}
              >
                <option value="">Selecione a Conta do Instagram</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <ProductField
            field="targetAudience"
            label="Público alvo"
            multiline
            rows={3}
            value={form.targetAudience}
            onChange={setField}
          />
          <ProductField
            field="avatar"
            label="Avatar"
            multiline
            rows={3}
            value={form.avatar}
            onChange={setField}
          />
          <ProductField
            field="explicitPain"
            label="Dor explícita"
            multiline
            rows={3}
            value={form.explicitPain}
            onChange={setField}
          />
        </ProductEditorSection>

        <ProductEditorSection
          icon={BadgeDollarSign}
          title="Oferta e monetização"
        >
          <ProductField
            field="primaryHypothesis"
            label="Hipótese/oferta principal"
            multiline
            rows={3}
            value={form.primaryHypothesis}
            onChange={setField}
          />
          <div className="product-editor-grid product-editor-grid--2">
            <ProductField
              field="promise"
              label="Promessa transformadora"
              multiline
              rows={4}
              value={form.promise}
              onChange={setField}
            />
            <ProductField
              field="uniqueMechanism"
              label="Mecanismo único"
              multiline
              rows={4}
              value={form.uniqueMechanism}
              onChange={setField}
            />
            <ProductField
              field="tripwire"
              label="Oferta principal"
              multiline
              rows={4}
              value={form.tripwire}
              onChange={setField}
            />
            <ProductField
              field="checkoutMonetization"
              label="Monetização do checkout"
              multiline
              rows={4}
              value={form.checkoutMonetization}
              onChange={setField}
            />
          </div>
          <ProductField
            field="riskReversal"
            label="Reversão de risco"
            multiline
            rows={3}
            value={form.riskReversal}
            onChange={setField}
          />
          <ProductField
            field="sevenDayJourney"
            label="Jornada de 7 dias"
            multiline
            rows={8}
            value={form.sevenDayJourney}
            onChange={setField}
          />
          <div className="product-editor-grid product-editor-grid--2">
            <ProductField
              field="primaryCta"
              label="CTA principal"
              value={form.primaryCta}
              onChange={setField}
            />
            <ProductField
              field="supportMaterialPositioning"
              label="Posicionamento do material de apoio"
              multiline
              rows={4}
              value={form.supportMaterialPositioning}
              onChange={setField}
            />
          </div>
        </ProductEditorSection>

        <ProductEditorSection icon={Megaphone} title="Comunicação e criativos">
          <div className="product-editor-grid product-editor-grid--2">
            <ProductField
              field="languageStyle"
              label="Estilo de linguagem"
              multiline
              rows={4}
              value={form.languageStyle}
              onChange={setField}
            />
            <ProductField
              field="storytelling"
              label="Storytelling"
              multiline
              rows={4}
              value={form.storytelling}
              onChange={setField}
            />
            <ProductField
              field="creativeVolume"
              label="Volume criativo"
              multiline
              rows={3}
              value={form.creativeVolume}
              onChange={setField}
            />
            <ProductField
              field="socialProof"
              label="Prova social"
              multiline
              rows={3}
              value={form.socialProof}
              onChange={setField}
            />
          </div>
          <ProductField
            field="scientificEvidencePack"
            label="Base científica operacional"
            multiline
            rows={5}
            value={form.scientificEvidencePack}
            onChange={setField}
          />
          <ProductField
            field="pdeExperienceJson"
            label="Contrato JSON da experiência PDE"
            multiline
            rows={12}
            value={form.pdeExperienceJson}
            onChange={setField}
          />
        </ProductEditorSection>

        <ProductEditorSection icon={Brush} title="Identidade visual e canais">
          <ProductField
            field="colorPalette"
            label="Paleta de cores"
            multiline
            rows={3}
            value={form.colorPalette}
            onChange={setField}
          />
          <ProductField
            field="funnel"
            label="Funil"
            multiline
            rows={4}
            value={form.funnel}
            onChange={setField}
          />
          <ProductField
            field="codeModules"
            label="Módulos de código"
            multiline
            rows={3}
            value={form.codeModules}
            onChange={setField}
          />
        </ProductEditorSection>

        <ProductEditorSection icon={FileText} title="Histórico e observações">
          <ProductField
            field="associatedExperiments"
            label="Experimentos associados"
            multiline
            rows={3}
            value={form.associatedExperiments}
            onChange={setField}
          />
          <ProductField
            field="commercialNotes"
            label="Observações comerciais"
            multiline
            rows={5}
            value={form.commercialNotes}
            onChange={setField}
          />
        </ProductEditorSection>
      </div>
    </div>
  );
}
