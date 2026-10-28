import axios from "axios";
export interface AdsDraft {
  experimentId: number;
  creativeId: number;
  name: string;
  title: string;
  body: string;
  imageUrl: string;
  targetUrl: string;
  contextHints: string[];
  currency: string;
  totalBudget: number;
  maxCpc: number;
  startsAt: string;
  endsAt: string;
}
export interface AdsCampaign {
  id: number;
  experimentId: number;
  productName: string;
  status: string;
  draft: AdsDraft;
  fingerprint: string;
  authorized: boolean;
  blockers: string[];
  canPublish: boolean;
  canActivate: boolean;
  canPause: boolean;
  remote: Record<string, unknown>;
  metrics: {
    impressions?: number;
    clicks?: number;
    spend?: number;
    currency?: string;
    observedAt?: string;
  };
  updatedAt: string;
}
export interface AdsOverview {
  connection: {
    status: string;
    name?: string;
    currency_code?: string;
    checkedAt?: string;
    message?: string;
  };
  campaigns: AdsCampaign[];
}
const root = "/api/chatgpt-ads/v1";
export const getAdsOverview = async (experimentId?: number) =>
  (await axios.get<AdsOverview>(root, { params: { experimentId } })).data;
export const saveAdsDraft = async (draft: AdsDraft, id?: number) =>
  (
    await (id
      ? axios.put<AdsCampaign>(`${root}/campaigns/${id}`, draft)
      : axios.post<AdsCampaign>(`${root}/campaigns`, draft))
  ).data;
export const adsAction = async (id: number, action: string, body?: unknown) =>
  (await axios.post<AdsCampaign>(`${root}/campaigns/${id}/${action}`, body))
    .data;
export const checkAdsConnection = async () => {
  await axios.post(`${root}/connection/check`);
};
