# Guia de implantação do backend no AWS Lightsail

Este guia descreve o passo a passo para publicar a aplicação backend do Marketing Hub em uma instância Linux do AWS Lightsail via SSH. As instruções a seguir assumem que você já gerou o `.jar` da aplicação (`ads-service`) e possui a chave SSH configurada na sua máquina local.

> Todos os comandos abaixo devem ser executados **dentro da VPS** depois de acessar via SSH, salvo quando explicitamente indicado que devem ser executados na máquina local.

## 1. Conectar na VPS

Na sua máquina local execute (substitua `chave.pem` e `IP_DA_VPS` pelos valores corretos):

```bash
ssh -i ~/.ssh/chave.pem ubuntu@IP_DA_VPS
```

Se a instância utilizar outro usuário padrão (por exemplo `ec2-user` ou `admin`), ajuste o comando conforme necessário.

## 2. Atualizar pacotes e configurar o ambiente base

Dentro da VPS, atualize o sistema e instale dependências básicas:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y unzip curl htop
```

Defina o fuso horário (opcional, ajuste conforme necessário):

```bash
sudo timedatectl set-timezone America/Sao_Paulo
```

## 3. Instalar o Java 21

A aplicação utiliza Java 21 (Spring Boot 3). Instale o runtime adequado:

```bash
sudo apt install -y openjdk-21-jdk
java -version
```

O último comando deve retornar a versão 21 do Java.

## 4. Criar usuário e diretórios da aplicação

Crie um usuário dedicado sem shell de login e os diretórios necessários para o deploy:

```bash
sudo useradd --system --home /opt/marketinghub --shell /usr/sbin/nologin marketinghub
sudo mkdir -p /opt/marketinghub/app /opt/marketinghub/config /var/log/marketinghub
sudo chown -R marketinghub:marketinghub /opt/marketinghub /var/log/marketinghub
```

## 5. Transferir o artefato `.jar`

Na máquina local, envie o arquivo compilado (`ads-service-<versao>.jar`) para a VPS:

```bash
scp -i ~/.ssh/chave.pem backend/ads-service/target/ads-service-<versao>.jar ubuntu@IP_DA_VPS:/tmp/app.jar
```

De volta ao SSH da VPS, mova o arquivo para o diretório da aplicação e ajuste as permissões:

```bash
sudo mv /tmp/app.jar /opt/marketinghub/app/app.jar
sudo chown marketinghub:marketinghub /opt/marketinghub/app/app.jar
sudo chmod 550 /opt/marketinghub/app/app.jar
```

## 6. Configurar propriedades sensíveis

Crie um arquivo externo com as configurações específicas do ambiente (credenciais de banco, tokens, etc.). Exemplo:

```bash
sudo tee /opt/marketinghub/config/application-prod.properties <<'CONFIG'
# Configurações de banco de dados
spring.datasource.url=jdbc:mysql://HOST_DO_BANCO:3306/NOME_DO_BANCO
spring.datasource.username=USUARIO
spring.datasource.password=SENHA

# Ajustes adicionais
server.port=8000
spring.jpa.hibernate.ddl-auto=update

# Tokens das integrações (preencha apenas quando utilizar)
integrations.meta.enabled=false
integrations.sendgrid.enabled=false
integrations.whatsapp.enabled=false
integrations.ga4.enabled=false
CONFIG

sudo chown marketinghub:marketinghub /opt/marketinghub/config/application-prod.properties
sudo chmod 640 /opt/marketinghub/config/application-prod.properties
```

## 7. Instalar o serviço systemd

Copie o arquivo `marketinghub-backend.service` do repositório para a VPS (com `scp`, semelhante ao passo 5) e mova para o diretório de unidades do systemd:

```bash
sudo mv /tmp/marketinghub-backend.service /etc/systemd/system/marketinghub-backend.service
```

Edite o serviço para carregar o arquivo de configuração externo. Adicione a linha `Environment="SPRING_CONFIG_ADDITIONAL_LOCATION=file:/opt/marketinghub/config/"` dentro da seção `[Service]` (use o editor de sua preferência, por exemplo `sudo nano /etc/systemd/system/marketinghub-backend.service`). O arquivo deve ficar parecido com:

```ini
[Service]
Type=simple
User=marketinghub
WorkingDirectory=/opt/marketinghub/app
Environment="SPRING_CONFIG_ADDITIONAL_LOCATION=file:/opt/marketinghub/config/"
ExecStart=/usr/bin/java -jar /opt/marketinghub/app/app.jar
Restart=on-failure
```

Depois aplique as alterações e habilite o serviço:

```bash
sudo systemctl daemon-reload
sudo systemctl enable marketinghub-backend.service
sudo systemctl start marketinghub-backend.service
sudo systemctl status marketinghub-backend.service
```

## 8. Configurar firewall (UFW opcional)

Caso esteja utilizando o UFW, libere a porta da aplicação (ex.: 8000) e o acesso SSH:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8000/tcp
sudo ufw enable
sudo ufw status
```

## 9. Logs e monitoramento

Os logs gerados pelo Spring Boot ficam em `/var/log/marketinghub`. Para acompanhar em tempo real:

```bash
sudo tail -f /var/log/marketinghub/application.log
```

Para reiniciar ou parar o serviço utilize:

```bash
sudo systemctl restart marketinghub-backend.service
sudo systemctl stop marketinghub-backend.service
```

## 10. Testar a API

Verifique se o serviço está respondendo (substitua `IP_DA_VPS` pelo endereço público):

```bash
curl http://IP_DA_VPS:8000/actuator/health
```

O retorno esperado é um JSON com o status `"UP"` quando a aplicação estiver funcionando corretamente.

---

Com esses passos o backend ficará publicado na instância Lightsail com um serviço systemd gerenciando o processo Java. Ajuste as configurações de acordo com as necessidades específicas do ambiente (por exemplo, domínios personalizados, certificados TLS ou integrações externas).

## 11. Automatizar o deploy com GitHub Actions

O repositório já inclui o workflow [`backend-lightsail-deploy.yml`](../.github/workflows/backend-lightsail-deploy.yml), eliminando a necessidade de criar o arquivo manualmente. Ele compila o backend, transfere o artefato para a VPS e reinicia o serviço systemd.

1. Configure os seguintes *secrets* no repositório do GitHub:
   - `LIGHTSAIL_HOST`: endereço público (ou domínio) da VPS.
   - `LIGHTSAIL_USER`: usuário SSH com permissão de deploy (por exemplo `ubuntu` ou `marketinghub`).
   - `LIGHTSAIL_SSH_KEY`: chave privada no formato PEM com acesso ao servidor.
2. Opcionalmente ajuste os caminhos-padrão editando as variáveis `APP_DIR` e `SERVICE_NAME` no workflow.
3. Execute o workflow a partir da aba **Actions** do GitHub usando o gatilho `Run workflow` (evento `workflow_dispatch`).

Após a conclusão, o artefato será movido para `/opt/marketinghub/app/app.jar` e o serviço `marketinghub-backend.service` será reiniciado automaticamente na VPS.
