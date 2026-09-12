/** Verifica os catálogos de pesquisa, comportamento e compatibilidade do JAR executável. */
public final class BackendResearchPackageSmoke {
  /** Falha antes do deploy quando os catálogos empacotados não conseguem inicializar. */
  public static void main(String[] args) throws ReflectiveOperationException {
    Class.forName("com.marketinghub.agentmonitor.AgentExecutorHealthService");
    Class<?> objectMapperType = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
    Class.forName("com.marketinghub.agentdetail.service.AgentHarnessCatalog")
        .getConstructor(objectMapperType)
        .newInstance(objectMapperType.getConstructor().newInstance());
    System.out.println("Catálogo de comportamento dos agentes inicializado no JAR executável.");
    Class<?> serviceType = Class.forName(
        "com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService");
    Object service = serviceType.getConstructor().newInstance();
    Object catalog = serviceType.getMethod("getCatalog").invoke(service);
    int total = (Integer) catalog.getClass().getMethod("totalCompiledCards").invoke(catalog);
    if (total <= 0) {
      throw new IllegalStateException("O JAR executável não contém artigos elegíveis.");
    }
    System.out.println("Catálogo do JAR executável inicializado: " + total + " cartões.");
  }
}
