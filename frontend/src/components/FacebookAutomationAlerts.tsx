import type { ReactNode } from "react";
import { AlertTriangle } from "lucide-react";
import { Link } from "react-router-dom";

import type { FacebookConfigurationStatus } from "../api/useFacebookConfigurationStatus";

interface FacebookAutomationAlertsProps {
  status?: FacebookConfigurationStatus;
}

export default function FacebookAutomationAlerts({
  status,
}: FacebookAutomationAlertsProps) {
  if (!status) {
    return null;
  }

  const alerts: ReactNode[] = [];

  const worker = status.worker;
  const hasWorkerIssues = !worker.ready || !worker.hasAccount;
  if (hasWorkerIssues) {
    alerts.push(
      <div
        key="worker"
        className="alert alert-warning d-flex align-items-start gap-2"
        role="alert"
      >
        <AlertTriangle size={18} className="mt-1" aria-hidden="true" />
        <div>
          <div className="fw-semibold mb-1">
            O Facebook Ads Worker está bloqueado
          </div>
          <ul className="mb-2 ps-3">
            {worker.messages.map((message) => (
              <li key={message.code}>{message.message}</li>
            ))}
          </ul>
          <Link className="btn btn-sm btn-outline-primary" to="/accounts/facebook">
            Revisar contas do Facebook
          </Link>
        </div>
      </div>,
    );
  }

  const tokenRenewal = status.tokenRenewal;
  if (tokenRenewal.enabledAccounts === 0) {
    alerts.push(
      <div
        key="token-renewal-disabled"
        className="alert alert-warning d-flex align-items-start gap-2"
        role="alert"
      >
        <AlertTriangle size={18} className="mt-1" aria-hidden="true" />
        <div>
          <div className="fw-semibold mb-1">
            Renovação automática desativada
          </div>
          <p className="mb-2">
            Nenhuma conta está com a renovação automática habilitada. Ative a opção
            nas contas relevantes para que o worker atualize os tokens antes do
            vencimento.
          </p>
          <Link className="btn btn-sm btn-outline-primary" to="/accounts/facebook">
            Configurar renovação automática
          </Link>
        </div>
      </div>,
    );
  } else if (tokenRenewal.eligibleAccounts === 0) {
    const accountsWithIssues = tokenRenewal.accounts.filter(
      (account) => !account.eligible,
    );

    if (accountsWithIssues.length > 0) {
      alerts.push(
        <div
          key="token-renewal-issues"
          className="alert alert-warning d-flex align-items-start gap-2"
          role="alert"
        >
          <AlertTriangle size={18} className="mt-1" aria-hidden="true" />
          <div>
            <div className="fw-semibold mb-1">
              Tokens não serão renovados automaticamente
            </div>
            <ul className="mb-2 ps-3">
              {accountsWithIssues.map((account) => (
                <li key={account.accountId}>
                  <span className="fw-semibold">{account.accountName}:</span>
                  <ul className="mb-1 ps-3">
                    {account.messages.map((message) => (
                      <li key={message.code}>{message.message}</li>
                    ))}
                  </ul>
                </li>
              ))}
            </ul>
            <Link className="btn btn-sm btn-outline-primary" to="/accounts/facebook">
              Ajustar dados de renovação
            </Link>
          </div>
        </div>,
      );
    }
  }

  if (alerts.length === 0) {
    return null;
  }

  return <div className="d-flex flex-column gap-3">{alerts}</div>;
}
