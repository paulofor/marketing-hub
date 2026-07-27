import { AlertTriangle, Video } from "lucide-react";
import { Link } from "react-router-dom";

import FacebookAutomationAlerts from "./FacebookAutomationAlerts";
import { useCreativeVideoReviews } from "../api/creative/useCreativeVideoReviews";
import { useFacebookConfigurationStatus } from "../api/useFacebookConfigurationStatus";

export default function GlobalAutomationAlerts() {
  const { data } = useFacebookConfigurationStatus();
  const videoReviewQuery = useCreativeVideoReviews("DRAFT");
  const pendingVideoReviews = videoReviewQuery.data ?? [];
  const pendingVideoReviewCount = pendingVideoReviews.length;
  const hasPendingVideoReviews = pendingVideoReviewCount > 0;

  if (!data && !hasPendingVideoReviews) {
    return null;
  }

  const workerIssues = data
    ? !data.worker.ready || !data.worker.hasAccount
    : false;
  const noRenewalEnabled = data ? data.tokenRenewal.enabledAccounts === 0 : false;
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
          className="alert alert-danger d-flex align-items-start gap-2 border border-danger-subtle shadow-sm"
          role="alert"
        >
          <Video size={20} className="mt-1 flex-shrink-0" aria-hidden="true" />
          <div>
            <div className="fw-bold mb-1">
              {pendingVideoReviewCount === 1
                ? "1 vídeo aguardando aprovação"
                : `${pendingVideoReviewCount} vídeos aguardando aprovação`}
            </div>
            <p className="mb-2">
              Aprove ou reprove os vídeos prontos para não atrasar criativos,
              PDEs e campanhas que dependem dessa liberação.
            </p>
            <Link
              className="btn btn-sm btn-danger d-inline-flex align-items-center gap-1"
              to="/creative-video-review"
            >
              <AlertTriangle size={14} aria-hidden="true" />
              Revisar vídeos agora
            </Link>
          </div>
        </div>
      )}
      {data && <FacebookAutomationAlerts status={data} />}
    </div>
  );
}
