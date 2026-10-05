import { AlertTriangle, Video } from "lucide-react";
import { Link } from "react-router-dom";

import FacebookAutomationAlerts from "./FacebookAutomationAlerts";
import {
  useVideoReviewScope,
  useVideoReviewSummary,
} from "../api/creative/useCreativeVideoReviews";
import { useFacebookConfigurationStatus } from "../api/useFacebookConfigurationStatus";

export default function GlobalAutomationAlerts() {
  const { data } = useFacebookConfigurationStatus();
  const scope = useVideoReviewScope();
  const videoReviewQuery = useVideoReviewSummary(scope);
  const pendingVideoReviewCount =
    videoReviewQuery.data?.awaitingReviewCount ?? 0;
  const reviewParams = new URLSearchParams();
  if (scope.productId) reviewParams.set("productId", String(scope.productId));
  if (scope.experimentId)
    reviewParams.set("experimentId", String(scope.experimentId));
  const query = reviewParams.toString();
  const reviewUrl = `/creative-video-review${query ? `?${query}` : ""}`;
  const hasPendingVideoReviews = pendingVideoReviewCount > 0;

  if (!data && !hasPendingVideoReviews) {
    return null;
  }

  const workerIssues = data
    ? !data.worker.ready || !data.worker.hasAccount
    : false;
  const noRenewalEnabled = data
    ? data.tokenRenewal.enabledAccounts === 0
    : false;
  const renewalIssues =
    data &&
    data.tokenRenewal.enabledAccounts > 0 &&
    data.tokenRenewal.eligibleAccounts === 0 &&
    data.tokenRenewal.accounts.some((account) => !account.eligible);

  if (
    !hasPendingVideoReviews &&
    !workerIssues &&
    !noRenewalEnabled &&
    !renewalIssues
  ) {
    return null;
  }

  return (
    <div className="mb-4 d-flex flex-column gap-3">
      {hasPendingVideoReviews && (
        <div
          className="alert alert-info d-flex align-items-start gap-2 border border-info-subtle shadow-sm"
          role="alert"
        >
          <Video size={20} className="mt-1 flex-shrink-0" aria-hidden="true" />
          <div>
            <div className="fw-bold mb-1">
              {pendingVideoReviewCount === 1
                ? "1 vídeo precisa da sua aprovação"
                : `${pendingVideoReviewCount} vídeos precisam da sua aprovação`}
            </div>
            <p className="mb-2">
              {scope.productId || scope.experimentId
                ? "Neste contexto"
                : "Na fila geral do Hub"}
              : peças necessárias e prontas para sua decisão. Candidatas
              opcionais ficam na biblioteca de revisão. Esta aprovação não
              libera campanha nem gasto.
            </p>
            <Link
              className="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1"
              to={reviewUrl}
            >
              <AlertTriangle size={14} aria-hidden="true" />
              Ver aprovações necessárias
            </Link>
          </div>
        </div>
      )}
      {data && <FacebookAutomationAlerts status={data} />}
    </div>
  );
}
