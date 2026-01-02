import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type LeadPortalPaymentHistory = {
  at: string | null;
  label: string;
  status: string | null;
  source: string;
  detail: string | null;
};

export type LeadPortalPayment = {
  id: number;
  packageId: number;
  submissionId?: string | null;
  buyerName?: string | null;
  buyerEmail?: string | null;
  status: string;
  mercadoPagoStatus?: string | null;
  mercadoPagoPaymentId?: string | null;
  mercadoPagoPreferenceId?: string | null;
  paymentType?: string | null;
  paymentMethod?: string | null;
  rejectionReason?: string | null;
  deliveryError?: string | null;
  amount?: number | null;
  currency?: string | null;
  checkoutExpiresAt?: string | null;
  paymentApprovedAt?: string | null;
  deliveredAt?: string | null;
  createdAt: string;
  updatedAt: string;
  history: LeadPortalPaymentHistory[];
};

export function useLeadPortalPayments(limit = 50) {
  return useQuery({
    queryKey: ["lead-portal-payments", limit],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalPayment[]>("/api/lead-portal/payments", {
        params: { limit },
      });
      return data;
    },
    refetchInterval: 30000,
  });
}
