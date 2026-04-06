import { z } from "zod";

const shortText = z.string().max(600).optional();
const summaryText = z.string().max(400).optional();

export const hypothesisFrameworkSchema = z.object({
  version: z.string().optional(),
  pain: z.object({
    surface: shortText,
    root: shortText,
    emotional: shortText,
    social: shortText,
    cost: shortText,
    summary: summaryText,
  }),
  result: z.object({
    desiredResult: shortText,
    desiredIdentity: shortText,
    businessOutcome: shortText,
    successSignal: shortText,
    summary: summaryText,
  }),
  mechanism: z.object({
    core: shortText,
    unique: shortText,
    visible: shortText,
    believability: shortText,
    summary: summaryText,
  }),
  proof: z.object({
    type: shortText,
    asset: shortText,
    message: shortText,
    deliveryStage: shortText,
    summary: summaryText,
  }),
  offer: z.object({
    name: shortText,
    corePromise: shortText,
    deliverables: shortText,
    riskReversal: shortText,
    priceLogic: shortText,
    cta: shortText,
    summary: summaryText,
    priceAmount: z.number().optional(),
    offerType: z.string().optional(),
  }),
  checklist: z.object({
    painReady: z.boolean().optional(),
    resultReady: z.boolean().optional(),
    mechanismReady: z.boolean().optional(),
    proofReady: z.boolean().optional(),
    offerReady: z.boolean().optional(),
    approvedForExperiment: z.boolean().optional(),
    notes: shortText,
  }),
});

export const hypothesisFormSchema = z
  .object({
    title: z.string().min(8).max(120),
    promise: z.string().max(140).optional(),
    problem: z.string().min(1),
    persona: z.string().min(1),
    mechanism: z.string().optional(),
    uniqueMechanism: z.string().optional(),
    entrega: z.string().optional(),
    successRule: z.string().optional(),
    imageFilterTitle: z.string().max(255).optional(),
    premiseAngleId: z.string().optional(),
    offerType: z.enum(["LEAD", "TRIPWIRE"]),
    price: z.preprocess(
      (v) => (v === "" || v === undefined ? undefined : Number(v)),
      z.number().optional(),
    ),
    kpiTargetCpl: z.preprocess(Number, z.number()),
    offerPackageId: z
      .preprocess(
        (val) =>
          val === "" || val === null || val === undefined ? null : Number(val),
        z.number().nullable(),
      )
      .optional(),
    framework: hypothesisFrameworkSchema,
  });

export type HypothesisFormValues = z.infer<typeof hypothesisFormSchema>;
