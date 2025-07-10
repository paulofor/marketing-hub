# Marketing Hub
This project manages ads, media assets, course plans and now products built with marketing principles.

```bash
docker compose up -d      # start MySQL
cd backend/ads-service && mvn spring-boot:run
cd ../../frontend && npm run dev
# run the background worker (optional)
cd ../success-product-worker && mvn spring-boot:run
# the worker fetches the ads-service dependency from
# https://maven.pkg.github.com/paulofor/ads-service
# The backend builds two JAR files when packaging:
# - `app.jar` is the thin artifact published to GitHub Packages and
#   consumed by the Success Product Worker using Maven.
# - `app-exec.jar` is the fat executable that gets copied to the VPS as
#   `app.jar`.
# create a .env file to point the React app to your backend
echo "VITE_API_URL=http://localhost:8000" > frontend/.env
# deploy to VPS (Java 21 already installed)
scp target/app-exec.jar <vps>:/opt/marketinghub/app/app.jar
ssh <vps> "java -jar /opt/marketinghub/app/app.jar"
```

To run the Media Hub locally:

```bash
docker compose up     # start MySQL
cd backend/ads-service && mvn package && mvn spring-boot:run
cd ../../frontend && npm run dev
```
