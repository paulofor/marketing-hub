import { useEffect, useMemo, useState } from "react";
import PageTitle from "../components/PageTitle";
import { useFacebookAccounts } from "../api/useFacebookAccounts";
import {
  useCreateFacebookAccount,
  useDeleteFacebookAccount,
  useUpdateFacebookAccount,
  FacebookAccountPayload,
} from "../api/facebookAccountMutations";
import { useFacebookPages } from "../api/useFacebookPages";
import {
  useCreateFacebookPage,
  useDeleteFacebookPage,
  useUpdateFacebookPage,
} from "../api/facebookPageMutations";

interface AccountFormState {
  id?: number;
  name: string;
}

interface PageFormState {
  id?: number;
  pageId: string;
  name: string;
}

const BRAZILIAN_REAL = "BRL";

const emptyAccountForm: AccountFormState = { name: "" };
const emptyPageForm: PageFormState = { pageId: "", name: "" };

export default function FacebookAccountsPage() {
  const { data, isLoading, error } = useFacebookAccounts();
  const accounts = useMemo(() => (Array.isArray(data) ? data : []), [data]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [accountForm, setAccountForm] = useState<AccountFormState>(emptyAccountForm);
  const [pageForm, setPageForm] = useState<PageFormState>(emptyPageForm);

  const createAccountMutation = useCreateFacebookAccount();
  const updateAccountMutation = useUpdateFacebookAccount();
  const deleteAccountMutation = useDeleteFacebookAccount();

  const { data: pagesData, isLoading: pagesLoading } = useFacebookPages(
    selectedAccountId ?? undefined,
  );
  const pages = useMemo(() => (Array.isArray(pagesData) ? pagesData : []), [pagesData]);

  const createPageMutation = useCreateFacebookPage(selectedAccountId ?? undefined);
  const updatePageMutation = useUpdateFacebookPage(selectedAccountId ?? undefined);
  const deletePageMutation = useDeleteFacebookPage(selectedAccountId ?? undefined);

  useEffect(() => {
    if (accounts.length === 0) {
      setSelectedAccountId(null);
      return;
    }
    if (!selectedAccountId || !accounts.some((account) => account.id === selectedAccountId)) {
      setSelectedAccountId(accounts[0].id);
    }
  }, [accounts, selectedAccountId]);

  useEffect(() => {
    setPageForm(emptyPageForm);
  }, [selectedAccountId]);

  if (isLoading) return <p>Carregando...</p>;
  if (error) return <p>Erro ao carregar contas</p>;

  const isEditingAccount = typeof accountForm.id === "number";
  const isEditingPage = typeof pageForm.id === "number";

  const submitAccount = () => {
    const payload: FacebookAccountPayload = {
      id: accountForm.id,
      name: accountForm.name,
      currency: BRAZILIAN_REAL,
    };
    const mutation = isEditingAccount ? updateAccountMutation : createAccountMutation;
    mutation.mutate(payload, {
      onSuccess: () => {
        setAccountForm(emptyAccountForm);
      },
    });
  };

  const submitPage = () => {
    if (!selectedAccountId) return;
    const payload: PageFormState = {
      id: pageForm.id,
      pageId: pageForm.pageId,
      name: pageForm.name,
    };
    const mutation = isEditingPage ? updatePageMutation : createPageMutation;
    mutation.mutate(payload, {
      onSuccess: () => {
        setPageForm(emptyPageForm);
      },
    });
  };

  return (
    <div>
      <PageTitle>Contas do Facebook</PageTitle>
      <div className="row g-4">
        <div className="col-12 col-xl-6">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Contas conectadas</h2>
                <span className="badge text-bg-primary">{accounts.length} conta(s)</span>
              </div>
              <div className="table-responsive mb-3">
                <table className="table table-striped align-middle">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nome</th>
                      <th>Moeda</th>
                      <th className="text-end">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {accounts.map(({ id, name, currency }) => (
                      <tr key={id} className={selectedAccountId === id ? "table-primary" : ""}>
                        <td>{id}</td>
                        <td>{name}</td>
                        <td>{currency}</td>
                        <td className="text-end d-flex justify-content-end gap-2">
                          <button
                            className="btn btn-sm btn-outline-primary"
                            onClick={() => {
                              setAccountForm({ id, name });
                            }}
                          >
                            Editar
                          </button>
                          <button
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => setSelectedAccountId(id)}
                          >
                            Páginas
                          </button>
                          <button
                            className="btn btn-sm btn-outline-danger"
                            onClick={() => deleteAccountMutation.mutate(id)}
                          >
                            Excluir
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="row g-2">
                <div className="col-md-5">
                  <input
                    className="form-control"
                    placeholder="Nome da conta"
                    value={accountForm.name}
                    onChange={(event) =>
                      setAccountForm((current) => ({
                        ...current,
                        name: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-md-7">
                  <button className="btn btn-primary w-100" onClick={submitAccount}>
                    {isEditingAccount ? "Atualizar conta" : "Adicionar conta"}
                  </button>
                </div>
              </div>
              <p className="text-muted small mt-2 mb-0">
                A moeda das contas do Facebook é sempre o Real brasileiro (BRL).
              </p>
            </div>
          </div>
        </div>
        <div className="col-12 col-xl-6">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Páginas vinculadas</h2>
                <span className="badge text-bg-primary">
                  {selectedAccountId ? pages.length : 0} página(s)
                </span>
              </div>
              {!selectedAccountId ? (
                <p className="text-muted mb-0">
                  Cadastre ou selecione uma conta para configurar as páginas utilizadas nas
                  campanhas.
                </p>
              ) : pagesLoading ? (
                <p>Carregando páginas...</p>
              ) : (
                <div className="table-responsive mb-3">
                  <table className="table table-hover align-middle">
                    <thead>
                      <tr>
                        <th>ID da página</th>
                        <th>Nome</th>
                        <th className="text-end">Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pages.map((page) => (
                        <tr key={page.id}>
                          <td>{page.pageId}</td>
                          <td>{page.name}</td>
                          <td className="text-end d-flex justify-content-end gap-2">
                            <button
                              className="btn btn-sm btn-outline-primary"
                              onClick={() =>
                                setPageForm({
                                  id: page.id,
                                  pageId: page.pageId,
                                  name: page.name,
                                })
                              }
                            >
                              Editar
                            </button>
                            <button
                              className="btn btn-sm btn-outline-danger"
                              onClick={() => deletePageMutation.mutate(page.id)}
                            >
                              Excluir
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <div className="row g-2">
                <div className="col-md-5">
                  <input
                    className="form-control"
                    placeholder="ID da página"
                    value={pageForm.pageId}
                    onChange={(event) =>
                      setPageForm((current) => ({
                        ...current,
                        pageId: event.target.value,
                      }))
                    }
                    disabled={!selectedAccountId}
                  />
                </div>
                <div className="col-md-4">
                  <input
                    className="form-control"
                    placeholder="Nome da página"
                    value={pageForm.name}
                    onChange={(event) =>
                      setPageForm((current) => ({
                        ...current,
                        name: event.target.value,
                      }))
                    }
                    disabled={!selectedAccountId}
                  />
                </div>
                <div className="col-md-3">
                  <button
                    className="btn btn-primary w-100"
                    onClick={submitPage}
                    disabled={!selectedAccountId}
                  >
                    {isEditingPage ? "Atualizar página" : "Adicionar página"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
