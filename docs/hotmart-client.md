# Manual de Configuração do Hotmart Client

Este manual explica como configurar o módulo `HotmartClient` utilizado pelo **Success Product Worker** para acessar a API da Hotmart e buscar os produtos com maior temperatura.

## Pré-requisitos
- Conta na Hotmart com acesso à API
- Usuário e senha válidos para autenticação básica

## Definindo as credenciais
O cliente utiliza propriedades do Spring Boot para se conectar ao Hotmart. Configure-as em `application.properties` ou como variáveis de ambiente.

### `application.properties`
```properties
hotmart.base-url=https://api.hotmart.com
hotmart.username=seu_usuario
hotmart.password=sua_senha
```

### Variáveis de ambiente
```bash
export HOTMART_BASE_URL=https://api.hotmart.com
export HOTMART_USERNAME=seu_usuario
export HOTMART_PASSWORD=sua_senha
```

A URL base é opcional e por padrão aponta para `https://api.hotmart.com`.

## Uso
Com as credenciais configuradas, o `HotmartClient` é registrado pelo Spring e pode ser injetado em outros componentes:

```java
@Autowired
private HotmartClient hotmartClient;

List<HotmartProduct> produtos = hotmartClient.fetchTopProducts(10);
```

Essas chamadas retornam a lista de produtos ordenados por temperatura de forma decrescente.
