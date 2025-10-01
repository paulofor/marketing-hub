import FacebookAutomationAlerts from "./FacebookAutomationAlerts";
import { useFacebookConfigurationStatus } from "../api/useFacebookConfigurationStatus";

export default function GlobalAutomationAlerts() {
  const { data } = useFacebookConfigurationStatus();

  if (!data) {
    return null;
  }

  const workerIssues = !data.worker.ready || !data.worker.hasAccount;
  const noRenewalEnabled = data.tokenRenewal.enabledAccounts === 0;
  const renewalIssues =
    data.tokenRenewal.enabledAccounts > 0 &&
    data.tokenRenewal.eligibleAccounts === 0 &&
    data.tokenRenewal.accounts.some((account) => !account.eligible);

  if (!workerIssues && !noRenewalEnabled && !renewalIssues) {
    return null;
  }

  return (
    <div className="mb-4">
      <FacebookAutomationAlerts status={data} />
    </div>
  );
}
