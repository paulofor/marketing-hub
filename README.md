# Marketing Hub

```bash
docker compose up -d      # start MySQL
cd backend/ads-service && mvn spring-boot:run
cd ../../frontend && npm run dev
# deploy to VPS (Java 21 already installed)
scp target/ads-service.jar <vps>:/opt/marketing-hub/
ssh <vps> "java -jar /opt/marketing-hub/ads-service.jar"
```

To run the Media Hub locally:

```bash
docker compose up     # start MySQL
cd backend/ads-service && mvn package && mvn spring-boot:run
cd ../../frontend && npm run dev
```
