"""Prepara contratos revisáveis de BPM; não chama APIs nem altera dados publicados."""
import copy

POLICY = "CHANGE_PER_CYCLE_V1"
COMMON = (
    " Contrato CHANGE_PER_CYCLE_V1 — Cada mudança das condições testadas exige novo ciclo e novo experimento, "
    "vinculados ao predecessor, com uma mudança principal e condições mantidas explícitas. "
    "Preservar versões, tentativas, custos, aprovações e resultados anteriores; não misturar métricas "
    "nem herdar autorização de gasto. Retentativa do mesmo contrato/artefato não constitui nova hipótese. "
)
PROCESS = {
    "pde-opportunity-discovery": "Argos preserva fontes, datas, contrapontos e lacunas como memória; nova pesquisa não comprova oportunidade nem cria experimento automaticamente.",
    "pde-commercial-plan-offer": "Atena declara hipótese, predecessor, variável principal e condições mantidas; Plutus verifica limites próprios e custos desconhecidos; Dédalo delimita a candidata. Um criativo novo não exige mudar também o produto.",
    "pde-construction-approval": "Dédalo altera somente a candidata do sucessor quando a experiência mudar; reutiliza artefatos compatíveis com origem e hash. Psique e Têmis conferem identidade e gates afetados, sem refazer produto inalterado.",
    "pde-communication-sales-journey": "Íris e Apolo vinculam novo vídeo, copy, CTA ou amostra ao sucessor antes da materialização; preservar produto, preço, público e destino quando a variável for apenas o criativo. Integração conserva a atribuição do novo experimento.",
    "pde-commercial-homologation-activation": "Comprovar produto, ciclo, experimento, versões e ativos da candidata, com testes segregados e limites próprios. Aprovação anterior não autoriza campanha, janela ou gasto no sucessor.",
    "pde-sales-delivery-learning": "Hermes e Plutus conciliam cada experimento separadamente; registrar fato, hipótese, causa, limites e decisão. Mudança de criativo ou investimento segue no sucessor; continuar coleta só com condições e limites inalterados.",
    "creative-production-approval": "Íris e Apolo preparam e identificam a peça do novo experimento; Psique e Têmis revisam a candidata exata. Não substituir o vídeo do experimento predecessor para testar outra comunicação.",
    "landing-page-generation": "Materializar destino e prova no contexto do sucessor quando mudarem; preservar a publicação anterior e atribuir captura, versão, oferta e eventos ao experimento correto.",
    "pde-tasting-proof-of-value": "Uma nova amostra ou mudança da degustação constitui outra hipótese: planejar no sucessor, separar do teste de criativo e verificar custo inclusive dos usos sem compra e limites aprovados.",
    "experiment-homologation-activation": "Verificar a identidade do novo ciclo/experimento e as evidências atuais; compra simulada e eventos QA não contam como vendas. Divergência de versão bloqueia antes de revisão paga.",
    "operacao-otimizacao-experimento": "Não aplicar variante no experimento em medição. Registrar causa, uma mudança, referência e retorno; materializar e medir somente no sucessor. Acompanhar compras líquidas, contribuição e custos próprios por janela.",
    "venda-entrega-satisfacao-cliente": "Entregar o contrato comprado no experimento de origem mesmo após iniciar sucessor. Receita, custo, reembolso, uso e retorno pertencem à sua versão; não transferir vendas ou aprovações entre ciclos.",
    "value-chain-learning-sales-cycle": "Registrar mudança pela decisão ADJUST e preparar sucessor planejado com novo ciclo e novo experimento antes do ajuste. Sem exposição, registrar hipótese e lacuna sem inventar resultado; depois de exposição, conciliar e preservar evidências. Expansão de investimento também usa sucessor e autorização própria.",
}


def revise(source):
    """Clona a definição e explicita missões sem modificar a identidade do histórico."""
    code = source["processCode"]
    if code not in PROCESS:
        raise ValueError("Processo fora do escopo da política")
    result = copy.deepcopy(source)
    result["versionNumber"] += 1
    result["status"] = "DRAFT"
    # Normaliza os vínculos legados comprovados contra a composição canônica do Processo 4.
    if code == "pde-communication-sales-journey":
        result["processType"] = "VALUE_PROCESS"
        result["parentProcessCode"] = None
    if code == "pde-tasting-proof-of-value":
        result["processType"] = "SUBPROCESS"
        result["parentProcessCode"] = "pde-communication-sales-journey"
    result["purpose"] += " Cada mudança das condições testadas exige novo ciclo e novo experimento com memória do predecessor."
    result["technicalReference"] = "docs/canonical/ciclos-aprendizado-vendas-canon.v1.md / " + POLICY
    for node in result["diagram"]["nodes"]:
        if node["type"] == "TASK":
            node["description"] = node.get("description", "") + COMMON + PROCESS[code]
        if code == "pde-commercial-homologation-activation":
            agent = {"humanExperienceReview": "customer-agent", "commercialIntegrityReview": "meta-ad-approver"}.get(node["id"])
            if agent:
                node["responsibleAgentKeys"] = [agent]
    if code == "value-chain-learning-sales-cycle":
        diagram = result["diagram"]
        diagram["experimentChangePolicy"] = POLICY
        diagram["nodes"] = [n for n in diagram["nodes"] if n["id"] != "SCALE_AUTHORIZATION"]
        for node in diagram["nodes"]:
            if node["type"] == "TASK" and not node.get("responsibleAgentKeys") and node["id"] not in ["AUTHORIZATION", "PUBLICATION"]:
                node["description"] += " Coordenação e registro pelo backend; especialistas executam nos subprocessos e ferramentas canônicas."
                node["owner"] = "Backend · coordenação e registro"
            if node["id"] == "DECISION":
                node["owner"] = "Atena"
            if node["type"] == "END":
                node["label"] = "Preservar histórico; mudança abre novo ciclo e experimento"
        for node in diagram["nodes"]:
            if node["id"] == "qualityDecision":
                node["description"] = "Repetir somente o mesmo contrato. Se a solução mudar a experiência ou as condições testadas, registrar ADJUST e preparar sucessor."
            elif node["id"] == "commercialDecision":
                node["description"] = "Continuar coleta inalterada, corrigir conciliação ou encerrar. Novo criativo, condição ou escala: ADJUST, novo ciclo e novo experimento."
        flows = []
        for flow in diagram["flows"]:
            if flow["from"] == "SCALE_AUTHORIZATION":
                continue
            flow = copy.deepcopy(flow)
            if flow["to"] == "SCALE_AUTHORIZATION" or (flow["from"] == "commercialDecision" and flow["to"] in ["LEARNING", "ADJUSTMENT"]) or (flow["from"] in ["qualityDecision", "VIDEO_APPROVAL"] and flow["to"] == "ADJUSTMENT"):
                flow["to"] = "end"
                flow["label"] = "Mudança: ADJUST, novo ciclo e novo experimento"
                flow.pop("kind", None)
            if not any(f["from"] == flow["from"] and f["to"] == flow["to"] for f in flows):
                flows.append(flow)
        for flow in flows:
            if flow["from"] == "commercialDecision" and flow["to"] == "end":
                flow["label"] += "; encerrar ou inconclusivo preserva o histórico"
        diagram["flows"] = flows
    if code == "pde-sales-delivery-learning":
        for flow in result["diagram"]["flows"]:
            if "AUTHORIZE_SCALE" in flow.get("label", ""):
                flow["label"] = "Sucessor homologado e autorizado: operação do novo experimento"
    return result
