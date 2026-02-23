import { FormEvent, useEffect, useMemo, useState } from "react";
import axios from "axios";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import {
  useEmailSmtpSettings,
  useTestEmailSettings,
  useUpdateEmailSmtpSettings,
} from "../../api/settings/useEmailSmtpSettings";
import { EmailProviderPreset, useEmailProviderPresets } from "../../api/settings/useEmailProviderPresets";

interface FormState {
  providerName: string;
  host: string;
  port: string;
  authEnabled: boolean;
  username: string;
  fromName: string;
  fromEmail: string;
  useStartTls: boolean;
  useSsl: boolean;
  connectionTimeoutMs: string;
  readTimeoutMs: string;
  writeTimeoutMs: string;
  dryRun: boolean;
}

function formatDateTime(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

const DEFAULT_FORM: FormState = {
  providerName: "",
  host: "",
  port: "465",
  authEnabled: true,
  username: "",
  fromName: "",
  fromEmail: "",
  useStartTls: false,
  useSsl: true,
  connectionTimeoutMs: "5000",
  readTimeoutMs: "5000",
  writeTimeoutMs: "5000",
  dryRun: false,
};

export default function EmailSettingsPage() {
  const { data, isLoading, isError } = useEmailSmtpSettings();
  const updateSettings = useUpdateEmailSmtpSettings();
  const testEmail = useTestEmailSettings();
  const { data: providerPresets, isLoading: isLoadingPresets, isError: presetsError } = useEmailProviderPresets();
  const [formState, setFormState] = useState<FormState>(DEFAULT_FORM);
  const [passwordInput, setPasswordInput] = useState("");
  const [passwordTouched, setPasswordTouched] = useState(false);
  const [testRecipient, setTestRecipient] = useState("");
  const [testSubject, setTestSubject] = useState("Teste de SMTP do Marketing Hub");
  const [testMessage, setTestMessage] = useState("Este e-mail confirma o envio pelo servidor configurado.");

  useEffect(() => {
    if (!data) return;
    setFormState({
      providerName: data.providerName ?? "",
      host: data.host ?? "",
      port: data.port != null ? String(data.port) : "",
      authEnabled: data.authEnabled,
      username: data.username ?? "",
      fromName: data.fromName ?? "",
      fromEmail: data.fromEmail ?? "",
      useStartTls: data.useStartTls,
      useSsl: data.useSsl,
      connectionTimeoutMs: data.connectionTimeoutMs != null ? String(data.connectionTimeoutMs) : "5000",
      readTimeoutMs: data.readTimeoutMs != null ? String(data.readTimeoutMs) : "5000",
      writeTimeoutMs: data.writeTimeoutMs != null ? String(data.writeTimeoutMs) : "5000",
      dryRun: data.dryRun,
    });
    setPasswordInput("");
    setPasswordTouched(false);
  }, [data]);

  const lastUpdatedLabel = useMemo(
    () => formatDateTime(data?.updatedAt),
    [data?.updatedAt],
  );

  const handleChange = (field: keyof FormState, value: string | boolean) => {
    setFormState((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleApplyPreset = (preset: EmailProviderPreset) => {
    setFormState((prev) => ({
      ...prev,
      providerName: preset.name,
      host: preset.host || prev.host,
      port: preset.port != null ? String(preset.port) : prev.port,
      authEnabled: preset.authEnabled,
      username: "",
      useStartTls: preset.useStartTls,
      useSsl: preset.useSsl,
    }));
    setPasswordInput("");
    setPasswordTouched(false);
    toast.info(`Preset ${preset.name} aplicado. Revise remetente, usuário e senha antes de salvar.`);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState.host.trim()) {
      toast.error("Informe o host do servidor");
      return;
    }
    if (!formState.fromEmail.trim()) {
      toast.error("Informe o e-mail do remetente");
      return;
    }
    if (!formState.port.trim()) {
      toast.error("Informe a porta do servidor");
      return;
    }
    const payload = {
      providerName: formState.providerName.trim() || null,
      host: formState.host.trim(),
      port: Number(formState.port),
      authEnabled: formState.authEnabled,
      username: formState.authEnabled ? formState.username.trim() || null : null,
      password: passwordTouched ? passwordInput.trim() : null,
      fromName: formState.fromName.trim() || null,
      fromEmail: formState.fromEmail.trim(),
      useStartTls: formState.useStartTls,
      useSsl: formState.useSsl,
      connectionTimeoutMs: Number(formState.connectionTimeoutMs) || 5000,
      readTimeoutMs: Number(formState.readTimeoutMs) || 5000,
      writeTimeoutMs: Number(formState.writeTimeoutMs) || 5000,
      dryRun: formState.dryRun,
    };

    try {
      await updateSettings.mutateAsync(payload);
      toast.success("Configuração salva com sucesso");
      if (passwordTouched) {
        setPasswordInput("");
        setPasswordTouched(false);
      }
    } catch (error) {
      const message = axiosErrorMessage(error) ?? "Não foi possível salvar a configuração.";
      toast.error(message);
    }
  };

  const handleTestSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!testRecipient.trim()) {
      toast.error("Informe o destinatário do teste");
      return;
    }
    try {
      const response = await testEmail.mutateAsync({
        recipient: testRecipient.trim(),
        subject: testSubject.trim() || undefined,
        message: testMessage.trim() || undefined,
      });
      toast.success(response.message || "E-mail de teste enviado");
    } catch (error) {
      const message = axiosErrorMessage(error) ?? "Não foi possível enviar o e-mail de teste.";
      toast.error(message);
    }
  };

  const renderProviderPresets = () => {
    if (isLoadingPresets) {
      return (
        <section className="card mt-3">
          <div className="card-header">
            <h5 className="mb-1">Sugestões de provedores</h5>
            <p className="text-muted mb-0">Carregando presets de SMTP...</p>
          </div>
          <div className="card-body">
            <p className="mb-0">Carregando presets...</p>
          </div>
        </section>
      );
    }
    if (presetsError) {
      return (
        <section className="card mt-3">
          <div className="card-header">
            <h5 className="mb-1">Sugestões de provedores</h5>
            <p className="text-muted mb-0">Use um dos modelos para preencher rapidamente o formulário.</p>
          </div>
          <div className="card-body">
            <div className="alert alert-warning mb-0" role="alert">
              Não foi possível carregar os presets agora. Recarregue a página ou informe o provedor manualmente.
            </div>
          </div>
        </section>
      );
    }
    if (!providerPresets || providerPresets.length === 0) {
      return null;
    }
    return (
      <section className="card mt-3">
        <div className="card-header">
          <h5 className="mb-1">Sugestões de provedores</h5>
          <p className="text-muted mb-0">
            Clique em "Aplicar preset" para preencher host, porta e segurança conforme a documentação oficial e depois informe as credenciais.
          </p>
        </div>
        <div className="card-body">
          <div className="row g-3">
            {providerPresets.map((preset) => (
              <div key={preset.id} className="col-12 col-lg-6">
                <div className="border rounded-3 h-100 d-flex flex-column p-3">
                  <div className="d-flex justify-content-between align-items-start gap-2">
                    <div>
                      <h6 className="mb-1">{preset.name}</h6>
                      <p className="text-muted small mb-1">{preset.headline}</p>
                    </div>
                    <span className="badge text-bg-light text-body-secondary">{preset.bestFor}</span>
                  </div>
                  <p className="mb-2 small">{preset.summary}</p>
                  <dl className="row small mb-2">
                    <dt className="col-sm-4">Host</dt>
                    <dd className="col-sm-8">
                      <code>{preset.host}</code>
                    </dd>
                    <dt className="col-sm-4">Porta padrão</dt>
                    <dd className="col-sm-8">{preset.port ?? "—"}</dd>
                    {preset.alternativePorts.length ? (
                      <>
                        <dt className="col-sm-4">Alternativas</dt>
                        <dd className="col-sm-8">{preset.alternativePorts.join(", ")}</dd>
                      </>
                    ) : null}
                    <dt className="col-sm-4">TLS</dt>
                    <dd className="col-sm-8">{preset.useStartTls ? "STARTTLS" : preset.useSsl ? "SSL" : "Sem criptografia"}</dd>
                    <dt className="col-sm-4">Plano</dt>
                    <dd className="col-sm-8">{preset.pricingSummary}</dd>
                    <dt className="col-sm-4">Free tier</dt>
                    <dd className="col-sm-8">{preset.freeTier}</dd>
                  </dl>
                  <ul className="small ps-3 mb-2">
                    {preset.highlights.map((highlight) => (
                      <li key={highlight}>{highlight}</li>
                    ))}
                  </ul>
                  {preset.notes ? (
                    <p className="text-muted small mb-2">{preset.notes}</p>
                  ) : null}
                  <div className="mt-auto d-flex flex-wrap gap-2">
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-primary"
                      onClick={() => handleApplyPreset(preset)}
                    >
                      Aplicar preset
                    </button>
                    <a
                      className="btn btn-sm btn-link"
                      href={preset.docsUrl}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Docs
                    </a>
                    <a
                      className="btn btn-sm btn-link"
                      href={preset.pricingUrl}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Preços
                    </a>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    );
  };

  const renderForm = () => {
    if (isLoading) {
      return <p>Carregando configuração SMTP...</p>;
    }
    if (isError || !data) {
      return (
        <p className="text-danger mb-0">
          Não foi possível carregar as configurações atuais. Recarregue a página ou tente novamente em instantes.
        </p>
      );
    }

    return (
      <form className="row gy-4" onSubmit={handleSubmit}>
        <div className="col-12 col-lg-8">
          <div className="row g-3">
            <div className="col-12 col-md-6">
              <label className="form-label">Nome do provedor</label>
              <input
                type="text"
                className="form-control"
                value={formState.providerName}
                onChange={(event) => handleChange("providerName", event.target.value)}
                placeholder="Hostinger, Amazon SES, SendGrid..."
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">
                E-mail do remetente <span className="text-danger">*</span>
              </label>
              <input
                type="email"
                className="form-control"
                required
                value={formState.fromEmail}
                onChange={(event) => handleChange("fromEmail", event.target.value)}
                placeholder="contato@seudominio.com"
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Nome exibido</label>
              <input
                type="text"
                className="form-control"
                value={formState.fromName}
                onChange={(event) => handleChange("fromName", event.target.value)}
                placeholder="Equipe Marketing Hub"
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">
                Host SMTP <span className="text-danger">*</span>
              </label>
              <input
                type="text"
                className="form-control"
                required
                value={formState.host}
                onChange={(event) => handleChange("host", event.target.value)}
                placeholder="smtp.seuprovedor.com"
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">
                Porta <span className="text-danger">*</span>
              </label>
              <input
                type="number"
                className="form-control"
                min={1}
                max={65535}
                value={formState.port}
                onChange={(event) => handleChange("port", event.target.value)}
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Usuário</label>
              <input
                type="text"
                className="form-control"
                value={formState.username}
                onChange={(event) => handleChange("username", event.target.value)}
                disabled={!formState.authEnabled}
              />
              <div className="form-text">
                Informe apenas se o provedor exigir autenticação.
              </div>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Senha</label>
              <input
                type="password"
                className="form-control"
                value={passwordInput}
                onChange={(event) => {
                  setPasswordInput(event.target.value);
                  setPasswordTouched(true);
                }}
                placeholder="Digite para atualizar"
                disabled={!formState.authEnabled}
              />
              <div className="mt-1">
                <small className="text-muted">
                  {data.hasPassword && !passwordTouched
                    ? "Senha já cadastrada. Digite um novo valor para atualizar."
                    : ""}
                </small>
              </div>
            </div>
            <div className="col-12">
              <div className="form-check form-switch">
                <input
                  className="form-check-input"
                  type="checkbox"
                  role="switch"
                  id="authToggle"
                  checked={formState.authEnabled}
                  onChange={(event) => handleChange("authEnabled", event.target.checked)}
                />
                <label className="form-check-label" htmlFor="authToggle">
                  Requer autenticação SMTP
                </label>
              </div>
            </div>
            <div className="col-12">
              <div className="form-check form-switch">
                <input
                  className="form-check-input"
                  type="checkbox"
                  role="switch"
                  id="tlsToggle"
                  checked={formState.useStartTls}
                  onChange={(event) => handleChange("useStartTls", event.target.checked)}
                />
                <label className="form-check-label" htmlFor="tlsToggle">
                  Habilitar STARTTLS
                </label>
              </div>
              <div className="form-check form-switch">
                <input
                  className="form-check-input"
                  type="checkbox"
                  role="switch"
                  id="sslToggle"
                  checked={formState.useSsl}
                  onChange={(event) => handleChange("useSsl", event.target.checked)}
                />
                <label className="form-check-label" htmlFor="sslToggle">
                  Exigir SSL/TLS na conexão
                </label>
              </div>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Timeout de conexão (ms)</label>
              <input
                type="number"
                min={100}
                className="form-control"
                value={formState.connectionTimeoutMs}
                onChange={(event) => handleChange("connectionTimeoutMs", event.target.value)}
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Timeout de leitura (ms)</label>
              <input
                type="number"
                min={100}
                className="form-control"
                value={formState.readTimeoutMs}
                onChange={(event) => handleChange("readTimeoutMs", event.target.value)}
              />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label">Timeout de escrita (ms)</label>
              <input
                type="number"
                min={100}
                className="form-control"
                value={formState.writeTimeoutMs}
                onChange={(event) => handleChange("writeTimeoutMs", event.target.value)}
              />
            </div>
            <div className="col-12">
              <div className="alert alert-warning mb-0" role="alert">
                <div className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="dryRunToggle"
                    checked={formState.dryRun}
                    onChange={(event) => handleChange("dryRun", event.target.checked)}
                  />
                  <label className="form-check-label" htmlFor="dryRunToggle">
                    Ativar modo dry-run (não envia e-mails, apenas registra no log)
                  </label>
                </div>
                <small className="text-muted">
                  Utilize apenas em ambientes de teste. Para validar com leads reais, mantenha desativado.
                </small>
              </div>
            </div>
          </div>
        </div>
        <div className="col-12 col-lg-4">
          <div className="bg-body-tertiary rounded-3 p-3 h-100">
            <h6 className="fw-semibold">Última atualização</h6>
            <p className="text-muted">{lastUpdatedLabel}</p>
            <h6 className="fw-semibold">Status</h6>
            <p className="mb-1">
              {formState.dryRun ? (
                <span className="badge text-bg-warning">Simulação</span>
              ) : (
                <span className="badge text-bg-success">Envio real</span>
              )}
            </p>
            <p className="text-muted small mb-0">
              Habilite o modo real para liberar disparos para os leads e para o e-mail de teste.
            </p>
          </div>
        </div>
        <div className="col-12 d-flex gap-2">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={updateSettings.isPending}
          >
            {updateSettings.isPending ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
                Salvando...
              </>
            ) : (
              "Salvar configuração"
            )}
          </button>
        </div>
      </form>
    );
  };

  return (
    <div className="mt-3">
      <div className="d-flex justify-content-between align-items-center gap-3">
        <PageTitle>Serviço de e-mail</PageTitle>
      </div>

      {renderProviderPresets()}

      <section className="card mt-3">
        <div className="card-header">
          <h5 className="mb-1">Servidor SMTP</h5>
          <p className="text-muted mb-0">
            Salve aqui as credenciais do provedor de e-mail utilizado para enviar mensagens aos leads.
          </p>
        </div>
        <div className="card-body">{renderForm()}</div>
      </section>

      <section className="card mt-4">
        <div className="card-header">
          <h5 className="mb-1">Enviar e-mail de teste</h5>
          <p className="text-muted mb-0">
            Dispare um e-mail simples para validar se o provedor está autenticando corretamente.
          </p>
        </div>
        <div className="card-body">
          <form className="row g-3" onSubmit={handleTestSubmit}>
            <div className="col-12 col-md-6">
              <label className="form-label">
                Destinatário <span className="text-danger">*</span>
              </label>
              <input
                type="email"
                className="form-control"
                value={testRecipient}
                onChange={(event) => setTestRecipient(event.target.value)}
                placeholder="voce@seudominio.com"
                required
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label">Assunto</label>
              <input
                type="text"
                className="form-control"
                value={testSubject}
                onChange={(event) => setTestSubject(event.target.value)}
              />
            </div>
            <div className="col-12">
              <label className="form-label">Mensagem</label>
              <textarea
                className="form-control"
                rows={4}
                value={testMessage}
                onChange={(event) => setTestMessage(event.target.value)}
              />
              <div className="form-text">
                O e-mail de teste usa o remetente configurado acima. Desative o modo dry-run para enviá-lo.
              </div>
            </div>
            <div className="col-12 d-flex gap-2">
              <button
                type="submit"
                className="btn btn-outline-primary"
                disabled={testEmail.isPending || formState.dryRun}
              >
                {testEmail.isPending ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
                    Enviando...
                  </>
                ) : (
                  "Enviar teste"
                )}
              </button>
              {formState.dryRun ? (
                <span className="text-danger small align-self-center">
                  Desative o modo dry-run para liberar o envio.
                </span>
              ) : null}
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}

function axiosErrorMessage(error: unknown): string | null {
  if (!axios.isAxiosError(error)) return null;
  const data = error.response?.data as { message?: string } | undefined;
  return data?.message ?? null;
}
