package com.marketinghub.oprm.infra;

import com.marketinghub.oprm.domain.OccupationCatalogItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StructuredOccupationCatalog {

    private final List<OccupationCatalogItem> items = List.of(
            new OccupationCatalogItem(
                    "personal trainer",
                    List.of("personal trainer", "treinador pessoal", "pt"),
                    "Profissional que orienta treinos, corrige execução e acompanha evolução física.",
                    List.of("Montar fichas de treino", "Acompanhar alunos", "Registrar evolução", "Captar novos clientes"),
                    List.of("Avaliação física", "Comunicação", "Didática", "Gestão de agenda"),
                    List.of("WhatsApp", "Planilhas", "Apps de treino", "Instagram"),
                    List.of("Academias", "Atendimento domiciliar", "Consultoria online"),
                    "oprm-mvp-structured-sources",
                    List.of("mvp-pt-001")),
            new OccupationCatalogItem(
                    "pastor",
                    List.of("pastor", "líder religioso evangélico", "ministro pastoral"),
                    "Lidera comunidade religiosa, organiza cultos e atende demandas pastorais da congregação.",
                    List.of("Preparar sermões", "Conduzir cultos", "Aconselhamento", "Visitas pastorais"),
                    List.of("Oratória", "Gestão comunitária", "Escuta ativa", "Planejamento"),
                    List.of("Bíblia", "Ferramentas de apresentação", "WhatsApp", "Planilhas"),
                    List.of("Igreja local", "Eventos comunitários", "Atendimento individual"),
                    "oprm-mvp-structured-sources",
                    List.of("mvp-pastor-001")),
            new OccupationCatalogItem(
                    "agricultor",
                    List.of("agricultor", "produtor rural", "lavrador"),
                    "Produz culturas agrícolas e administra atividades operacionais da propriedade rural.",
                    List.of("Planejar plantio", "Monitorar clima", "Gerir insumos", "Organizar colheita"),
                    List.of("Manejo de solo", "Gestão operacional", "Negociação", "Controle de custos"),
                    List.of("Maquinário agrícola", "Aplicativos meteorológicos", "Planilhas", "WhatsApp"),
                    List.of("Propriedade rural", "Cooperativas", "Feiras e distribuidores"),
                    "oprm-mvp-structured-sources",
                    List.of("mvp-agri-001")),
            new OccupationCatalogItem(
                    "manicure",
                    List.of("manicure", "nail designer", "profissional de unhas"),
                    "Atende clientes para cuidados estéticos das unhas, com forte dependência de agenda e fidelização.",
                    List.of("Atender clientes", "Gerenciar agenda", "Esterilizar materiais", "Divulgar serviços"),
                    List.of("Técnicas de esmaltação", "Atendimento", "Higienização", "Venda consultiva"),
                    List.of("Agenda digital", "WhatsApp", "Instagram", "Máquina de cartão"),
                    List.of("Salão", "Atendimento em domicílio", "Studio próprio"),
                    "oprm-mvp-structured-sources",
                    List.of("mvp-mani-001")),
            new OccupationCatalogItem(
                    "cabeleireiro",
                    List.of("cabeleireiro", "hair stylist", "profissional de salão"),
                    "Profissional de beleza que realiza cortes, tratamentos e organiza fluxo de clientes no salão.",
                    List.of("Realizar cortes", "Fazer procedimentos", "Organizar agenda", "Vender produtos"),
                    List.of("Técnicas capilares", "Atendimento", "Gestão de tempo", "Upsell"),
                    List.of("Tesouras e secadores", "Agenda digital", "Instagram", "Sistema de caixa"),
                    List.of("Salões de beleza", "Studio próprio", "Atendimento em eventos"),
                    "oprm-mvp-structured-sources",
                    List.of("mvp-cabel-001")),
            new OccupationCatalogItem(
                    "dono de loja de celulares",
                    List.of("dono de loja de celulares", "lojista de celulares", "comerciante de smartphones"),
                    "Empreendedor que comercializa celulares e acessórios, gerindo estoque, vendas e pós-venda.",
                    List.of("Atender clientes", "Negociar preços", "Controlar estoque", "Pós-venda e garantia"),
                    List.of("Vendas", "Negociação", "Gestão de estoque", "Relacionamento com fornecedores"),
                    List.of("Sistema de PDV", "Marketplace", "WhatsApp", "Planilhas"),
                    List.of("Loja física", "Comércio local", "Canais digitais"),
                    "oprm-mvp-structured-sources",
                    List.of("mvp-cel-001"))
    );

    public List<OccupationCatalogItem> listAll() {
        return items;
    }
}
