import { useState } from "react";
import type { ComponentType, ReactNode } from "react";
import { Link } from "react-router-dom";
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
import { useProductTypes } from "../../api/productType/useProductTypes";

type ProductFormValues = {
  [K in Exclude<keyof CreateProduct, "aliases">]-?: string;
} & { aliases: string };

const defaultForm: ProductFormValues = {
  slug: "",
  name: "",
  internalName: "",
  aliases: "",
  publicUrl: "",
  logoUrl: "",
  colorPalette: "",
  targetAudience: "",
  languageStyle: "",
  codeModules: "",
  productType: "",
  productTypeId: "",
  productFormat: "",
  deliveryMode: "",
  revenueModel: "",
  valueUnit: "",
  valueEvidenceMetric: "",
  validationDefinitionVersion: "v1",
  validationDefinitionJson: "",
  desireAssociationMapVersion: "v1",
  desireAssociationMapJson: "",
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
    internalName: product.internalName ?? product.name ?? "",
    aliases: product.aliases?.join("\n") ?? "",
    publicUrl: product.publicUrl ?? "",
    logoUrl: product.logoUrl ?? "",
    colorPalette: product.colorPalette ?? "",
    targetAudience: product.targetAudience ?? "",
    languageStyle: product.languageStyle ?? "",
    codeModules: product.codeModules ?? "",
    productType: product.productType ?? defaultForm.productType,
    productTypeId:
      product.productTypeId != null ? String(product.productTypeId) : "",
    productFormat: product.productFormat ?? "",
    deliveryMode: product.deliveryMode ?? "",
    revenueModel: product.revenueModel ?? "",
    valueUnit: product.valueUnit ?? "",
    valueEvidenceMetric: product.valueEvidenceMetric ?? "",
    validationDefinitionVersion:
      product.validationDefinitionVersion ??
      defaultForm.validationDefinitionVersion,
    validationDefinitionJson: product.validationDefinitionJson ?? "",
    desireAssociationMapVersion: product.desireAssociationMapVersion ?? "v1",
    desireAssociationMapJson: product.desireAssociationMapJson ?? "",
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
    aliases: form.aliases
      .split(/[,;\n]/)
      .map((alias) => alias.trim())
      .filter(Boolean),
    marketNicheId: Number(form.marketNicheId) || undefined,
    instagramAccountId: Number(form.instagramAccountId) || undefined,
    currentPriceBrl: Number(form.currentPriceBrl) || undefined,
    productTypeId: Number(form.productTypeId) || undefined,
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
  maxLength?: number;
  helpText?: string;
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
  maxLength,
  helpText,
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
          maxLength={maxLength}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(field, e.target.value)}
        />
      ) : (
        <input
          id={`product-${String(field)}`}
          className="form-control"
          inputMode={inputMode}
          maxLength={maxLength}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(field, e.target.value)}
        />
      )}
      {helpText && <div className="form-text">{helpText}</div>}
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
  const productTypesQuery = useProductTypes(true);
  const accounts = Array.isArray(accountsData) ? accountsData : [];
  const niches = Array.isArray(nichesData) ? nichesData : [];
  const [form, setForm] = useState<ProductFormValues>(() =>
    toFormValues(initialProduct),
  );
  const productTypes = Array.isArray(productTypesQuery.data)
    ? productTypesQuery.data.filter(
        (type) =>
          type.status === "ACTIVE" || String(type.id) === form.productTypeId,
      )
    : [];

  const setField = (field: keyof ProductFormValues, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const submit = () => {
    if (!form.productTypeId) return;
    onSubmit(toPayload(form));
  };

  const selectedProductType = productTypes.find(
    (type) => String(type.id) === form.productTypeId,
  );

  const applyAgendaCheiaDesireMap = () => {
    setForm((current) => ({
      ...current,
      desireAssociationMapVersion: "v1",
      desireAssociationMapJson: JSON.stringify(
        {
          painState:
            "Comunicação improvisada que não transmite o valor profissional do trabalho da nail designer.",
          desiredState:
            "Sentir orgulho, reconhecimento e tranquilidade ao divulgar uma presença profissional consistente.",
          territories: [
            {
              code: "PROFESSIONAL_PRIDE",
              name: "Orgulho profissional",
              idea: "Um perfil à altura do talento da profissional.",
              symbols: [
                "perfil organizado",
                "trabalho bem apresentado",
                "post publicado com confiança",
              ],
              truthBoundary:
                "Pode mostrar valorização da apresentação; não pode garantir fama, renda ou agenda lotada.",
            },
            {
              code: "RECOGNITION",
              name: "Reconhecimento",
              idea: "O trabalho percebido como profissional antes mesmo da primeira conversa.",
              symbols: [
                "cliente demonstrando interesse",
                "elogio real",
                "portfólio coerente",
              ],
              truthBoundary:
                "Pode representar interesse plausível; não pode inventar depoimentos, números ou resultados.",
            },
            {
              code: "TRANQUILITY",
              name: "Tranquilidade",
              idea: "Conteúdo pronto para divulgar sem perder horas criando.",
              symbols: [
                "conteúdo planejado",
                "tempo preservado",
                "rotina de divulgação simples",
              ],
              truthBoundary:
                "Pode prometer redução de esforço compatível com a entrega; não pode garantir vendas.",
            },
          ],
          causalChain: [
            "ativos visuais personalizados",
            "presença profissional consistente",
            "maior clareza e confiança percebida",
            "mais oportunidades de conversa e agendamento",
          ],
          evidence: {
            currentLevel: "HYPOTHESIS",
            required: [
              "visualização da prévia",
              "briefing concluído",
              "venda aprovada",
              "satisfação pós-entrega",
            ],
          },
          prohibitedAssociations: [
            "agenda lotada garantida",
            "renda garantida",
            "depoimento ou resultado não comprovado",
          ],
          measurementPlan: {
            isolateOneTerritoryPerCreative: true,
            keepConstant: ["público", "oferta", "preço", "canal", "CTA"],
            funnel: [
              "impressão",
              "clique",
              "briefing",
              "venda",
              "uso",
              "satisfação",
            ],
            publicationRequires: ["AD_SPECIALIST_APPROVED", "HUMAN_APPROVED"],
          },
        },
        null,
        2,
      ),
    }));
  };

  return (
    <div className="product-editor">
      <aside className="product-editor-summary">
        <span className="badge text-bg-light border">
          {form.commercialStatus || "Sem status"}
        </span>
        <h2>{form.name || "Produto sem nome"}</h2>
        <p className="text-muted small mb-3">
          Internamente: {form.internalName || "nome ainda não definido"}
        </p>
        <dl>
          <div>
            <dt>Tipo</dt>
            <dd>
              {selectedProductType?.name || form.productType || "Não informado"}
            </dd>
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
          disabled={isSaving || !form.productTypeId}
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
                label="Nome comercial (visível ao cliente)"
                required
                maxLength={191}
                value={form.name}
                onChange={setField}
              />
            </div>
            <ProductField
              field="internalName"
              label="Nome interno/de trabalho"
              required
              maxLength={191}
              value={form.internalName}
              onChange={setField}
              helpText="Permanece estável enquanto o nome comercial evolui."
            />
            <ProductField
              field="slug"
              label="Slug"
              required
              maxLength={191}
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
          <ProductField
            field="aliases"
            label="Apelidos internos"
            multiline
            rows={3}
            value={form.aliases}
            onChange={setField}
            placeholder="MUSA v7, vídeos orientados ao desejo, projeto presença"
            helpText="Use vírgula ou uma linha por apelido. Eles servem para busca interna e nunca aparecem na oferta pública."
          />
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
            <div className="product-editor-field">
              <label className="form-label" htmlFor="product-productTypeId">
                Tipo de produto *
              </label>
              <select
                id="product-productTypeId"
                className="form-select"
                required
                value={form.productTypeId}
                onChange={(event) => {
                  const selected = productTypes.find(
                    (type) => String(type.id) === event.target.value,
                  );
                  setForm((current) => ({
                    ...current,
                    productTypeId: event.target.value,
                    productType: selected?.name ?? current.productType,
                  }));
                }}
              >
                <option value="">
                  {productTypesQuery.isLoading
                    ? "Carregando tipos..."
                    : "Selecione um tipo"}
                </option>
                {productTypes.map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.name}
                    {type.status === "RETIRED" ? " (aposentado)" : ""}
                  </option>
                ))}
              </select>
              <div className="form-text">
                Não encontrou uma classificação adequada?{" "}
                <Link to="/product-types">Cadastre um tipo ou apelido</Link> sem
                limitar a ideia do produto.
              </div>
              {!form.productTypeId && (
                <div className="form-text text-danger">
                  Selecione um tipo em uso antes de salvar o produto.
                </div>
              )}
              {productTypesQuery.isError && (
                <div className="form-text text-danger">
                  Não foi possível carregar o catálogo de tipos.
                </div>
              )}
            </div>
            <ProductField
              field="commercialStatus"
              label="Status comercial"
              maxLength={64}
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
              maxLength={64}
              value={form.productFormat}
              onChange={setField}
              placeholder="Programa guiado, pacote de imagens, diagnóstico..."
            />
            <ProductField
              field="deliveryMode"
              label="Modo de entrega"
              maxLength={64}
              value={form.deliveryMode}
              onChange={setField}
              placeholder="Automática, personalizada, híbrida..."
            />
            <ProductField
              field="revenueModel"
              label="Modelo de receita"
              maxLength={64}
              value={form.revenueModel}
              onChange={setField}
              placeholder="Compra única, assinatura, recorrência..."
            />
            <ProductField
              field="valueUnit"
              label="Unidade de valor"
              maxLength={191}
              value={form.valueUnit}
              onChange={setField}
              placeholder="7 dias concluídos, 10 imagens utilizáveis..."
            />
            <ProductField
              field="valueEvidenceMetric"
              label="Evidência de valor"
              maxLength={191}
              value={form.valueEvidenceMetric}
              onChange={setField}
              placeholder="Uso, satisfação, recompra ou resultado percebido"
            />
            <ProductField
              field="validationDefinitionVersion"
              label="Versão da definição"
              maxLength={32}
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
          <div className="d-flex flex-wrap align-items-end gap-3 mb-3">
            <div className="flex-grow-1">
              <ProductField
                field="desireAssociationMapVersion"
                label="Versão do mapa de associações de desejo"
                value={form.desireAssociationMapVersion}
                onChange={setField}
              />
            </div>
            <button
              type="button"
              className="btn btn-outline-primary mb-3"
              onClick={applyAgendaCheiaDesireMap}
            >
              Aplicar mapa inicial do Agenda Cheia
            </button>
          </div>
          <ProductField
            field="desireAssociationMapJson"
            label="Mapa de associações de desejo (JSON)"
            multiline
            rows={16}
            value={form.desireAssociationMapJson}
            onChange={setField}
            placeholder='{"painState":"...","desiredState":"...","territories":[],"causalChain":[],"evidence":{},"prohibitedAssociations":[],"measurementPlan":{}}'
          />
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
