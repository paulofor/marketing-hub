/** Verifica os catálogos de pesquisa e compatibilidade técnica do JAR executável. */
public final class BackendResearchPackageSmoke {
  /** Falha antes do deploy quando os catálogos empacotados não conseguem inicializar. */
  public static void main(String[] args) throws ReflectiveOperationException {
    Class.forName("com.marketinghub.agentmonitor.AgentExecutorHealthService");
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
