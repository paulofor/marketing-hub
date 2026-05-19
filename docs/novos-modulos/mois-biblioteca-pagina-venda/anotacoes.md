# Fluxo Clickbank
## Fluxo Principal Dispara por hora 
### Existem 3 cilcos:
*CICLO_1_TOP_OFFERS
*CICLO_2_VENDAS_PAGE
*CICLO_3_GRAPHQL

### CICLO_2_VENDAS_PAGE

#### Insert
* collectorService.collectSecondCycleFromBackend(request);
* ao final chama : publishSalesPagesToLibrary(List<ClickbankProductSnapshot> products)
* envio para : /api/mois/sales-library/urls:ingest

* Backend ( v1 )
* MoisSalesLibraryController.ingestUrls
* MoisSalesLibraryService.ingestUrls
  1. Para cada item
  2. INSERT INTO mois_sales_library_url_ingest
 
#### Claim
/api/mois/sales-library/claim
( ninguem ???? )
